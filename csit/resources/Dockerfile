FROM nexus3.onap.org:10001/library/python:3.13-slim-bullseye
ARG CSIT_SCRIPT=${CSIT_SCRIPT}
ARG ROBOT_FILE=${ROBOT_FILE}
ENV ROBOT_WORKSPACE=/opt/robotworkspace ROBOT_FILE=$ROBOT_FILE TEST_ENV=$TEST_ENV PROJECT=$PROJECT
RUN apt-get update && \
    apt-get install -y gcc postgresql-client \
    && apt-get clean; \
    python3 -m pip -qq install --upgrade pip; \
    python3 -m pip -qq install --upgrade confluent-kafka; \
    python3 -m pip -qq install --upgrade --extra-index-url="https://nexus3.onap.org/repository/PyPi.staging/simple" 'robotframework-onap==0.6.0.*' --pre; \
    python3 -m pip freeze; \
    mkdir -p ${ROBOT_WORKSPACE}
COPY scripts/run-test.sh tests/ ${ROBOT_WORKSPACE}/
WORKDIR ${ROBOT_WORKSPACE}
CMD ["sh", "-c", "./run-test.sh" ]
