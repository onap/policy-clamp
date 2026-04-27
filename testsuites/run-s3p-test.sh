#!/bin/bash
# ============LICENSE_START=======================================================
#  Copyright (C) 2023-2026 OpenInfra Foundation Europe. All rights reserved.
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

#===MAIN===#
if [ -z "${WORKSPACE}" ]; then
    export WORKSPACE=$(git rev-parse --show-toplevel)
fi

export PROJECT="clamp"
export TESTDIR=${WORKSPACE}/testsuites

function run_tests() {
    bash $WORKSPACE/testsuites/script/start-sp3.sh
}

function clean() {
    bash $WORKSPACE/testsuites/script/clean-sp3.sh
}

function run_tests_k8s() {
    mkdir -p automate-s3p-test
    cd automate-s3p-test || exit 1
    git clone "https://gerrit.onap.org/r/policy/docker"
    cd docker/csit || exit 1

    bash run-s3p-tests.sh test $ACM_TEST_FILE $PROJECT
}

function clean_k8s() {
   cd $TESTDIR/automate-s3p-test/docker/csit
    bash run-s3p-tests.sh clean
}

echo "================================="
echo "Triggering S3P test for: $PROJECT"
echo "================================="

case $1 in
    performance)
        export ACM_TEST_FILE=$TESTDIR/performance/src/main/resources/testplans/performance.jmx
        run_tests
        ;;
    stability)
        export ACM_TEST_FILE=$TESTDIR/stability/src/main/resources/testplans/stability.jmx
        run_tests
        ;;
    clean)
        clean
        ;;
    performance-k8s)
        export ACM_TEST_FILE=$TESTDIR/performance/src/main/resources/testplans/performance-k8s.jmx
        run_tests_k8s
        ;;
    stability-k8s)
        export ACM_TEST_FILE=$TESTDIR/stability/src/main/resources/testplans/stability-k8s.jmx
        run_tests_k8s
        ;;
    clean-k8s)
        clean_k8s
        ;;
    *)
        echo "Invalid arguments provided. Usage: $0 {performance | stability | clean | performance-k8s | stability-k8s | clean-k8s}"
        exit 1
        ;;
esac
