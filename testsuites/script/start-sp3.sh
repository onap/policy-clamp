#!/bin/bash
# ============LICENSE_START=======================================================
#  Copyright (C) 2026 OpenInfra Foundation Europe. All rights reserved.
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

sudo rm -rf "${TESTDIR}/automate-s3p-test"
mkdir -p "${TESTDIR}/automate-s3p-test"
cd "${TESTDIR}/automate-s3p-test" || { log_message "ERROR" "Failed to change directory"; ((errors_encountered++)); exit 1; }

script_start_time=$(date +%s)
log_file="${TESTDIR}/automate-s3p-test/s3p_test_log_$(date +%Y%m%d_%H%M%S).log"
errors_encountered=0

# Function to log messages
log_message() {
    local level="$1"
    local message="$2"
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] [$level] $message" | tee -a "$log_file"
}

# Start Docker Compose
function start_docker() {
    log_message "INFO" "Starting Docker Compose for $PROJECT"
    cd $WORKSPACE/csit || { log_message "ERROR" "Failed to change directory"; ((errors_encountered++)); exit 1; }
    bash run-project-csit.sh $PROJECT --skip-test
}

# crete jmeter image
function crete_jmeter_image() {
    log_message "INFO" "Create JMeter Image"

    IMAGE_ID=$(docker images -q ${JMETER_DOCKER_IMAGE})

    if [ -n "$IMAGE_ID" ]; then
        echo "Image ${JMETER_DOCKER_IMAGE} exists. Removing..."
        docker rmi ${JMETER_DOCKER_IMAGE}
    fi

    cd ${TESTDIR}/jmeter || { log_message "ERROR" "Failed to change directory"; ((errors_encountered++)); exit 1; }
    docker build . --file Dockerfile  --tag ${JMETER_DOCKER_IMAGE} --quiet

    log_message "INFO" "JMeter image created"
}

# start jmeter container
function start_jmeter() {
    log_message "INFO" "Starting JMeter container"
    export JMETER_LOG_DIR="${TESTDIR}/automate-s3p-test/log"
    docker compose -f "${WORKSPACE}"/compose/compose.common.yml up jmeter-tests
}

function on_exit() {
    local exit_status=$?
    local end_time=$(date +%s)
    local runtime=$((end_time - script_start_time))

    log_message "INFO" "=============== Exit Report ==============="
    log_message "INFO" "Script execution completed at $(date)"
    log_message "INFO" "Exit status: $exit_status"
    log_message "INFO" "Total runtime: $runtime seconds"
    log_message "INFO" "Operations summary:"
    log_message "INFO" "  - Errors encountered: $errors_encountered"
    log_message "INFO" "Resource usage:"
    ps -p $$ -o %cpu,%mem,etime >> "$log_file"
    log_message "INFO" "Full log available at: $log_file"
    log_message "INFO" "Jmeter logs available at: $TESTDIR/automate-s3p-test/log"
    log_message "INFO" "============================================"
}

start_docker

export JMETER_DOCKER_IMAGE="onap/policy-jmeter"
crete_jmeter_image

log_message "INFO" "Executing tests"
start_jmeter

on_exit
