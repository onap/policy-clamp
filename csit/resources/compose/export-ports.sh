#!/bin/bash
# ============LICENSE_START=======================================================
# Copyright 2023-2025 OpenInfra Foundation Europe. All rights reserved.
# Modifications Copyright 2024 Deutsche Telekom
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

# EXPLICITLY ASSIGN PORTS FOR TESTING PURPOSES
export APEX_PORT=30001
export APEX_EVENTS_PORT=23324
export API_PORT=30002
export PAP_PORT=30003
export ACM_PORT=30007
export POLICY_PARTICIPANT_PORT=30008
export SIM_PARTICIPANT1_PORT=30011
export SIM_PARTICIPANT2_PORT=30013
export SIM_PARTICIPANT3_PORT=30014
export SIMULATOR_PORT=30904
export KAFKA_PORT=29092
export PROMETHEUS_PORT=30259
export GRAFANA_PORT=30269
