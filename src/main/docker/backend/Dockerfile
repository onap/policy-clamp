###
# ============LICENSE_START=======================================================
# ONAP CLAMP
# ================================================================================
# Copyright (C) 2018 AT&T Intellectual Property. All rights
#                             reserved.
# ================================================================================
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# ============LICENSE_END============================================
# ===================================================================
#
###

FROM onap/integration-java11:7.1.0

MAINTAINER "The Onap Team"
LABEL Description="This image contains alpine, openjdk 11 and clamp"

ARG http_proxy
ARG https_proxy
ENV HTTP_PROXY=$http_proxy
ENV HTTPS_PROXY=$https_proxy
ENV http_proxy=$HTTP_PROXY
ENV https_proxy=$HTTPS_PROXY
USER root
RUN mkdir /opt/clamp
RUN chown -R onap:onap /opt/clamp

VOLUME /opt/clamp/config

COPY --chown=onap:onap onap-clamp-backend/clamp.jar /opt/clamp/app.jar

USER onap
WORKDIR /opt/clamp/
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-XX:MinRAMPercentage=50 -XX:MaxRAMPercentage=75","-jar" ,"./app.jar"]