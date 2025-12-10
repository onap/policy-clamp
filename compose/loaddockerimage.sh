#! /bin/bash -x

# ============LICENSE_START====================================================
#  Copyright (C) 2023 Nordix Foundation. All rights reserved.

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

# This script gets executed in Nordix infra for consuming locally generated docker images for k8s tests.

if [ -z "${WORKSPACE}" ]; then
    WORKSPACE=$(git rev-parse --show-toplevel)
    export WORKSPACE
fi



echo "Setting project configuration for: $PROJECT"
case $PROJECT in

clamp | policy-clamp)
    echo "CLAMP"
    env
    if [ "$(docker images nexus3.onap.org:10001/onap/policy-clamp-runtime-acm:$POLICY_CLAMP_VERSION | grep -v REPOSITORY | wc -l)" == 1 ];  then
        docker save nexus3.onap.org:10001/onap/policy-clamp-runtime-acm:$POLICY_CLAMP_VERSION -o ./runtime_image.tar
        microk8s images import ./runtime_image.tar
    fi
    if [ "$(docker images nexus3.onap.org:10001/onap/policy-clamp-ac-http-ppnt:$POLICY_CLAMP_VERSION | grep -v REPOSITORY | wc -l)" == 1 ];  then
        docker save nexus3.onap.org:10001/onap/policy-clamp-ac-http-ppnt:$POLICY_CLAMP_VERSION -o ./http_ppnt_image.tar
        microk8s images import ./http_ppnt_image.tar
    fi
    if [ "$(docker images nexus3.onap.org:10001/onap/policy-clamp-ac-k8s-ppnt:$POLICY_CLAMP_VERSION | grep -v REPOSITORY | wc -l)" == 1 ];  then
        docker save nexus3.onap.org:10001/onap/policy-clamp-ac-k8s-ppnt:$POLICY_CLAMP_VERSION -o ./k8s_ppnt_image.tar
        microk8s images import ./k8s_ppnt_image.tar
    fi
    if [ "$(docker images nexus3.onap.org:10001/onap/policy-clamp-ac-pf-ppnt:$POLICY_CLAMP_VERSION | grep -v REPOSITORY | wc -l)" == 1 ];  then
        docker save nexus3.onap.org:10001/onap/policy-clamp-ac-pf-ppnt:$POLICY_CLAMP_VERSION -o ./pf_ppnt_image.tar
        microk8s images import ./pf_ppnt_image.tar
    fi
    ;;

api | policy-api)
    echo "API"
    env
    if [ "$(docker images nexus3.onap.org:10001/onap/policy-api:$POLICY_API_VERSION | grep -v REPOSITORY | wc -l)" == 1 ];  then
        docker save nexus3.onap.org:10001/onap/policy-api:$POLICY_API_VERSION -o ./image.tar
        microk8s images import ./image.tar
    fi
    ;;

pap | policy-pap)
    echo "PAP"
    env
    if [ "$(docker images nexus3.onap.org:10001/onap/policy-pap:$POLICY_PAP_VERSION | grep -v REPOSITORY | wc -l)" == 1 ];  then
        docker save nexus3.onap.org:10001/onap/policy-pap:$POLICY_PAP_VERSION -o ./image.tar
        microk8s images import ./image.tar
    fi
    ;;

apex-pdp | policy-apex-pdp)
    echo "APEX"
    env
    if [ "$(docker images nexus3.onap.org:10001/onap/policy-apex-pdp:$POLICY_APEX_PDP_VERSION | grep -v REPOSITORY | wc -l)" == 1 ];  then
        docker save nexus3.onap.org:10001/onap/policy-apex-pdp:$POLICY_APEX_PDP_VERSION -o ./image.tar
        microk8s images import ./image.tar
    fi
    ;;

*)
    echo "UNKNOWN"
    ;;
esac
