#!/bin/bash
# ============LICENSE_START=======================================================
#  Copyright (C) 2023-2025 Nordix Foundation. All rights reserved.
#  Modifications Copyright © 2024 Deutsche Telekom
# ================================================================================
#
#
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

# This script will be used to gather cluster information
# for JMeter to work towards the installed cluster

# EXPLICITLY ASSIGN PORTS FOR TESTING PURPOSES
export APEX_PORT=30001
export API_PORT=30002
export PAP_PORT=30003
export XACML_PORT=30004
export DROOLS_PORT=30005
export DIST_PORT=30006
export ACM_PORT=30007
export PF_PARTICIPANT_PORT=30008
export HTTP_PARTICIPANT_PORT=30009
export K8S_PARTICIPANT_PORT=30010
export SIM_PARTICIPANT_PORT=30011
export OPA_PORT=30012
export SIMULATOR_PORT=30904

# Retrieve pod names
function get_pod_name() {
  microk8s kubectl get pods --no-headers -o custom-columns=':metadata.name' | grep $1
}

function get_pod_names() {
  export APEX_POD=$(get_pod_name apex)
  export PAP_POD=$(get_pod_name pap)
  export API_POD=$(get_pod_name api)
  export DMAAP_POD=$(get_pod_name message-router)
  export XACML_POD=$(get_pod_name xacml)
  export OPA_POD=$(get_pod_name opa-pdp)
  export DROOLS_POD=$(get_pod_name drools-pdp)
  export DIST_POD=$(get_pod_name distribution)
  export ACM_POD=$(get_pod_name acm-runtime)
  export POLICY_PPNT_POD=$(get_pod_name policy-ppnt)
  export POLICY_HTTP_POD=$(get_pod_name http-ppnt)
  export POLICY_SIM_POD=$(get_pod_name sim-ppnt)
  export POLICY_K8S_POD=$(get_pod_name k8s-ppnt)
}

# Retrieve service names
function get_svc_name() {
  microk8s kubectl get svc --no-headers -o custom-columns=':metadata.name' | grep $1
}

function get_svc_names() {
  export APEX_SVC=$(get_svc_name policy-apex-pdp)
  export PAP_SVC=$(get_svc_name policy-pap)
  export API_SVC=$(get_svc_name policy-api)
  export DMAAP_SVC=$(get_svc_name message-router)
  export DROOLS_SVC=$(get_svc_name drools-pdp)
  export XACML_SVC=$(get_svc_name policy-xacml-pdp)
  export OPA_SVC=$(get_svc_name policy-opa-pdp)
  export DIST_SVC=$(get_svc_name policy-distribution)
  export ACM_SVC=$(get_svc_name policy-clamp-runtime-acm)
  export POLICY_PPNT_SVC=$(get_svc_name policy-clamp-ac-pf-ppnt)
  export POLICY_HTTP_SVC=$(get_svc_name policy-clamp-ac-http-ppnt)
  export POLICY_SIM_SVC=$(get_svc_name policy-clamp-ac-sim-ppnt)
  export POLICY_K8S_SVC=$(get_svc_name policy-clamp-ac-k8s-ppnt)
}

# Assign set port values
function patch_port() {
  microk8s kubectl patch service "$1-svc" --namespace=default --type='json' --patch='[{"op": "replace", "path": "/spec/ports/0/nodePort", "value":'"$2"'}]'
}

function patch_ports() {
  patch_port "$APEX_SVC" $APEX_PORT
  patch_port "$API_SVC" $API_PORT
  patch_port "$PAP_SVC" $PAP_PORT
  patch_port "$ACM_SVC" $ACM_PORT
  patch_port "$POLICY_PPNT_SVC" $PF_PARTICIPANT_PORT
  patch_port "$POLICY_HTTP_SVC" $HTTP_PARTICIPANT_PORT
  patch_port "$POLICY_SIM_SVC" $SIM_PARTICIPANT_PORT
  patch_port "$POLICY_K8S_SVC" $K8S_PARTICIPANT_PORT
  patch_port "$DIST_SVC" $DIST_PORT
  patch_port "$DROOLS_SVC" $DROOLS_PORT
  patch_port "$XACML_SVC" $XACML_PORT
  patch_port "$OPA_SVC" $OPA_PORT
}

function setup_message_router_svc() {
  microk8s kubectl expose service message-router --name message-router-svc --type NodePort --protocol TCP --port 3904 --target-port 3904
  microk8s kubectl patch service message-router-svc --namespace=default --type='json' --patch='[{"op": "replace", "path": "/spec/ports/0/nodePort", "value":'"$SIMULATOR_PORT"'}]'
}

# Expose services in order to perform tests from JMeter
function expose_service() {
  microk8s kubectl expose service $1 --name $1"-svc" --type NodePort --protocol TCP --port 6969 --target-port 6969
}

function expose_service_opa_pdp() {
  microk8s kubectl expose service $1 --name $1"-svc" --type NodePort --protocol TCP --port 8282 --target-port 8282
}

function expose_services() {
    expose_service $APEX_SVC
    expose_service $PAP_SVC
    expose_service $API_SVC
    expose_service $XACML_SVC
    expose_service $DROOLS_SVC
    expose_service $DIST_SVC
    expose_service $ACM_SVC
    expose_service $POLICY_PPNT_SVC
    expose_service $POLICY_HTTP_SVC
    expose_service $POLICY_SIM_SVC
    expose_service $POLICY_K8S_SVC
    expose_service_opa_pdp $OPA_SVC

    setup_message_router_svc
    sleep 2
    patch_ports
}

# Port forward Kafka to handle traffic to/from JMeter
function setup_kafka_connection() {
  # Get the Kafka pod name
  KAFKA_POD=$(kubectl get pods -l app=kafka -o jsonpath="{.items[0].metadata.name}")

  # Set up port forwarding
  kubectl port-forward pod/$KAFKA_POD 29092:29092 &
  PF_PID=$!

  # Wait for port forwarding to be established
  sleep 5

  KAFKA_POD_IP=$(kubectl get pod $KAFKA_POD -o jsonpath='{.status.podIP}')

  # Update hosts file
  echo "127.0.0.1 $KAFKA_POD" | sudo tee -a /etc/hosts

  export KAFKA_HOST="127.0.0.1"
  export KAFKA_PORT="29092"
}

function teardown_kafka_connection() {
  kill $PF_PID
  sudo sed -i "/$KAFKA_POD/d" /etc/hosts
}

####MAIN###
if [ "$1" = "teardown" ]; then
  teardown_kafka_connection
else
  get_pod_names
  get_svc_names
  expose_services
  setup_kafka_connection
fi