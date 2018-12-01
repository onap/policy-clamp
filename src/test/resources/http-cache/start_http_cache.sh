#!/bin/bash
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

echo "Starting HTTP CACHE python script"
for i in "$@"
do
case $i in
     --python_proxyaddress=*)
      python_proxyaddress="--proxyaddress ${i#*=}"
      echo "- Using python_proxyaddress and set it to: $python_proxyaddress"
      shift # past argument=value
      ;;
     --http_proxyaddress=*)
      export http_proxy="${i#*=}"
      export https_proxy="${i#*=}"
      echo "- Defining http_proxy/https_proxy env variables to: $http_proxy"
      shift # past argument=value
      ;;
     -?|--help|-help)
      echo "Usage: $(basename $0) [--http_proxyaddress=<http://proxy_address:port>] [--python_proxyaddress=<python_simulator_address:port>]"
      echo "--http_proxyaddress Set the http_proxy/https_proxy in the script before running python"
      echo "--python_proxyaddress <python_simulator_address:port>, like localhost:8080 and will be set as --proxyaddress, this is the adress returned by DCAE simulator response"
      exit 2
      ;;
esac
done

echo 'Installing requests packages for Python'
pip install requests
echo 'Executing the Http proxy in Cache mode only'
python -u third_party_proxy.py --port 8080 --root /usr/src/http-cache-app/data-cache $python_proxyaddress
