#-------------------------------------------------------------------------------
# ============LICENSE_START=======================================================
#  Copyright (C) 2021-2022 Nordix Foundation.
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
COPY /maven/lib/kubernetes-participant.tar.gz /packages/
RUN tar xvzf /packages/kubernetes-participant.tar.gz --directory /extracted/

FROM onap/policy-jre-alpine:2.6.0

LABEL maintainer="Policy Team"
LABEL org.opencontainers.image.title="Policy CLAMP ACM K8S Participant"
LABEL org.opencontainers.image.description="Policy CLAMP ACM K8S Participant image based on Alpine"
LABEL org.opencontainers.image.url="https://github.com/onap/policy-clamp"
LABEL org.opencontainers.image.vendor="ONAP Policy Team"
LABEL org.opencontainers.image.licenses="Apache-2.0"
LABEL org.opencontainers.image.created="${git.build.time}"
LABEL org.opencontainers.image.version="${git.build.version}"
LABEL org.opencontainers.image.revision="${git.commit.id.abbrev}"

ARG POLICY_LOGS=/var/log/onap/policy/k8s-participant

ENV POLICY_LOGS=$POLICY_LOGS
ENV POLICY_HOME=$POLICY_HOME/clamp

RUN mkdir -p $POLICY_HOME $POLICY_LOGS && \
    chown -R policy:policy $POLICY_HOME $POLICY_LOGS

COPY --chown=policy:policy --from=tarball /extracted $POLICY_HOME

WORKDIR $POLICY_HOME
COPY --chown=policy:policy kubernetes-participant.sh bin/
COPY --chown=policy:policy /maven/policy-clamp-participant-impl-kubernetes.jar /app/app.jar

RUN chmod 755 bin/*.sh && \
    apk update && \
    apk add wget && \
    wget -nv https://get.helm.sh/helm-v3.5.2-linux-amd64.tar.gz && \
    tar xvf helm-v3.5.2-linux-amd64.tar.gz && \
    mv linux-amd64/helm /usr/local/bin && \
    rm -rf linux-amd64 && \
    rm helm-v3.5.2-linux-amd64.tar.gz && \
    wget -nv https://storage.googleapis.com/kubernetes-release/release/v1.21.1/bin/linux/amd64/kubectl && \
    chmod +x kubectl && \
    mv kubectl /usr/local/bin/kubectl

EXPOSE 8083

USER policy
WORKDIR $POLICY_HOME/bin
ENTRYPOINT [ "./kubernetes-participant.sh" ]


