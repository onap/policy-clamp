#-------------------------------------------------------------------------------
# ============LICENSE_START=======================================================
#  Copyright (C) 2021-2023 Nordix Foundation.
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

FROM busybox AS tarball
RUN mkdir /packages /extracted
COPY /maven/lib/http-participant.tar.gz /packages/
RUN tar xvzf /packages/http-participant.tar.gz --directory /extracted/

FROM onap/policy-jre-alpine:3.0.1-SNAPSHOT

LABEL maintainer="Policy Team"
LABEL org.opencontainers.image.title="Policy CLAMP ACM HTTP Participant"
LABEL org.opencontainers.image.description="Policy CLAMP ACM HTTP Participant image based on Alpine"
LABEL org.opencontainers.image.url="https://github.com/onap/policy-clamp"
LABEL org.opencontainers.image.vendor="ONAP Policy Team"
LABEL org.opencontainers.image.licenses="Apache-2.0"
LABEL org.opencontainers.image.created="${git.build.time}"
LABEL org.opencontainers.image.version="${git.build.version}"
LABEL org.opencontainers.image.revision="${git.commit.id.abbrev}"

ARG POLICY_LOGS=/var/log/onap/policy/http-participant

ENV POLICY_LOGS=$POLICY_LOGS
ENV POLICY_HOME=$POLICY_HOME/clamp

USER root
RUN mkdir -p $POLICY_LOGS $POLICY_HOME && \
    chown -R policy:policy $POLICY_HOME $POLICY_LOGS

COPY --chown=policy:policy --from=tarball /extracted $POLICY_HOME

WORKDIR $POLICY_HOME
COPY --chown=policy:policy http-participant.sh bin/
COPY --chown=policy:policy /maven/policy-clamp-participant-impl-http.jar /app/app.jar

RUN chmod 755 bin/*.sh

EXPOSE 8084

USER policy
WORKDIR $POLICY_HOME/bin
ENTRYPOINT [ "./http-participant.sh" ]
