#!/bin/bash
#
# ============LICENSE_START====================================================
#  Copyright (C) 2022-2026 OpenInfra Foundation Europe. All rights reserved.
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

usage() {
  cat << EOF
Usage: $0 [policy-component] [OPTIONS]
  OPTIONS:
    --grafana    start docker compose with grafana
    --local      use local images instead of pulling from registry
    --help       display this help message

  If no policy-component is specified, all components will be started
EOF
}

if [ -z "${WORKSPACE}" ]; then
    WORKSPACE=$(git rev-parse --show-toplevel)
    export WORKSPACE
fi
COMPOSE_FOLDER="${WORKSPACE}"/compose

# Set default values for the options
grafana=false

# Parse the command-line arguments
while [[ $# -gt 0 ]]; do
  case "$1" in
    --help|-h)
      usage
      exit 0
      ;;
    --grafana)
      grafana=true
      shift
      ;;
    --local)
      export USE_LOCAL_IMAGES=true
      shift
      ;;
    *)
      component="$1"
      shift
      ;;
  esac
done

cd "${COMPOSE_FOLDER}"

echo "Configuring docker compose..."
source export-ports.sh > /dev/null 2>&1
source get-versions.sh > /dev/null 2>&1

if [ -n "$component" ]; then
  if [ "$grafana" = true ]; then
    docker compose up -d "${component}" grafana --wait
    echo "Prometheus server: http://localhost:${PROMETHEUS_PORT}"
    echo "Grafana server: http://localhost:${GRAFANA_PORT}"
  else
    docker compose up -d "${component}" --wait
  fi
else
  docker compose up -d --wait
fi

cd "${WORKSPACE}"
