#! /bin/bash

# ============LICENSE_START====================================================
#  Copyright (C) 2023-2024 Nordix Foundation. All rights reserved.
#  Modifications Copyright 2024-2025 Deutsche Telekom
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

# Fetches the latest snapshot tags of policy components and updates the values.yaml in policy helm chart.

if [ -z "${WORKSPACE}" ]; then
    WORKSPACE=$(git rev-parse --show-toplevel)
    export WORKSPACE
fi

VALUES_YML=${WORKSPACE}/helm/policy/values.yaml

policy_components=(policy-api policy-pap policy-apex-pdp policy-models-simulator policy-pdpd-cl policy-xacml-pdp policy-distribution policy-db-migrator policy-opa-pdp)

clamp_components=(policy-clamp-runtime-acm policy-clamp-ac-kserve-ppnt policy-clamp-ac-k8s-ppnt policy-clamp-ac-pf-ppnt policy-clamp-ac-http-ppnt policy-clamp-ac-sim-ppnt policy-clamp-ac-a1pms-ppnt)

version_tags=$(source ${WORKSPACE}/compose/get-versions.sh)
export version_tags

function update_yaml() {
   local version=$(cut -d ":" -f2 <<< $(echo $version_tags | tr ' ' '\n' | grep "$1:" | tr -d '"'))
   echo "$2:$version"
   sed -i -e "s#onap/$2:[^=&]*#onap/$2:$version#g" $VALUES_YML
}

function update_image_tags() {
    sub_components=("$@")
    for sub_component in ${sub_components[@]}
    do
        if [[ $1 == 'clamp' ]]
        then
	    component=policy-clamp-ac-sim-ppnt
        elif [[ $1 == 'policy' ]]
	then
            component=$sub_component
	fi
        update_yaml $component $sub_component
    done
}

echo "Update the latest image tags:"
update_image_tags policy ${policy_components[@]}
update_image_tags clamp ${clamp_components[@]}
