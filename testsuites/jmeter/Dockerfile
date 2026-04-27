#-------------------------------------------------------------------------------
# ============LICENSE_START=======================================================
#  Copyright (C) 2026 OpenInfra Foundation Europe. All rights reserved.
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
#-------------------------------------------------------------------------------

FROM onap/policy-jre-alpine:5.0.0-SNAPSHOT

ENV LOGS="/app/log"
ENV WORKSPACE="/app"
ENV JMETER_HOME="/app/apache-jmeter"

USER root
RUN mkdir -p $WORKSPACE $LOGS

WORKDIR $WORKSPACE

RUN curl -sO https://archive.apache.org/dist/jmeter/binaries/apache-jmeter-5.6.2.tgz \
    && tar -xzf apache-jmeter-5.6.2.tgz \
    && mv apache-jmeter-5.6.2 apache-jmeter \
    && rm -rf apache-jmeter/docs apache-jmeter/printable_docs \
    && rm apache-jmeter-5.6.2.tgz \
    && cd apache-jmeter/lib \
    && curl -O https://repo1.maven.org/maven2/kg/apc/cmdrunner/2.2.1/cmdrunner-2.2.1.jar \
    && curl -O https://repo1.maven.org/maven2/org/apache/kafka/kafka-clients/3.9.0/kafka-clients-3.9.0.jar \
    && curl -O https://repo1.maven.org/maven2/org/apache/kafka/kafka_2.13/3.9.0/kafka_2.13-3.9.0.jar

COPY setenv.sh ${WORKSPACE}/apache-jmeter/bin/setenv.sh
COPY run-test.sh ${WORKSPACE}/run-test.sh
RUN chmod 755 ${WORKSPACE}/run-test.sh

WORKDIR $WORKSPACE
ENTRYPOINT ["sh", "-c", "/app/run-test.sh"]
