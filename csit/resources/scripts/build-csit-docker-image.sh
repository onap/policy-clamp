#!/bin/bash -x
#
# Copyright 2024-2025 OpenInfra Foundation Europe. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# SPDX-License-Identifier: Apache-2.0

function clone_models() {
    # download models examples
    git clone -b "${GERRIT_BRANCH}" --single-branch https://github.com/onap/policy-models.git "${WORKSPACE}"/csit/resources/tests/models

    # create a couple of variations of the policy definitions
    sed -e 's!Measurement_vGMUX!ADifferentValue!' \
        tests/models/models-examples/src/main/resources/policies/vCPE.policy.monitoring.input.tosca.json \
        >tests/models/models-examples/src/main/resources/policies/vCPE.policy.monitoring.input.tosca.v1_2.json

    sed -e 's!"version": "1.0.0"!"version": "2.0.0"!' \
        -e 's!"policy-version": 1!"policy-version": 2!' \
        tests/models/models-examples/src/main/resources/policies/vCPE.policy.monitoring.input.tosca.json \
        >tests/models/models-examples/src/main/resources/policies/vCPE.policy.monitoring.input.tosca.v2.json
}

if [ -z "${WORKSPACE}" ]; then
    WORKSPACE=$(git rev-parse --show-toplevel)
    export WORKSPACE
fi

GERRIT_BRANCH=$(awk -F= '$1 == "defaultbranch" { print $2 }' "${WORKSPACE}"/.gitreview)
export ROBOT_DOCKER_IMAGE="policy-csit-robot"

cd "${WORKSPACE}"/csit/resources || exit

IMAGE_ID=$(docker images -q "onap/${ROBOT_DOCKER_IMAGE}")

if [ -n "$IMAGE_ID" ]; then
    echo "Image onap/${ROBOT_DOCKER_IMAGE} exists. Removing..."
    docker rmi "onap/${ROBOT_DOCKER_IMAGE}"
fi

# get models
clone_models

echo "Building robot framework docker image"

has_buildx() {
    docker buildx version >/dev/null 2>&1
}

echo "Checking for Docker Buildx..."

if has_buildx; then
    echo "Docker Buildx is already installed."
else
    echo "Attempting to install Docker Buildx..."
    # Try enabling buildx via Docker CLI plugin
    if docker buildx install >/dev/null 2>&1; then
        echo "Docker Buildx enabled."
    else
        # Attempt installation depending on system
        if command -v docker >/dev/null 2>&1; then
            mkdir -p ~/.docker/cli-plugins
            BUILD_VERSION="v0.11.2" # change if needed
            curl -fsSL \
              "https://github.com/docker/buildx/releases/download/${BUILD_VERSION}/buildx-${BUILD_VERSION}.linux-amd64" \
              -o ~/.docker/cli-plugins/docker-buildx || true
            chmod +x ~/.docker/cli-plugins/docker-buildx
        fi

        if has_buildx; then
            echo "Docker Buildx installed successfully."
        else
            echo "Failed to install Docker Buildx. Falling back to normal docker build."
        fi
    fi
fi

if has_buildx; then
    echo "Building with Docker Buildx..."
    docker buildx build . --file Dockerfile  --tag "onap/${ROBOT_DOCKER_IMAGE}" --quiet
else
    echo "Building with normal Docker build..."
    docker build . --file Dockerfile  --tag "onap/${ROBOT_DOCKER_IMAGE}" --quiet
fi

docker save -o policy-csit-robot.tar "onap/${ROBOT_DOCKER_IMAGE}":latest

rm -rf "${WORKSPACE}"/csit/resources/policy-csit-robot.tar
rm -rf "${WORKSPACE}"/csit/resources/tests/models/
