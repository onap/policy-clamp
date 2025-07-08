#!/usr/bin/env sh
#
# ============LICENSE_START=======================================================
#  Copyright (C) 2021-2022, 2025 OpenInfra Foundation Europe. All rights reserved.
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
#

if [ "$#" -eq 1 ]; then
    CONFIG_FILE=$1
fi

if [ -z "$CONFIG_FILE" ]; then
    CONFIG_FILE="${POLICY_HOME}/etc/A1pmsParticipantParameters.yaml"
fi

echo "Policy clamp A1 Pms participant config file: $CONFIG_FILE"

if [ -f "${POLICY_HOME}/etc/mounted/logback.xml" ]; then
    echo "overriding logback xml file"
    cp -f "${POLICY_HOME}"/etc/mounted/logback.xml "${POLICY_HOME}"/etc/
fi

"$JAVA_HOME"/bin/java \
    -Dlogging.config="${POLICY_HOME}/etc/logback.xml" \
    -Dserver.ssl.keyStore="${KEYSTORE}" \
    -Dserver.ssl.keyStorePassword="${KEYSTORE_PASSWD}" \
    -Djavax.net.ssl.trustStore="${TRUSTSTORE}" \
    -Djavax.net.ssl.trustStorePassword="${TRUSTSTORE_PASSWD}" \
    -jar /app/app.jar \
    --spring.config.location="${CONFIG_FILE}"
