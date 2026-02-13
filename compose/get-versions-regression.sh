#! /bin/bash

# ============LICENSE_START====================================================
#  Copyright (C) 2024,2026 OpenInfra Foundation Europe. All rights reserved.
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


function getDockerVersion
{
    REPO=${1}
    GERRIT_BRANCH=${2}
    DEFAULT_DOCKER_IMAGE_NAME=${3:-}
    DEFAULT_DOCKER_IMAGE_VERSION=${4:-}

    REPO_RELEASE_DATA=$(
        curl -qL --silent \
            "https://github.com/onap/policy-parent/raw/${GERRIT_BRANCH}/integration/src/main/resources/release/pf_release_data.csv" |
        grep "^policy/$REPO"
    )

    # shellcheck disable=SC2034
    read -r repo \
        latest_released_tag \
        latest_snapshot_tag \
        changed_files \
        docker_images \
        <<< "$(echo "$REPO_RELEASE_DATA" | tr ',' ' ' )"

    if [[ -z "$docker_images" ]]
    then
        if [[ -z "$DEFAULT_DOCKER_IMAGE_NAME" ]]
        then
            echo "repo $REPO does not produce a docker image, execution terminated"
            exit 1
        else
            docker_images="$DEFAULT_DOCKER_IMAGE_NAME"
        fi
    fi

    # docker_image_version=$(echo "$latest_snapshot_tag" | awk -F \. '{print $1"."$2"-SNAPSHOT-latest"}')
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


if [ -z "${WORKSPACE}" ]; then
    WORKSPACE=$(git rev-parse --show-toplevel)
    export WORKSPACE
fi

if [ $# -eq 0 ]; then
    echo "No release versions provided. Fetching the default version for ACM and participants"
    echo "Usage: $0 <acm_release> <participant_release> ..."
    ACM_RELEASE=$(awk -F= '$1 == "defaultbranch" { print $2 }' \
                        "${WORKSPACE}"/.gitreview)
    PPNT_RELEASE=$ACM_RELEASE
fi
if [ $1 ]; then
    ACM_RELEASE=$1
fi

if [ $2 ]; then
    PPNT_RELEASE=$2
fi

export POLICY_MARIADB_VER=10.10.2
echo POLICY_MARIADB_VER=${POLICY_MARIADB_VER}

export POLICY_POSTGRES_VER=11.1
echo POLICY_POSTGRES_VER=${POLICY_POSTGRES_VER}

getDockerVersion docker $ACM_RELEASE
export POLICY_DOCKER_VERSION="$docker_image_version"

getDockerVersion api $ACM_RELEASE
export POLICY_API_VERSION="$docker_image_version"

getDockerVersion pap $ACM_RELEASE
export POLICY_PAP_VERSION="$docker_image_version"

getDockerVersion apex-pdp $ACM_RELEASE
export POLICY_APEX_PDP_VERSION="$docker_image_version"

getDockerVersion clamp $ACM_RELEASE
export POLICY_CLAMP_VERSION="$docker_image_version"

getDockerVersion clamp $PPNT_RELEASE
export POLICY_CLAMP_PPNT_VERSION="$docker_image_version"
