#!/bin/bash -x
#
# Copyright 2024-2026 OpenInfra Foundation Europe. All rights reserved.
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

if [ -z "${WORKSPACE}" ]; then
    WORKSPACE=$(git rev-parse --show-toplevel)
    export WORKSPACE
fi

export ROBOT_DOCKER_IMAGE="policy-csit-robot"

cd "${WORKSPACE}"/csit/resources || exit

IMAGE_ID=$(docker images -q "onap/${ROBOT_DOCKER_IMAGE}")

if [ -n "$IMAGE_ID" ]; then
    echo "Image onap/${ROBOT_DOCKER_IMAGE} exists. Removing..."
    docker rmi "onap/${ROBOT_DOCKER_IMAGE}"
fi

echo "Building robot framework docker image"
docker build . --file Dockerfile --tag "onap/${ROBOT_DOCKER_IMAGE}" --quiet

docker save -o policy-csit-robot.tar "onap/${ROBOT_DOCKER_IMAGE}":latest

rm -rf "${WORKSPACE}"/csit/resources/policy-csit-robot.tar
rm -rf "${WORKSPACE}"/csit/resources/tests/models/
