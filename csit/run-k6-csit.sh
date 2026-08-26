#!/bin/bash
#
# ============LICENSE_START====================================================
#  Copyright (C) 2026 OpenInfra Foundation Europe. All rights reserved.
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
#
# Standalone k6 CSIT driver — mirrors run-project-csit.sh but runs k6 tests
# instead of Robot tests.
#
# Usage:
#   ./run-k6-csit.sh [clamp|clamp-performance] [--no-exit] [--stop] [--local]
#
# Test suites:
#   clamp             — health-check + single-element (functionalOptions, 1 VU)
#   clamp-performance — performance (performanceOptions, ramp-up VUs)

DO_TEARDOWN=true
SKIP_TEST=false

# even with forced finish, clean up docker containers
function on_exit() {
    if [ "${DO_TEARDOWN}" = true ]; then
        source "${DOCKER_COMPOSE_DIR}"/stop-compose.sh "${PROJECT}"
        mv "${DOCKER_COMPOSE_DIR}"/*.log "${K6_LOG_DIR}" 2>/dev/null || true
    fi
    exit $RC
}

function run_k6() {
    local script=$1
    local out_name
    out_name=$(basename "${script}" .js)

    echo "--- Running k6: ${script} ---"

    k6 run \
        -e POLICY_RUNTIME_ACM_IP="localhost:${ACM_PORT}" \
        -e HTTP_PARTICIPANT_SIM1_IP="localhost:${SIM_PARTICIPANT1_PORT}" \
        -e POLICY_HTTP_PARTICIPANT="localhost:${HTTP_PPNT_PORT}" \
        --out "json=${K6_LOG_DIR}/${out_name}.json" \
        "${WORKSPACE}/csit/k6/${script}"

    local exit_code=$?
    if [ ${exit_code} -eq 0 ]; then
        echo "k6 ${out_name} PASSED"
    else
        echo "k6 ${out_name} FAILED (exit ${exit_code})"
        RC=${exit_code}
    fi
}

function run_k6_functional() {
    run_k6 "health-check.js"
    run_k6 "single-element.js"
}

function run_k6_performance() {
    run_k6 "performance.js"
}

trap on_exit EXIT

# Parse arguments
while [[ $# -gt 0 ]]; do
    case "$1" in
    --local)
        export USE_LOCAL_IMAGES=true
        shift
        ;;
    --no-exit)
        DO_TEARDOWN=false
        shift
        ;;
    --stop)
        on_exit
        ;;
    --skip-test)
        SKIP_TEST=true
        DO_TEARDOWN=false
        shift
        ;;
    *)
        PROJECT="${1}"
        shift
        ;;
    esac
done

PROJECT="${PROJECT:-clamp}"
RC=0

if [ -z "${WORKSPACE}" ]; then
    WORKSPACE=$(git rev-parse --show-toplevel)
    export WORKSPACE
fi

export DOCKER_COMPOSE_DIR="${WORKSPACE}/compose"
export K6_LOG_DIR="${WORKSPACE}/csit/archives/${PROJECT}"

sudo rm -rf "${K6_LOG_DIR}"
mkdir -p "${K6_LOG_DIR}"

# Verify k6 is installed
if ! command -v k6 &> /dev/null; then
    echo "ERROR: k6 is not installed. Install from https://grafana.com/docs/k6/latest/set-up/install-k6/"
    exit 1
fi

export ACM_REPLICAS=2
source "${DOCKER_COMPOSE_DIR}"/start-compose.sh policy-clamp-runtime-acm --grafana

source "${DOCKER_COMPOSE_DIR}"/export-ports.sh > /dev/null 2>&1

if [ "${SKIP_TEST}" = true ]; then
    echo "Skipping tests (--skip-test)"
else
    case $PROJECT in
    clamp-performance)
        run_k6_performance
        ;;
    *)
        run_k6_functional
        ;;
    esac
fi
