#!/bin/bash
#
# ============LICENSE_START====================================================
#  Copyright 2026 OpenInfra Foundation Europe. All rights reserved.
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
# Sanity test script to verify all OS participants can be deployed.
# Run before every OS release to confirm all images start and become healthy.
#
# Usage:
#   ./run-sanity-test.sh [--local|--version <ver>]
#
# Options:
#   --local           Use locally built "latest" images
#   --version <ver>   Use a specific image version from nexus

set -e

if [ -z "${WORKSPACE}" ]; then
    WORKSPACE=$(git rev-parse --show-toplevel)
    export WORKSPACE
fi

COMPOSE_FOLDER="${WORKSPACE}/compose"

cd "${COMPOSE_FOLDER}"

echo "============================================="
echo " ACM OS Participants - Pre-Release Sanity Test"
echo "============================================="

# Configure environment
source export-ports.sh
source get-versions.sh "$@"

echo ""
echo "Starting all OS participants..."
docker compose -f compose-sanity-run.yaml up -d --wait
RC=$?

echo ""
echo "============================================="
echo " Container Status"
echo "============================================="
docker compose -f compose-sanity-run.yaml ps

if [ $RC -eq 0 ]; then
    echo ""
    echo "============================================="
    echo " SANITY TEST PASSED - All participants healthy"
    echo "============================================="
else
    echo ""
    echo "============================================="
    echo " SANITY TEST FAILED - Some participants unhealthy"
    echo "============================================="
    docker compose -f compose-sanity-run.yaml ps --format "table {{.Name}}\t{{.Status}}" | grep -v "healthy"
fi

echo ""
echo "To inspect logs:  docker compose -f compose-sanity-run.yaml logs <service-name>"

exit $RC
