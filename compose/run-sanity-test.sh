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
usage() {
  cat << EOF
Usage: $0 [OPTIONS]
  OPTIONS:
    --local              use local images instead of pulling from registry
    --version <ver>      use a specific image version from nexus
    --branch <branch>    resolve versions from a specific release branch
    --help               display this help message
EOF
}

set -e

if [ -z "${WORKSPACE}" ]; then
    WORKSPACE=$(git rev-parse --show-toplevel)
    export WORKSPACE
fi

COMPOSE_FOLDER="${WORKSPACE}/compose"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --help|-h)
      usage
      exit 0
      ;;
    --local)
      export USE_LOCAL_IMAGES=true
      shift
      ;;
    *)
      shift
      ;;
  esac
done

cd "${COMPOSE_FOLDER}"

echo "============================================="
echo " ACM All Participants - Sanity Test"
echo "============================================="

source export-ports.sh > /dev/null 2>&1
source get-versions.sh

COMPOSE_CMD="docker compose -f compose.yaml -f compose-sanity-run.yaml"

echo ""
echo "Starting all OS participants..."
$COMPOSE_CMD up -d --wait
RC=$?

echo ""
echo "============================================="
echo " Container Status"
echo "============================================="
$COMPOSE_CMD ps

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
    $COMPOSE_CMD ps --format "table {{.Name}}\t{{.Status}}" | grep -v "healthy"
fi

echo ""
echo "To inspect logs:  $COMPOSE_CMD logs <service-name>"

exit $RC
