#!/bin/bash
# ============LICENSE_START=======================================================
# Copyright 2023 Nordix Foundation.
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

if [ -z "${WORKSPACE}" ]; then
    WORKSPACE=$(git rev-parse --show-toplevel)
    export WORKSPACE
fi

GERRIT_BRANCH=$(awk -F= '$1 == "defaultbranch" { print $2 }' \
                    "${WORKSPACE}"/.gitreview)

echo GERRIT_BRANCH="${GERRIT_BRANCH}"

rm -rf "${WORKSPACE}"/models
mkdir "${WORKSPACE}"/models

# download models examples
git clone -b "${GERRIT_BRANCH}" --single-branch https://github.com/onap/policy-models.git \
    "${WORKSPACE}"/models

export DATA=${WORKSPACE}/models/models-examples/src/main/resources/policies

export NODETEMPLATES=${WORKSPACE}/models/models-examples/src/main/resources/nodetemplates

# create a couple of variations of the policy definitions
sed -e 's!Measurement_vGMUX!ADifferentValue!' \
        "${DATA}"/vCPE.policy.monitoring.input.tosca.json \
    >"${DATA}"/vCPE.policy.monitoring.input.tosca.v1_2.json

sed -e 's!"version": "1.0.0"!"version": "2.0.0"!' \
        -e 's!"policy-version": 1!"policy-version": 2!' \
        "${DATA}"/vCPE.policy.monitoring.input.tosca.json \
    >"${DATA}"/vCPE.policy.monitoring.input.tosca.v2.json
