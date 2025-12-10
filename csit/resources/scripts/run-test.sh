#!/bin/bash
#
# ============LICENSE_START====================================================
#  Copyright (C) 2023-2025 OpenInfra Foundation Europe. All rights reserved.
#  Modifications Copyright 2024 Deutsche Telekom
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

echo "Invoking the robot tests from: ${ROBOT_FILE}"

DEFAULT_PORT=6969
DATA=/opt/robotworkspace/models/models-examples/src/main/resources/policies
NODETEMPLATES=/opt/robotworkspace/models/models-examples/src/main/resources/nodetemplates

POLICY_API_IP=policy-api:${DEFAULT_PORT}

POLICY_PAP_IP=policy-pap:${DEFAULT_PORT}

APEX_IP=policy-apex-pdp:${DEFAULT_PORT}
APEX_EVENTS_IP=policy-apex-pdp:23324

POLICY_PDPX_IP=policy-xacml-pdp:${DEFAULT_PORT}
POLICY_OPA_IP=policy-opa-pdp:8282

POLICY_DROOLS_IP=policy-drools-pdp:9696
DROOLS_IP_1=policy-drools-apps:${DEFAULT_PORT}
DROOLS_IP_2=policy-drools-apps:9696

DISTRIBUTION_IP=policy-distribution:${DEFAULT_PORT}

POLICY_RUNTIME_ACM_IP=policy-clamp-runtime-acm:${DEFAULT_PORT}
HTTP_PARTICIPANT_SIM1_IP=policy-clamp-ac-sim-ppnt-1:${DEFAULT_PORT}
HTTP_PARTICIPANT_SIM2_IP=policy-clamp-ac-sim-ppnt-2:${DEFAULT_PORT}
HTTP_PARTICIPANT_SIM3_IP=policy-clamp-ac-sim-ppnt-3:${DEFAULT_PORT}
JAEGER_IP=jaeger:16686

KAFKA_IP=kafka:9092
PROMETHEUS_IP=prometheus:9090

DIST_TEMP_FOLDER=/tmp/distribution

ROBOT_VARIABLES="-v DATA:${DATA}
-v NODETEMPLATES:${NODETEMPLATES}
-v POLICY_API_IP:${POLICY_API_IP}
-v POLICY_RUNTIME_ACM_IP:${POLICY_RUNTIME_ACM_IP}
-v HTTP_PARTICIPANT_SIM1_IP:$HTTP_PARTICIPANT_SIM1_IP
-v HTTP_PARTICIPANT_SIM2_IP:$HTTP_PARTICIPANT_SIM2_IP
-v HTTP_PARTICIPANT_SIM3_IP:$HTTP_PARTICIPANT_SIM3_IP
-v POLICY_PAP_IP:${POLICY_PAP_IP}
-v APEX_IP:${APEX_IP}
-v APEX_EVENTS_IP:${APEX_EVENTS_IP}
-v KAFKA_IP:${KAFKA_IP}
-v PROMETHEUS_IP:${PROMETHEUS_IP}
-v POLICY_PDPX_IP:${POLICY_PDPX_IP}
-v POLICY_OPA_IP:${POLICY_OPA_IP}
-v POLICY_DROOLS_IP:${POLICY_DROOLS_IP}
-v DROOLS_IP:${DROOLS_IP_1}
-v DROOLS_IP_2:${DROOLS_IP_2}
-v TEMP_FOLDER:${DIST_TEMP_FOLDER}
-v DISTRIBUTION_IP:${DISTRIBUTION_IP}
-v TEST_ENV:${TEST_ENV}
-v JAEGER_IP:${JAEGER_IP}"

export ROBOT_VARIABLES

echo "Run Robot test"
echo ROBOT_VARIABLES="${ROBOT_VARIABLES}"
echo "Starting Robot test suites ..."
mkdir -p /tmp/results/
python3 -m robot.run --name "${PROJECT}" -d /tmp/results/ ${ROBOT_VARIABLES} ${ROBOT_FILE}
RESULT=$?
echo "RESULT: ${RESULT}"

exit $RESULT
