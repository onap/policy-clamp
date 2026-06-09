#!/bin/bash
#
# Copyright 2024-2026 OpenInfra Foundation Europe.

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

# Script to run the ACM regression test suite using the Robot CSIT framework.
# Deploys ACM-R and participants in two different release branch/versions for testing backward compatibility.

function usage() {
  echo "Usage: $0 --release <acmr-release_branch> <ppnt-release_branch> | --version <acmr-version> <ppnt-version>"
  exit 1
}

# Legacy config files for releases up to 'quebec'
function find_release_profile() {
  if [[ "$1" == 'master' ]] || [[ "$(echo "$1" | cut -c1 )" > 'q' ]]; then
    echo "default"
  else
    echo "default,legacy"
  fi
}

# Legacy config files for versions before 9.0.2
function find_version_profile() {
  if [[ "$1" == 'latest' ]] || [[ "$(printf '%s\n' "$1" "9.0.2" | sort -V | head -n 1)" == "9.0.2" ]]; then
    echo "default"
  else
    echo "default,legacy"
  fi
}

function validate_version() {
    local version=$1
    if [[ "$1" != 'latest' ]] && [[ ! $version =~ ^[0-9]+\.[0-9]+\.[0-9]+(-SNAPSHOT)?$ ]]; then
        echo "Invalid version format: $version. Expected format: 'latest' or x.y.z where x, y, and z are numbers."
        usage
    fi
}

function validate_release() {
    local release=$1
    if [[ ! $release =~ ^[a-zA-Z._-]+$ ]]; then
        echo "Invalid release format: $release. Expected a string release name"
        usage
    fi
}

if [ "$#" -ne 0 ] && [ "$#" -ne 3 ]; then
  usage
fi

if [ -z "${WORKSPACE}" ]; then
    WORKSPACE=$(git rev-parse --show-toplevel)
    export WORKSPACE
fi

COMPOSE_FOLDER="${WORKSPACE}"/compose
DEFAULT_BRANCH=$(awk -F= '$1 == "defaultbranch" { print $2 }' "${WORKSPACE}"/.gitreview)

# Run from default branch
if [ $# -eq 0 ]; then
  echo "*** No release_branch/versions provided. Default branch will be used."
  source ${COMPOSE_FOLDER}/get-versions.sh --branch $DEFAULT_BRANCH $DEFAULT_BRANCH > /dev/null 2>&1
  echo "*** Starting Regression with ACM-R and PPNT from the default release branch $DEFAULT_BRANCH ***"
  export CLAMP_PROFILE=$(find_release_profile "$DEFAULT_BRANCH")
  export PPNT_PROFILE="$CLAMP_PROFILE"

# Run with specific release/version
else
  case $1 in
    --release)
      validate_release $2
      validate_release $3
      source ${COMPOSE_FOLDER}/get-versions.sh --branch $2 $3 > /dev/null 2>&1
      echo "*** Starting Regression with ACM-R from branch $2 and PPNT from branch $3 ***"
      export CLAMP_PROFILE=$(find_release_profile $2)
      export PPNT_PROFILE=$(find_release_profile $3)
      ;;
    --version)
      validate_version $2
      validate_version $3
      source ${COMPOSE_FOLDER}/get-versions.sh --version $2 $3 > /dev/null 2>&1
      echo "*** Starting Regression with ACM-R version $2 and PPNT version $3 ***"
      export CLAMP_PROFILE=$(find_version_profile "$2")
      export PPNT_PROFILE=$(find_version_profile "$3")
      ;;
    *)
      echo "Unknown parameter: $1"
      usage
      ;;
  esac
fi

echo "Using $CLAMP_PROFILE profile for ACM-R and $PPNT_PROFILE profile for PPNTS."

# Delegate to the CSIT framework with the clamp-regression project
exec "${WORKSPACE}/csit/run-project-csit.sh" clamp-regression
