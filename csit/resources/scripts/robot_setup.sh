#!/bin/bash
# ============LICENSE_START=======================================================
#  Copyright (C) 2025 OpenInfra Foundation Europe.
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

# Source the shared config script
source "$(dirname "$0")/config_setup.sh"

DISTRIBUTION_CSAR=${WORKSPACE}/csit/resources/tests/data/csar
DIST_TEMP_FOLDER=/tmp/distribution

function clone_models() {
    local retry_count=3
    local success=false
    cd tests || exit
    sudo rm -rf models/
    for ((i = 1; i <= retry_count; i++)); do
        git clone "https://gerrit.onap.org/r/policy/models" && success=true && break
        echo "Retrying git clone ($i/$retry_count)..."
        sleep 5
    done

    cd ../
    if [ "$success" = false ]; then
        echo "Error: failed to clone policy-models repository after $retry_count attempts"
        exit 1
    fi

    sed -e 's!Measurement_vGMUX!ADifferentValue!' \
        tests/models/models-examples/src/main/resources/policies/vCPE.policy.monitoring.input.tosca.json \
        >tests/models/models-examples/src/main/resources/policies/vCPE.policy.monitoring.input.tosca.v1_2.json

    sed -e 's!"version": "1.0.0"!"version": "2.0.0"!' \
        -e 's!"policy-version": 1!"policy-version": 2!' \
        tests/models/models-examples/src/main/resources/policies/vCPE.policy.monitoring.input.tosca.json \
        >tests/models/models-examples/src/main/resources/policies/vCPE.policy.monitoring.input.tosca.v2.json

}

function copy_csar_file() {
    zip -F "${DISTRIBUTION_CSAR}"/sample_csar_with_apex_policy.csar \
        --out "${DISTRIBUTION_CSAR}"/csar_temp.csar -q
    sudo rm -rf "${DIST_TEMP_FOLDER}"
    sudo mkdir "${DIST_TEMP_FOLDER}"
    sudo cp "${DISTRIBUTION_CSAR}"/csar_temp.csar "${DISTRIBUTION_CSAR}"/temp.csar
    sudo mv "${DISTRIBUTION_CSAR}"/temp.csar ${DIST_TEMP_FOLDER}/sample_csar_with_apex_policy.csar
}

function build_robot_image() {
    echo "Build docker image for robot framework"
    cd "${WORKSPACE}"/csit/resources || exit
    clone_models
    if [ "${PROJECT}" == "distribution" ] || [ "${PROJECT}" == "policy-distribution" ]; then
        copy_csar_file
    fi
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

function push_acelement_chart() {
    echo "Pushing acelement chart to the chartmuseum repo..."
    helm repo add policy-chartmuseum http://localhost:30208
    cd tests || exit
    local retry_count=3
    local success=false
    for ((i = 1; i <= retry_count; i++)); do
        git clone "https://gerrit.onap.org/r/policy/clamp" && success=true && break
        echo "Retrying git clone ($i/$retry_count)..."
        sleep 5
    done

    ACELEMENT_CHART=${WORKSPACE}/csit/resources/tests/clamp/examples/src/main/resources/clamp/acm/acelement-helm/acelement
    helm cm-push "$ACELEMENT_CHART" policy-chartmuseum
    helm repo update
    rm -rf "${WORKSPACE}"/csit/resources/tests/clamp/
    echo "-------------------------------------------"
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
        rm -rf "${WORKSPACE}"/csit/resources/tests/models/
        echo "---------------------------------------------"
        if [ "$PROJECT" == "clamp" ] || [ "$PROJECT" == "policy-clamp" ]; then
          POD_READY_STATUS="0/1"
          while [[ ${POD_READY_STATUS} != "1/1" ]]; do
            echo "Waiting for chartmuseum pod to come up..."
            sleep 5
            POD_READY_STATUS=$(kubectl get pods | grep -e "policy-chartmuseum" | awk '{print $2}')
          done
          push_acelement_chart
        fi
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
