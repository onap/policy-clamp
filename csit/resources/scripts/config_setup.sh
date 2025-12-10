#!/bin/bash
# ============LICENSE_START=======================================================
#  Copyright (C) 2025 Nordix Foundation. All rights reserved.
#  Modifications Copyright 2025 Deutsche Telekom
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

export POLICY_CLAMP_ROBOT="clamp-health-check.robot clamp-db-restore.robot clamp-single-element-test.robot clamp-timeout-test.robot clamp-migrate-rollback.robot clamp-trace-test.robot clamp-slas.robot"
export POLICY_API_ROBOT="api-test.robot api-slas.robot"
export POLICY_PAP_ROBOT="pap-test.robot pap-slas.robot"
export POLICY_APEX_PDP_ROBOT="apex-pdp-test.robot apex-slas.robot"
export POLICY_XACML_PDP_ROBOT="xacml-pdp-test.robot xacml-pdp-slas.robot"
export POLICY_OPA_PDP_ROBOT="opa-pdp-test.robot opa-pdp-slas.robot"
export POLICY_DROOLS_PDP_ROBOT="drools-pdp-test.robot"
export POLICY_DISTRIBUTION_ROBOT="distribution-test.robot"

export POLICY_API_CONTAINER="policy-api"
export POLICY_PAP_CONTAINER="policy-pap"
export POLICY_CLAMP_CONTAINER="policy-clamp-runtime-acm"
export POLICY_APEX_CONTAINER="policy-apex-pdp"
export POLICY_DROOLS_CONTAINER="policy-drools-pdp"
export POLICY_XACML_CONTAINER="policy-xacml-pdp"
export POLICY_OPA_CONTAINER="policy-opa-pdp"
export POLICY_DISTRIBUTION_CONTAINER="policy-distribution"
export POLICY_K8S_PPNT_CONTAINER="policy-clamp-ac-k8s-ppnt"
export POLICY_HTTP_PPNT_CONTAINER="policy-clamp-ac-http-ppnt"
export POLICY_SIM_PPNT_CONTAINER1="policy-clamp-ac-sim-ppnt-1"
export POLICY_SIM_PPNT_CONTAINER2="policy-clamp-ac-sim-ppnt-2"
export POLICY_SIM_PPNT_CONTAINER3="policy-clamp-ac-sim-ppnt-3"
export POLICY_PF_PPNT_CONTAINER="policy-clamp-ac-pf-ppnt"
export JAEGER_CONTAINER="jaeger"

function install_chartmuseum () {
    echo "---------------------------------------------"
    echo "Installing Chartmuseum helm repository..."
    helm repo add chartmuseum-git https://chartmuseum.github.io/charts
    helm repo update
    helm install policy-chartmuseum chartmuseum-git/chartmuseum --set env.open.DISABLE_API=false --set service.type=NodePort --set service.nodePort=30208
    helm plugin install https://github.com/chartmuseum/helm-push
    echo "---------------------------------------------"
}

function set_project_config() {
    echo "Setting project configuration for: $PROJECT"
    case $PROJECT in
    clamp | policy-clamp)
        export ROBOT_FILE=$POLICY_CLAMP_ROBOT
        export READINESS_CONTAINERS=($POLICY_CLAMP_CONTAINER,$POLICY_APEX_CONTAINER,$POLICY_PF_PPNT_CONTAINER,$POLICY_K8S_PPNT_CONTAINER,
            $POLICY_HTTP_PPNT_CONTAINER,$POLICY_SIM_PPNT_CONTAINER1,$POLICY_SIM_PPNT_CONTAINER2,$POLICY_SIM_PPNT_CONTAINER3,$JAEGER_CONTAINER)
        export SET_VALUES="--set $POLICY_CLAMP_CONTAINER.enabled=true --set $POLICY_APEX_CONTAINER.enabled=true
            --set $POLICY_PF_PPNT_CONTAINER.enabled=true --set $POLICY_K8S_PPNT_CONTAINER.enabled=true
            --set $POLICY_HTTP_PPNT_CONTAINER.enabled=true --set $POLICY_SIM_PPNT_CONTAINER1.enabled=true
            --set $POLICY_SIM_PPNT_CONTAINER2.enabled=true --set $POLICY_SIM_PPNT_CONTAINER3.enabled=true
            --set $JAEGER_CONTAINER.enabled=true"
        install_chartmuseum
        ;;
    api | policy-api)
        export ROBOT_FILE=$POLICY_API_ROBOT
        export READINESS_CONTAINERS=($POLICY_API_CONTAINER)
        ;;
    pap | policy-pap)
        export ROBOT_FILE=$POLICY_PAP_ROBOT
        export READINESS_CONTAINERS=($POLICY_APEX_CONTAINER,$POLICY_PAP_CONTAINER,$POLICY_API_CONTAINER,$POLICY_XACML_CONTAINER)
        export SET_VALUES="--set $POLICY_APEX_CONTAINER.enabled=true --set $POLICY_XACML_CONTAINER.enabled=true"
        ;;
    apex-pdp | policy-apex-pdp)
        export ROBOT_FILE=$POLICY_APEX_PDP_ROBOT
        export READINESS_CONTAINERS=($POLICY_APEX_CONTAINER,$POLICY_API_CONTAINER,$POLICY_PAP_CONTAINER)
        export SET_VALUES="--set $POLICY_APEX_CONTAINER.enabled=true"
        ;;
    xacml-pdp | policy-xacml-pdp)
        export ROBOT_FILE=($POLICY_XACML_PDP_ROBOT)
        export READINESS_CONTAINERS=($POLICY_API_CONTAINER,$POLICY_PAP_CONTAINER,$POLICY_XACML_CONTAINER)
        export SET_VALUES="--set $POLICY_XACML_CONTAINER.enabled=true"
        ;;
    opa-pdp | policy-opa-pdp)
        export ROBOT_FILE=($POLICY_OPA_PDP_ROBOT)
        export READINESS_CONTAINERS=($POLICY_API_CONTAINER,$POLICY_PAP_CONTAINER,$POLICY_OPA_CONTAINER)
        export SET_VALUES="--set $POLICY_OPA_CONTAINER.enabled=true"
        ;;
    drools-pdp | policy-drools-pdp)
        export ROBOT_FILE=($POLICY_DROOLS_PDP_ROBOT)
        export READINESS_CONTAINERS=($POLICY_DROOLS_CONTAINER)
        export SET_VALUES="--set $POLICY_DROOLS_CONTAINER.enabled=true"
        ;;
    distribution | policy-distribution)
        export ROBOT_FILE=($POLICY_DISTRIBUTION_ROBOT)
        export READINESS_CONTAINERS=($POLICY_APEX_CONTAINER,$POLICY_API_CONTAINER,$POLICY_PAP_CONTAINER,$POLICY_DISTRIBUTION_CONTAINER)
        export SET_VALUES="--set $POLICY_APEX_CONTAINER.enabled=true --set $POLICY_DISTRIBUTION_CONTAINER.enabled=true"
        ;;
    *)
        echo "Unknown project supplied. Enabling all policy charts for the deployment"
        export READINESS_CONTAINERS=($POLICY_APEX_CONTAINER,$POLICY_API_CONTAINER,$POLICY_PAP_CONTAINER,
                    $POLICY_DISTRIBUTION_CONTAINER,$POLICY_DROOLS_CONTAINER,$POLICY_XACML_CONTAINER,$POLICY_OPA_CONTAINER,
                    $POLICY_CLAMP_CONTAINER,$POLICY_PF_PPNT_CONTAINER,$POLICY_K8S_PPNT_CONTAINER,
                    $POLICY_HTTP_PPNT_CONTAINER,$POLICY_SIM_PPNT_CONTAINER1,$POLICY_SIM_PPNT_CONTAINER2,$POLICY_SIM_PPNT_CONTAINER3)
        export SET_VALUES="--set $POLICY_APEX_CONTAINER.enabled=true --set $POLICY_XACML_CONTAINER.enabled=true
            --set $POLICY_OPA_CONTAINER.enabled=true --set $POLICY_DISTRIBUTION_CONTAINER.enabled=true --set $POLICY_DROOLS_CONTAINER.enabled=true
            --set $POLICY_CLAMP_CONTAINER.enabled=true --set $POLICY_PF_PPNT_CONTAINER.enabled=true
            --set $POLICY_K8S_PPNT_CONTAINER.enabled=true --set $POLICY_HTTP_PPNT_CONTAINER.enabled=true
            --set $POLICY_K8S_PPNT_CONTAINER.enabled=true --set $POLICY_SIM_PPNT_CONTAINER1.enabled=true
            --set $POLICY_SIM_PPNT_CONTAINER2.enabled=true --set $POLICY_SIM_PPNT_CONTAINER3.enabled=true"
        ;;
    esac
}
