#! /bin/bash
# ============LICENSE_START====================================================
#  Copyright (C) 2020-2021 AT&T Intellectual Property. All rights reserved.
#  Modification Copyright 2021-2026 OpenInfra Foundation Europe. All rights reserved.
#  Modifications Copyright (C) 2021 Bell Canada. All rights reserved.
#  Modifications Copyright 2024-2025 Deutsche Telekom
# =============================================================================
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# SPDX-License-Identifier: Apache-2.0
# ============LICENSE_END======================================================

# Unified script to resolve docker image versions for ACM-R and participants.
#
# Usage: source get-versions.sh [OPTIONS]
#   --local                    Use local "latest" images (no registry pull)
#   --branch <acm> [ppnt]     Resolve versions from specified release branch(es)
#   --version <acm> [ppnt]    Use explicit version strings directly
#   (no args)                  Resolve from default .gitreview branch

if [ -z "${WORKSPACE}" ]; then
    WORKSPACE=$(git rev-parse --show-toplevel)
    export WORKSPACE
fi

export POLICY_POSTGRES_VER=17.7

# --- Resolve docker image version from release data on a given branch ---
# Usage: getDockerVersion <repo> <branch> [default_image_name] [default_image_version]
# Sets: docker_image_version, docker_image_name
function getDockerVersion
{
    local REPO=${1}
    local BRANCH=${2}
    local DEFAULT_DOCKER_IMAGE_NAME=${3:-}
    local DEFAULT_DOCKER_IMAGE_VERSION=${4:-}

    local REPO_RELEASE_DATA
    REPO_RELEASE_DATA=$(
        curl -qL --silent \
            "https://github.com/onap/policy-parent/raw/${BRANCH}/integration/src/main/resources/release/pf_release_data.csv" |
        grep "^policy/$REPO"
    )

    local repo latest_released_tag latest_snapshot_tag changed_files docker_images
    # shellcheck disable=SC2034
    read -r repo \
        latest_released_tag \
        latest_snapshot_tag \
        changed_files \
        docker_images \
        <<< "$(echo "$REPO_RELEASE_DATA" | tr ',' ' ' )"

    if [[ -z "$docker_images" ]]; then
        if [[ -z "$DEFAULT_DOCKER_IMAGE_NAME" ]]; then
            echo "repo $REPO does not produce a docker image, execution terminated"
            exit 1
        else
            docker_images="$DEFAULT_DOCKER_IMAGE_NAME"
        fi
    fi

    docker_image_version=$latest_snapshot_tag
    docker_image_name=$(echo "$docker_images" | sed -e "s/^.*://" -e "s/^.//" -e "s/.$//")

    if \
        curl -qL --silent \
            "https://nexus3.onap.org/service/rest/repository/browse/docker.snapshot/v2/onap/$docker_image_name/tags/" |
            grep -q "$docker_image_version"
    then
        echo "using \"$docker_image_name:$docker_image_version\" docker image for repo \"$repo\""
        return
    fi

    docker_image_version="$latest_released_tag"
    if \
        curl -qL --silent \
            "https://nexus3.onap.org/service/rest/repository/browse/docker.release/v2/onap/$docker_image_name/tags/" |
            grep -q "$docker_image_version"
    then
        echo "using \"$docker_image_name:$docker_image_version\" docker image for repo \"$repo\""
        return
    fi

    docker_image_version="$DEFAULT_DOCKER_IMAGE_VERSION"
    if \
        curl -qL --silent \
            "https://nexus3.onap.org/service/rest/repository/browse/docker.release/v2/onap/$docker_image_name/tags/" |
            grep -q "$docker_image_version"
    then
        echo "using \"$docker_image_name:$docker_image_version\" docker image for repo \"$repo\""
        return
    else
        echo "docker image \"$docker_image_name:$docker_image_version\" not found for repo \"$repo\""
        exit 1
    fi
}

# --- Parse arguments ---
MODE=""
ACM_ARG=""
PPNT_ARG=""

while [[ $# -gt 0 ]]; do
    case "$1" in
        --local)
            MODE="local"
            shift
            ;;
        --branch)
            MODE="branch"
            shift
            ACM_ARG="${1:?'--branch requires at least one branch argument'}"
            shift
            if [[ $# -gt 0 && "$1" != --* ]]; then
                PPNT_ARG="$1"
                shift
            else
                PPNT_ARG="$ACM_ARG"
            fi
            ;;
        --version)
            MODE="version"
            shift
            ACM_ARG="${1:?'--version requires at least one version argument'}"
            shift
            if [[ $# -gt 0 && "$1" != --* ]]; then
                PPNT_ARG="$1"
                shift
            else
                PPNT_ARG="$ACM_ARG"
            fi
            ;;
        *)
            shift
            ;;
    esac
done

# Also honour legacy USE_LOCAL_IMAGES env var
if [ -z "$MODE" ] && [ "${USE_LOCAL_IMAGES}" = "true" ]; then
    MODE="local"
fi

# --- Apply mode ---
case "$MODE" in
    local)
        echo "Running with local images."
        export POLICY_CLAMP_VERSION="latest"
        export POLICY_CLAMP_PPNT_VERSION="latest"
        export CONTAINER_LOCATION=""
        ;;
    version)
        echo "Using explicit versions: ACM=$ACM_ARG PPNT=$PPNT_ARG"
        export POLICY_CLAMP_VERSION="$ACM_ARG"
        export POLICY_CLAMP_PPNT_VERSION="$PPNT_ARG"
        export CONTAINER_LOCATION="nexus3.onap.org:10001/"
        ;;
    branch)
        echo "Resolving versions from branches: ACM=$ACM_ARG PPNT=$PPNT_ARG"
        export CONTAINER_LOCATION="nexus3.onap.org:10001/"

        getDockerVersion clamp "$ACM_ARG"
        export POLICY_CLAMP_VERSION="$docker_image_version"

        getDockerVersion clamp "$PPNT_ARG"
        export POLICY_CLAMP_PPNT_VERSION="$docker_image_version"
        ;;
    *)
        # Default: resolve from .gitreview branch.
        export CONTAINER_LOCATION="nexus3.onap.org:10001/"

        if [ -z "$POLICY_CLAMP_VERSION" ]; then
            GERRIT_BRANCH=$(awk -F= '$1 == "defaultbranch" { print $2 }' \
                                "${WORKSPACE}"/.gitreview)
            echo "Resolving versions from default branch: $GERRIT_BRANCH"
            getDockerVersion clamp "$GERRIT_BRANCH"
            export POLICY_CLAMP_VERSION="$docker_image_version"
        fi
        export POLICY_CLAMP_PPNT_VERSION=${POLICY_CLAMP_PPNT_VERSION:-$POLICY_CLAMP_VERSION}
        ;;
esac

echo "POLICY_CLAMP_VERSION=$POLICY_CLAMP_VERSION"
echo "POLICY_CLAMP_PPNT_VERSION=$POLICY_CLAMP_PPNT_VERSION"
