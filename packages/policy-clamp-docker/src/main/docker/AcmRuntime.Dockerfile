#-------------------------------------------------------------------------------
# ============LICENSE_START=======================================================
#  Copyright (C) 2021-2025 OpenInfra Foundation Europe. All rights reserved.
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
COPY /maven/lib/policy-clamp-runtime-acm.tar.gz /packages/
RUN tar xvzf /packages/policy-clamp-runtime-acm.tar.gz --directory /extracted/

FROM onap/policy-jre-alpine:4.2.1-SNAPSHOT

LABEL maintainer="Policy Team"
LABEL org.opencontainers.image.title="Policy CLAMP ACM runtime"
LABEL org.opencontainers.image.description="Policy CLAMP ACM runtime image based on Alpine"
LABEL org.opencontainers.image.url="https://github.com/onap/policy-clamp"
LABEL org.opencontainers.image.vendor="ONAP Policy Team"
LABEL org.opencontainers.image.licenses="Apache-2.0"
LABEL org.opencontainers.image.created="${git.build.time}"
LABEL org.opencontainers.image.version="${git.build.version}"
LABEL org.opencontainers.image.revision="${git.commit.id.abbrev}"

ARG POLICY_LOGS=/var/log/onap/policy/policy-clamp-runtime-acm

ENV POLICY_LOGS=$POLICY_LOGS
ENV POLICY_HOME=$POLICY_HOME/clamp

USER root
RUN mkdir -p $POLICY_HOME $POLICY_LOGS && \
    chown -R policy:policy $POLICY_HOME $POLICY_LOGS

COPY --chown=policy:policy --from=tarball /extracted $POLICY_HOME

WORKDIR $POLICY_HOME
COPY --chown=policy:policy acm-runtime.sh bin/
COPY --chown=policy:policy /maven/policy-clamp-runtime-acm.jar /app/app.jar

RUN pip uninstall -y setuptools || true

RUN python3 -m pip uninstall -y pip || true && \
    rm -rf /usr/bin/pip* /usr/local/bin/pip*

RUN chmod 755 bin/*.sh

EXPOSE 6969

USER policy
WORKDIR $POLICY_HOME/bin
ENTRYPOINT [ "./acm-runtime.sh" ]
