#!/bin/bash
# ============LICENSE_START=======================================================
#  Copyright (C) 2025 OpenInfra Foundation Europe.
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

WORKSPACE=$(git rev-parse --show-toplevel)
export WORKSPACE

export GERRIT_BRANCH=$(awk -F= '$1 == "defaultbranch" { print $2 }' "${WORKSPACE}"/.gitreview)

# Source the shared config script
source "$(dirname "$0")/config_setup.sh"

KAFKA_DIR=${WORKSPACE}/helm/cp-kafka
SET_VALUES=""

ZK_CONTAINER="zookeeper-deployment"
KAFKA_CONTAINER="kafka-deployment"

function spin_microk8s_cluster() {
    echo "Verify if Microk8s cluster is running.."
    microk8s version
    exitcode="${?}"

    if [ "$exitcode" -ne 0 ]; then
        echo "Microk8s cluster not available, Spinning up the cluster.."
        sudo snap install microk8s --classic --channel=1.30/stable

        if [ "${?}" -ne 0 ]; then
            echo "Failed to install kubernetes cluster. Aborting.."
            return 1
        fi
        echo "Microk8s cluster installed successfully"
        sudo usermod -a -G microk8s $USER
        echo "Enabling DNS and Storage plugins"
        sudo microk8s.enable dns hostpath-storage
        echo "Creating configuration file for Microk8s"
        sudo mkdir -p $HOME/.kube
        sudo chown -R $USER:$USER $HOME/.kube
        sudo microk8s kubectl config view --raw >$HOME/.kube/config
        sudo chmod 600 $HOME/.kube/config
        echo "K8s installation completed"
        echo "----------------------------------------"
    else
        echo "K8s cluster is already running"
        echo "----------------------------------------"
    fi

    echo "Verify if kubectl is running.."
    kubectl version
    exitcode="${?}"

    if [ "$exitcode" -ne 0 ]; then
        echo "Kubectl not available, Installing.."
        sudo snap install kubectl --classic --channel=1.30/stable

        if [ "${?}" -ne 0 ]; then
            echo "Failed to install Kubectl. Aborting.."
            return 1
        fi
        echo "Kubectl installation completed"
        echo "----------------------------------------"
    else
        echo "Kubectl is already running"
        echo "----------------------------------------"
        return 0
    fi

    echo "Verify if helm is running.."
    helm version
    exitcode="${?}"

    if [ "$exitcode" -ne 0 ]; then
        echo "Helm not available, Installing.."
        sudo snap install helm --classic --channel=3.7

        if [ "${?}" -ne 0 ]; then
            echo "Failed to install Helm client. Aborting.."
            return 1
        fi
        echo "Helm installation completed"
        echo "----------------------------------------"
    else
        echo "Helm is already running"
        echo "----------------------------------------"
        return 0
    fi
}

function install_kafka() {
  echo "Installing Confluent kafka"
  kubectl apply -f $KAFKA_DIR/zookeeper.yaml
  kubectl apply -f $KAFKA_DIR/kafka.yaml
  echo "----------------------------------------"
}

function uninstall_policy() {
    echo "Removing the policy helm deployment"
    helm uninstall csit-policy
    helm uninstall prometheus
    helm uninstall csit-robot
    kubectl delete deploy $ZK_CONTAINER $KAFKA_CONTAINER
    rm -rf ${WORKSPACE}/helm/policy/Chart.lock

    if [ "$PROJECT" == "clamp" ] || [ "$PROJECT" == "policy-clamp" ]; then
      helm uninstall policy-chartmuseum
      helm repo remove chartmuseum-git policy-chartmuseum
    fi

    kubectl delete pvc --all
    echo "Policy deployment deleted"
    echo "Clean up docker"
    docker image prune -f
}

function teardown_cluster() {
    echo "Removing k8s cluster and k8s configuration file"
    sudo snap remove microk8s;rm -rf $HOME/.kube/config
    sudo snap remove helm;
    sudo snap remove kubectl;
    echo "MicroK8s Cluster removed"
}

function install_chartmuseum () {
    echo "---------------------------------------------"
    echo "Installing Chartmuseum helm repository..."
    helm repo add chartmuseum-git https://chartmuseum.github.io/charts
    helm repo update
    helm install policy-chartmuseum chartmuseum-git/chartmuseum --set env.open.DISABLE_API=false --set service.type=NodePort --set service.nodePort=30208
    helm plugin install https://github.com/chartmuseum/helm-push
    echo "---------------------------------------------"
}

function get_pod_name() {
  pods=$(kubectl get pods --no-headers -o custom-columns=':metadata.name' | grep $1)
  read -rd '' -a pod_array <<< "$pods"
  echo "${pod_array[@]}"
}

function wait_for_pods_running() {
  local namespace="$1"
  shift
  local timeout_seconds="$1"
  shift

  IFS=',' read -ra pod_names <<< "$@"
  shift

  local pending_pods=("${pod_names[@]}")
  local start_time
  start_time=$(date +%s)

  while [ ${#pending_pods[@]} -gt 0 ]; do
    local current_time
    current_time=$(date +%s)
    local elapsed_time
    elapsed_time=$((current_time - start_time))

    if [ "$elapsed_time" -ge "$timeout_seconds" ]; then
      echo "Timed out waiting for the pods to reach 'Running' state."
      echo "Printing the current status of the deployment before exiting.."
      kubectl get po;
      kubectl describe pods;
      echo "------------------------------------------------------------"
      for pod in "${pending_pods[@]}"; do
        echo "Logs of the pod $pod"
        kubectl logs $pod
        echo "---------------------------------------------------------"
      done
      exit 1
    fi

    local newly_running_pods=()

    for pod_name_prefix in "${pending_pods[@]}"; do
      local pod_names=$(get_pod_name "$pod_name_prefix")
      IFS=' ' read -r -a pod_array <<< "$pod_names"
      if [ "${#pod_array[@]}" -eq 0 ]; then
             echo "*** Error: No pods found for the deployment $pod_name_prefix . Exiting ***"
             return 1
      fi
      for pod in "${pod_array[@]}"; do
         local pod_status
         local pod_ready
         pod_status=$(kubectl get pod "$pod" -n "$namespace" --no-headers -o custom-columns=STATUS:.status.phase 2>/dev/null)
         pod_ready=$(kubectl get pod "$pod" -o jsonpath='{.status.containerStatuses[*].ready}')

         if [ "$pod_status" == "Running" ] && { [ "$pod_ready" == "true" ] || [ "$pod_ready" == "true true" ]; }; then
           echo "Pod '$pod' in namespace '$namespace' is now in 'Running' state and 'Readiness' is true"
         else
           newly_running_pods+=("$pod")
           echo "Waiting for pod '$pod' in namespace '$namespace' to reach 'Running' and 'Ready' state..."
         fi
      done
    done

    pending_pods=("${newly_running_pods[@]}")

    sleep 5
  done

  echo "All specified pods are in the 'Running and Ready' state. Exiting the function."
}

OPERATION="$1"
PROJECT="$2"
LOCALIMAGE="${3:-false}"

if [ $OPERATION == "install" ]; then
    spin_microk8s_cluster
    if [ "${?}" -eq 0 ]; then
        export KAFKA_CONTAINERS=($KAFKA_CONTAINER,$ZK_CONTAINER)
        install_kafka
        wait_for_pods_running default 300 $KAFKA_CONTAINERS
        set_project_config "$PROJECT"
        echo "Installing policy helm charts in the default namespace"
        source ${WORKSPACE}/compose/get-k8s-versions.sh
        if [ $LOCALIMAGE == "true" ]; then
            echo "loading local image"
            source ${WORKSPACE}/compose/get-versions.sh
            ${WORKSPACE}/compose/loaddockerimage.sh
        fi
        cd ${WORKSPACE}/helm || exit
        helm dependency build policy
        helm install csit-policy policy ${SET_VALUES}
        helm install prometheus prometheus
        wait_for_pods_running default 900 ${READINESS_CONTAINERS[@]}
        echo "Policy chart installation completed"
        echo "-------------------------------------------"
    fi
elif [ $OPERATION == "uninstall" ]; then
    uninstall_policy
elif [ $OPERATION == "clean" ]; then
    teardown_cluster
else
    echo "Invalid arguments provided. Usage: $0 [options..] {install {project_name} | uninstall | clean} {uselocalimage = true/false}"
fi
