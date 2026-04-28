#!/bin/bash
# ============LICENSE_START=======================================================
#  Copyright (C) 2025-2026 OpenInfra Foundation Europe.
# ================================================================================
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
# ============LICENSE_END=========================================================

WORKSPACE=$(git rev-parse --show-toplevel)
export WORKSPACE

export ROBOT_FILE=""

PROJECT="$1"
CSIT_SCRIPT="scripts/run-test.sh"
ROBOT_DOCKER_IMAGE="policy-csit-robot"
ROBOT_LOG_DIR=${WORKSPACE}/csit/archives

export POLICY_CLAMP_ROBOT="clamp-health-check.robot clamp-db-restore.robot clamp-single-element-test.robot clamp-timeout-test.robot clamp-migrate-rollback.robot clamp-trace-test.robot clamp-slas.robot"
export POLICY_CLAMP_CONTAINER="policy-clamp-runtime-acm"
export POLICY_HTTP_PPNT_CONTAINER="policy-clamp-ac-http-ppnt"
export POLICY_SIM_PPNT_CONTAINER1="policy-clamp-ac-sim-ppnt-1"
export POLICY_SIM_PPNT_CONTAINER2="policy-clamp-ac-sim-ppnt-2"
export POLICY_SIM_PPNT_CONTAINER3="policy-clamp-ac-sim-ppnt-3"
export JAEGER_CONTAINER="jaeger"

function set_project_config() {
    echo "Setting project configuration for: $PROJECT"
    export ROBOT_FILE=$POLICY_CLAMP_ROBOT
    export READINESS_CONTAINERS=($POLICY_CLAMP_CONTAINER,$POLICY_HTTP_PPNT_CONTAINER,$POLICY_SIM_PPNT_CONTAINER1,
        $POLICY_SIM_PPNT_CONTAINER2,$POLICY_SIM_PPNT_CONTAINER3,$JAEGER_CONTAINER)
}

function build_robot_image() {
    echo "Build docker image for robot framework"
    cd "${WORKSPACE}"/csit/resources || exit
    echo "Build robot framework docker image"
    sudo apt install gnupg2 pass -y
    export DOCKERPW=docker
    echo "$DOCKERPW" | docker login -u docker --password-stdin nexus3.onap.org:10001
    docker build . --file Dockerfile \
        --build-arg CSIT_SCRIPT="$CSIT_SCRIPT" \
        --build-arg ROBOT_FILE="$ROBOT_FILE" \
        --tag "${ROBOT_DOCKER_IMAGE}" --no-cache
    echo "---------------------------------------------"
}

function print_robot_log() {
    count_pods=0
    while [[ ${count_pods} -eq 0 ]]; do
        echo "Waiting for pods to come up..."
        sleep 5
        count_pods=$(kubectl get pods --output name | wc -l)
    done
    robotpod=$(kubectl get po | grep policy-csit)
    podName=$(echo "$robotpod" | awk '{print $1}')
    echo "The robot tests will begin once the policy components {${READINESS_CONTAINERS[*]}} are up and running..."
    kubectl wait --for=jsonpath='{.status.phase}'=Running --timeout=18m pod/"$podName"
    echo "Policy deployment status:"
    kubectl get po
    kubectl get all -A
    echo "Robot Test logs:"
    kubectl logs -f "$podName"
}

function start_csit() {
    build_robot_image
    if [ "${?}" -eq 0 ]; then
        echo "Importing robot image into microk8s registry"
        docker save -o policy-csit-robot.tar ${ROBOT_DOCKER_IMAGE}:latest
        sudo microk8s ctr image import policy-csit-robot.tar
        rm -rf "${WORKSPACE}"/csit/resources/policy-csit-robot.tar
        echo "---------------------------------------------"
        echo "Installing Robot framework pod for running CSIT"
        cd "${WORKSPACE}"/helm || exit
        mkdir -p "${ROBOT_LOG_DIR}"
        helm install csit-robot robot --set robot="$ROBOT_FILE" --set "readiness={$(echo "${READINESS_CONTAINERS}" | sed 's/[{}]//g' | sed 's/,$//')}" --set robotLogDir=$ROBOT_LOG_DIR
        print_robot_log
    fi
}

if [ "$PROJECT" ]; then
    set_project_config "$PROJECT"
    export ROBOT_LOG_DIR=${WORKSPACE}/csit/archives/${PROJECT}
    echo "CSIT will be invoked from $ROBOT_FILE"
    echo "Readiness containers: ${READINESS_CONTAINERS[*]}"
    echo "-------------------------------------------"
    start_csit
else
    echo "No project supplied for running CSIT"
fi
