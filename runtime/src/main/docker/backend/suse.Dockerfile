#
# ============LICENSE_START=======================================================
# ONAP
# ================================================================================
# Copyright (C) 2022 Nordix Foundation.
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

FROM opensuse/leap:15.3

MAINTAINER "The Onap Team"
LABEL Description="This image contains opensuse, openjdk 11 and policy clamp"

ARG http_proxy
ARG https_proxy
ENV HTTP_PROXY=$http_proxy
ENV HTTPS_PROXY=$https_proxy
ENV http_proxy=$HTTP_PROXY
ENV https_proxy=$HTTPS_PROXY
ENV LANG=en_US.UTF-8 LANGUAGE=en_US:en LC_ALL=en_US.UTF-8
ENV JAVA_HOME=/usr/lib64/jvm/java-11-openjdk-11

USER root

RUN zypper -n -q install --no-recommends java-11-openjdk-headless netcat-openbsd && \
    zypper -n -q update; zypper -n -q clean --all && \
    groupadd --system onap && \
    useradd --system --shell /bin/sh -G onap onap && \
    mkdir -p /opt/policy/clamp /var/log/onap/clamp && \
    chown -R onap:onap /opt/policy/clamp /var/log/onap/clamp

VOLUME /opt/policy/clamp/config

COPY --chown=onap:onap onap-policy-clamp-backend/policy-clamp-backend.jar /opt/policy/clamp/policy-clamp-backend.jar

USER onap
WORKDIR /opt/policy/clamp/
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-XX:MinRAMPercentage=50.0","-XX:MaxRAMPercentage=75.0","-jar","./policy-clamp-backend.jar"]
