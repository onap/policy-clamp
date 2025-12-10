#!/bin/bash
#
# Copyright 2024 Nordix Foundation.

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

# Script to run the ACM regression test suite in cucumber.
# Deploys ACM-R and participants in two different release branch/versions for testing backward compatibility.

function usage() {
  echo "Usage: $0 --release <acmr-release_branch> <ppnt-release_branch> | --version <acmr-version> <ppnt-version>"
  exit 1
}

# Legacy config files for releases up to 'newdelhi'
function find_release_profile() {
  if [ $1 == 'master' ] || [[ "$(echo "$1" | cut -c1 )" > 'n' ]]; then
    echo "default"
  else
    echo "default,legacy"
  fi
}

# Legacy config files for versions before 8.0.0
function find_version_profile() {
  if [[ "$(printf '%s\n' "$1" "8.0.0" | sort -V | head -n 1)" == "8.0.0" ]]; then
    echo "default"
  else
    echo "default,legacy"
  fi
}

function validate_version() {
    local version=$1
    if [[ ! $version =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
        echo "Invalid version format: $version. Expected format: x.y.z where x, y, and z are numbers."
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

# Invalid input
if [ "$#" -ne 0 ] && [ "$#" -ne 3 ]; then
  usage
fi

if [ -z "${WORKSPACE}" ]; then
    WORKSPACE=$(git rev-parse --show-toplevel)
    export WORKSPACE
fi

export SCRIPTS="${WORKSPACE}/csit/resources/scripts"
COMPOSE_FOLDER="${WORKSPACE}"/compose
REGRESSION_FOLDER="${WORKSPACE}"/policy-regression-tests/policy-clamp-regression/
export PROJECT='clamp'
DEFAULT_BRANCH=$(awk -F= '$1 == "defaultbranch" { print $2 }' \
                            "${WORKSPACE}"/.gitreview)

# Run from default branch
if [ $# -eq 0 ]
then
  echo "Usage: $0 --release <acmr-release_branch> <ppnt-release_branch> | --version <acmr-version> <ppnt-version>"
  echo "*** No release_branch/versions provided. Default branch will be used."
  echo "Fetching image versions for all components..."
  source ${COMPOSE_FOLDER}/get-versions-regression.sh $DEFAULT_BRANCH $DEFAULT_BRANCH > /dev/null 2>&1
  echo "Starting Regression with ACM-R and PPNT from the default release branch $DEFAULT_BRANCH ***"
  export CLAMP_PROFILE=$(find_release_profile "$DEFAULT_BRANCH")
  export PPNT_PROFILE="$CLAMP_PROFILE"
  echo "Using $CLAMP_PROFILE profile for ACM-R and $PPNT_PROFILE profile for PPNTS."

# Run with specific release/version
elif [ "$#" -gt 0 ]
then
  case $1 in
    --release)
      validate_release $2
      validate_release $3
      echo "Fetching image versions for all components..."
      source ${COMPOSE_FOLDER}/get-versions-regression.sh $2 $3 > /dev/null 2>&1
      echo "*** Starting Regression with ACM-R from branch $2 and PPNT from branch $3 ***"
      export CLAMP_PROFILE=$(find_release_profile $2)
      export PPNT_PROFILE=$(find_release_profile $3)
      echo "Using $CLAMP_PROFILE profile for ACM-R and $PPNT_PROFILE profile for PPNTS." 
      ;;
    --version)
      validate_version $2
      validate_version $3
      echo "Fetching image versions for all components..."
      source ${COMPOSE_FOLDER}/get-versions-regression.sh $DEFAULT_BRANCH $DEFAULT_BRANCH > /dev/null 2>&1
      export POLICY_CLAMP_VERSION=$2
      export POLICY_CLAMP_PPNT_VERSION=$3
      echo "*** Starting Regression with ACM-R version $2 and PPNT version $3 ***"
      export CLAMP_PROFILE=$(find_version_profile "$2")
      export PPNT_PROFILE=$(find_version_profile "$3")
      echo "Using $CLAMP_PROFILE profile for ACM-R and $PPNT_PROFILE profile for PPNTS."
      ;;
    *)
      echo "Unknown parameter: $1"
      usage
      ;;
  esac
fi

echo "*** Configure docker compose and trigger deployment***"
cd ${COMPOSE_FOLDER}
docker login -u docker -p docker nexus3.onap.org:10001 > /dev/null 2>&1
source export-ports.sh > /dev/null 2>&1

export CONTAINER_LOCATION="nexus3.onap.org:10001/"

docker compose up -d "policy-clamp-runtime-acm"

# wait for the app to start up
"${SCRIPTS}"/wait_for_rest.sh localhost "${ACM_PORT}"

cd ${REGRESSION_FOLDER}

# Invoke the regression test cases
mvn clean test -Dtests.skip=false

cd ${COMPOSE_FOLDER}
source stop-compose.sh clamp
mv ${COMPOSE_FOLDER}/*.log ${REGRESSION_FOLDER}

cd ${REGRESSION_FOLDER}
