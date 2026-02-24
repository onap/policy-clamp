#!/bin/bash
#
# Copyright 2016-2017 Huawei Technologies Co., Ltd.
# Modification Copyright 2019 © Samsung Electronics Co., Ltd.
# Modification Copyright 2021 © AT&T Intellectual Property.
# Modification Copyright 2021-2026 OpenInfra Foundation Europe.
# Modifications Copyright 2024-2025 Deutsche Telekom
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

SKIP_BUILDING_ROBOT_IMG=false
DO_NOT_TEARDOWN=false

# even with forced finish, clean up docker containers
function on_exit(){
    rm -rf "${CSAR_DIR}"/csar_temp.csar

    if [ "${DO_NOT_TEARDOWN}" = false ]; then
        source "${DOCKER_COMPOSE_DIR}"/stop-compose.sh "${PROJECT}"
        mv "${DOCKER_COMPOSE_DIR}"/*.log "${ROBOT_LOG_DIR}"
    fi

    exit $RC
}

function docker_stats(){
    # General memory details
    if [ "$(uname -s)" == "Darwin" ]
    then
        sh -c "top -l1 | head -10"
        echo
    else
        sh -c "top -bn1 | head -3"
        echo

        sh -c "free -h"
        echo
    fi

    # Memory details per Docker
    docker ps --format "table {{ .Image }}\t{{ .Names }}\t{{ .Status }}"
    echo

    docker stats --no-stream
    echo
}

function setup_clamp() {
    export ROBOT_FILES="clamp-health-check.robot clamp-db-restore.robot clamp-single-element-test.robot clamp-multiple-element-type-test.robot clamp-timeout-test.robot
    clamp-migrate-rollback.robot clamp-trace-test.robot clamp-slas.robot"
    export TEST_ENV="docker"
    export PROJECT="clamp"
    source "${DOCKER_COMPOSE_DIR}"/start-compose.sh policy-clamp-runtime-acm --grafana
}

function build_robot_image() {
    bash "${SCRIPTS}"/build-csit-docker-image.sh
    cd "${WORKSPACE}" || exit
}

function run_robot() {
    docker compose -f "${DOCKER_COMPOSE_DIR}"/compose.yaml up csit-tests
    export RC=$?
}

function set_project_config() {
    echo "Setting project configuration for: $PROJECT"
    case $PROJECT in

    clamp | policy-clamp)
        export ACM_REPLICAS=2
        setup_clamp
        ;;

    clamp-simple | policy-simple)
        export ACM_REPLICAS=1
        setup_clamp
        ;;

    *)
        echo "Unknown project supplied. No test will run."
        exit 1
        ;;
    esac
}

# ensure that teardown and other finalizing steps are always executed
trap on_exit EXIT

# start the script

# Parse the command-line arguments
while [[ $# -gt 0 ]]
do
  key="$1"

  case $key in
    --skip-build-csit)
      export SKIP_BUILDING_ROBOT_IMG=true
      shift
      ;;
    --local)
      export USE_LOCAL_IMAGES=true
      shift
      ;;
    --no-exit)
      export DO_NOT_TEARDOWN=true
      shift
      ;;
    --stop)
      export TEARDOWN=true
      shift
      ;;
    *)
      export PROJECT="${1}"
      shift
      ;;
  esac
done

# setup all directories used for test resources
if [ -z "${WORKSPACE}" ]; then
    WORKSPACE=$(git rev-parse --show-toplevel)
    export WORKSPACE
fi

export GERRIT_BRANCH=$(awk -F= '$1 == "defaultbranch" { print $2 }' "${WORKSPACE}"/.gitreview)
export ROBOT_LOG_DIR="${WORKSPACE}/csit/archives/${PROJECT}"
export SCRIPTS="${WORKSPACE}/csit/resources/scripts"
export CSAR_DIR="${WORKSPACE}/csit/resources/tests/data/csar"
export DOCKER_COMPOSE_DIR="${WORKSPACE}/compose"
export ROBOT_FILES=""

cd "${WORKSPACE}" || exit

# recreate the log folder with test results
rm -rf "${ROBOT_LOG_DIR}"
mkdir -p "${ROBOT_LOG_DIR}"

# log into nexus docker
docker login -u docker -p docker nexus3.onap.org:10001

# based on $PROJECT var, setup robot test files and docker compose execution
compose_version=$(docker compose version)

if [[ $compose_version == *"Docker Compose version"* ]]; then
    echo "$compose_version"
else
    echo "Docker Compose Plugin not installed. Installing now..."
    sudo mkdir -p /usr/local/lib/docker/cli-plugins
    sudo curl -SL https://github.com/docker/compose/releases/download/v2.29.1/docker-compose-linux-x86_64 -o /usr/local/lib/docker/cli-plugins/docker-compose
    sudo chmod +x /usr/local/lib/docker/cli-plugins/docker-compose
fi

if [ "${TEARDOWN}" == "true" ]; then
    on_exit
fi

set_project_config

unset http_proxy https_proxy

export ROBOT_FILES

# use a separate script to build a CSIT docker image, to isolate the test run
if [ "${SKIP_BUILDING_ROBOT_IMG}" == "true" ]; then
    echo "Skipping build csit robot image"
else
    build_robot_image
fi

docker_stats | tee "${ROBOT_LOG_DIR}/_sysinfo-1-after-setup.txt"

# start the CSIT container and run the tests
run_robot

docker ps --format "table {{ .Image }}\t{{ .Names }}\t{{ .Status }}"
