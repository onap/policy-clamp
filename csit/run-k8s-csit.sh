#!/bin/bash
#
# ============LICENSE_START====================================================
#  Copyright (C) 2022-2025 OpenInfra Foundation Europe.
#  Modifications Copyright © 2024 Deutsche Telekom
# =============================================================================
#
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
# ============LICENSE_END======================================================

# This script spins up kubernetes cluster in Microk8s for deploying policy helm charts.
# Runs CSITs in kubernetes.

WORKSPACE=$(git rev-parse --show-toplevel)

function print_usage() {
    echo "Usage: $0 [OPTIONS] OPERATION PROJECT"
    echo ""
    echo "OPTIONS:"
    echo "  -c, --cluster-only    Install cluster only, without running robot tests"
    echo "  -l, --local-image     Use local Docker image"
    echo "  -h, --help            Display this help message"
    echo ""
    echo "OPERATION:"
    echo "  install               Install the cluster and optionally run robot tests"
    echo "  uninstall             Uninstall the policy deployment"
    echo "  clean                 Teardown the cluster"
    echo ""
    echo "PROJECT:"
    echo "  Specify the project name (e.g., clamp, api, pap, etc.)"
}

CLUSTER_ONLY=false
LOCAL_IMAGE=false

# Parse command-line options
while [[ $# -gt 0 ]]; do
    case $1 in
        -c|--cluster-only)
            CLUSTER_ONLY=true
            shift
            ;;
        -l|--local-image)
            LOCAL_IMAGE=true
            shift
            ;;
        -h|--help)
            print_usage
            exit 0
            ;;
        *)
            break
            ;;
    esac
done

# Check for required arguments
if [ $# -lt 2 ]; then
    echo "Error: Insufficient arguments"
    print_usage
    exit 1
fi

OPERATION=$1
PROJECT=$2

# Set local image flag
if [ "$LOCAL_IMAGE" = true ]; then
    LOCAL_IMAGE_ARG="true"
else
    LOCAL_IMAGE_ARG="false"
fi

# Execute the appropriate script based on the operation
case $OPERATION in
    install)
        "${WORKSPACE}"/csit/resources/scripts/cluster_setup.sh install "$PROJECT" $LOCAL_IMAGE_ARG
        if [ "$CLUSTER_ONLY" = false ]; then
            "${WORKSPACE}"/csit/resources/scripts/robot_setup.sh "$PROJECT"
        fi
        ;;
    uninstall)
        "${WORKSPACE}"/csit/resources/scripts/cluster_setup.sh uninstall
        ;;
    clean)
        "${WORKSPACE}"/csit/resources/scripts/cluster_setup.sh clean
        ;;
    *)
        echo "Error: Invalid operation"
        print_usage
        exit 1
        ;;
esac
