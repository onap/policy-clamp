###
# ============LICENSE_START=======================================================
# ONAP CLAMP
# ================================================================================
# Copyright (C) 2018, 2021 AT&T Intellectual Property. All rights reserved.
# Modifications Copyright (C) 2022 Nordix Foundation.
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

FROM onap/integration-java11:8.0.0

LABEL maintainer="Policy Team"
LABEL Description="This image contains alpine, openjdk 11 and policy clamp"
LABEL org.opencontainers.image.title="Policy CLAMP runtime"
LABEL org.opencontainers.image.description="Policy CLAMP runtime image based on Alpine"
LABEL org.opencontainers.image.url="https://github.com/onap/policy-clamp"
LABEL org.opencontainers.image.vendor="ONAP Policy Team"
LABEL org.opencontainers.image.licenses="Apache-2.0"
LABEL org.opencontainers.image.created="${git.build.time}"
LABEL org.opencontainers.image.version="${git.build.version}"
LABEL org.opencontainers.image.revision="${git.commit.id.abbrev}"

ARG http_proxy
ARG https_proxy
ENV HTTP_PROXY=$http_proxy
ENV HTTPS_PROXY=$https_proxy
ENV http_proxy=$HTTP_PROXY
ENV https_proxy=$HTTPS_PROXY

USER root

RUN mkdir -p /opt/policy/clamp && \
    chown -R onap:onap /opt/policy/clamp

VOLUME /opt/policy/clamp/config

COPY --chown=onap:onap onap-policy-clamp-backend/policy-clamp-backend.jar /opt/policy/clamp/policy-clamp-backend.jar
RUN set -x && \
    apk add --update libintl && \
    apk add --virtual build_deps gettext &&  \
    cp /usr/bin/envsubst /usr/local/bin/envsubst && \
    apk del build_deps

USER onap
WORKDIR /opt/policy/clamp/
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-XX:MinRAMPercentage=50.0", "-XX:MaxRAMPercentage=75.0","-jar" ,"./policy-clamp-backend.jar"]

