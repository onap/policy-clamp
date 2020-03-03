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

FROM onap/policy-jdk-debian:2.0.1

MAINTAINER "The Onap Team"
LABEL Description="This immage contains alpine, openjdk 11 and clamp"

ARG http_proxy
ARG https_proxy
ENV HTTP_PROXY=$http_proxy
ENV HTTPS_PROXY=$https_proxy
ENV http_proxy=$HTTP_PROXY
ENV https_proxy=$HTTPS_PROXY

RUN addgroup onap
RUN useradd -g onap clamp
VOLUME /opt/clamp/config
RUN mkdir /var/log/onap
RUN chmod a+rwx /var/log/onap

COPY onap-clamp-backend/clamp.jar /opt/clamp/app.jar
RUN chmod 700 /opt/clamp/app.jar

RUN chown -R clamp:onap /opt/clamp

USER clamp
WORKDIR /opt/clamp/
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-Xms256m", "-Xmx1g", "-jar" ,"./app.jar"]
