#! /bin/bash
# ============LICENSE_START====================================================
#  Copyright (C) 2020-2021 AT&T Intellectual Property. All rights reserved.
#  Modification Copyright 2021-2025 Nordix Foundation.
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

if [ -z "${WORKSPACE}" ]; then
    WORKSPACE=$(git rev-parse --show-toplevel)
    export WORKSPACE
fi

#default values
export POLICY_POSTGRES_VER=17.7
echo POLICY_POSTGRES_VER=${POLICY_POSTGRES_VER}

if [ -n "${USE_LOCAL_IMAGES}" ] && [ "${USE_LOCAL_IMAGES}" = "true" ]; then
    echo "Running with local images."
    export POLICY_DOCKER_VERSION="latest"
    export POLICY_MODELS_VERSION="latest"
    export POLICY_API_VERSION="latest"
    export POLICY_PAP_VERSION="latest"
    export POLICY_APEX_PDP_VERSION="latest"
    export POLICY_CLAMP_VERSION="latest"
    export POLICY_CLAMP_PPNT_VERSION=$POLICY_CLAMP_VERSION
    export POLICY_DROOLS_APPS_VERSION="latest"
    export CONTAINER_LOCATION=""

else
    echo "Downloading latest released images..."
    export CONTAINER_LOCATION="nexus3.onap.org:10001/"
    GERRIT_BRANCH=$(awk -F= '$1 == "defaultbranch" { print $2 }' \
                        "${WORKSPACE}"/.gitreview)

    echo GERRIT_BRANCH="${GERRIT_BRANCH}"

    function getDockerVersion
    {
        REPO=${1}
        DEFAULT_DOCKER_IMAGE_NAME=${2:-}
        DEFAULT_DOCKER_IMAGE_VERSION=${3:-}

        REPO_RELEASE_DATA=$(
            curl -qL --silent \
                "https://github.com/onap/policy-parent/raw/$GERRIT_BRANCH/integration/src/main/resources/release/pf_release_data.csv" |
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

    getDockerVersion docker
    export POLICY_DOCKER_VERSION="$docker_image_version"

    getDockerVersion models
    export POLICY_MODELS_VERSION="$docker_image_version"

    getDockerVersion api
    export POLICY_API_VERSION="$docker_image_version"

    getDockerVersion pap
    export POLICY_PAP_VERSION="$docker_image_version"

    getDockerVersion apex-pdp
    export POLICY_APEX_PDP_VERSION="$docker_image_version"

    getDockerVersion clamp
    export POLICY_CLAMP_VERSION="$docker_image_version"
    export POLICY_CLAMP_PPNT_VERSION=$POLICY_CLAMP_VERSION
fi
