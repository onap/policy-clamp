#!/bin/bash
#
# ============LICENSE_START====================================================
#  Copyright (C) 2023-2025 OpenInfra Foundation Europe. All rights reserved.
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

echo "Shut down started!"
if [ -z "${WORKSPACE}" ]; then
    WORKSPACE=$(git rev-parse --show-toplevel)
    export WORKSPACE
fi

database=postgres

while [[ $# -gt 0 ]]
do
  key="$1"

  case $key in
    --mariadb)
      database=mariadb
      shift
      ;;
    --postgres)
      database=postgres
      shift
      ;;
    *)
      component="$1"
      shift
      ;;
  esac
done

if [ -z "$component" ]; then
  export component=api
fi

COMPOSE_FOLDER="${WORKSPACE}"/compose

cd ${COMPOSE_FOLDER}

source export-ports.sh > /dev/null 2>&1
source get-versions.sh > /dev/null 2>&1

echo "Collecting logs from docker compose containers..."
rm -rf *.log

# this will collect logs by service instead of mixing all together
containers=$(docker compose ps --all --format '{{.Service}}')

IFS=$'\n' read -d '' -r -a item_list <<< "$containers"
for item in "${item_list[@]}"
do
    if [ -n "$item" ]; then
        docker compose logs $item >> $item.log
        if [ "${DONT_PRINT_LOGS}" == "false" ]; then
            cat $item.log
        fi
    fi
done

echo "Tearing down containers..."
docker compose down -v --remove-orphans

cd ${WORKSPACE}
