#!/bin/bash
#
# ============LICENSE_START====================================================
#  Copyright (C) 2022-2025 Nordix Foundation.
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

#Usage: $0 [policy-component] [OPTIONS]"
#"  OPTIONS:"
#"  --grafana              start the docker compose with grafana"
#"  --mariadb              start the docker compose using mariadb"
#"  --postgres [default]   start the docker compose using postgres db"
#"  no policy-component will start all components"

if [ -z "${WORKSPACE}" ]; then
    WORKSPACE=$(git rev-parse --show-toplevel)
    export WORKSPACE
fi
COMPOSE_FOLDER="${WORKSPACE}"/compose

# Set default values for the options
grafana=false
database=postgres

# Parse the command-line arguments
while [[ $# -gt 0 ]]
do
  key="$1"

  case $key in
    --grafana)
      grafana=true
      shift
      ;;
    --mariadb)
      database=mariadb
      shift
      ;;
    --postgres)
      database=postgres
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

cd ${COMPOSE_FOLDER}

echo "Configuring docker compose..."
source export-ports.sh > /dev/null 2>&1
source get-versions.sh > /dev/null 2>&1

# in case of csit running for PAP (groups should be for pap) but starts apex-pdp for dependencies.
if [ -z "$PROJECT" ]; then
  export PROJECT=$component
fi

export database

if [ -n "$component" ]; then
  if [ "$grafana" = true ]; then
    echo "Starting ${component} using ${database} + Grafana/Prometheus"
    docker compose up -d ${component} ${database} grafana

    echo "Prometheus server: http://localhost:${PROMETHEUS_PORT}"
    echo "Grafana server: http://localhost:${GRAFANA_PORT}"

  else
    echo "Starting ${component} using ${database}"
    docker compose up -d ${component} ${database}
  fi
else
  export PROJECT=api # api has groups.json complete with all 3 pdps
  echo "Starting all components using ${database}"
  docker compose up -d
fi

cd ${WORKSPACE}
