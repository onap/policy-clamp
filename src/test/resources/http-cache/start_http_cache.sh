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
# ECOMP is a trademark and service mark of AT&T Intellectual Property.
###

if [ $# -eq 2 ]
	then
		echo 'Setting http_proxy and proxyaddress script parameters';
		export http_proxy=$1
		export https_proxy=$1
		python_proxyaddress=$2
		echo 'http_proxy was set to '$http_proxy
		echo 'python_proxyaddress was set to '$python_proxyaddress
	else
		echo 'Required parameters are not set';
		echo 'Command Format:  start_http_cache.sh <http_proxy_adress> <host_running_test:port>';
		echo '  http_proxy_adress, like http://my.proxy.com:8080 and will be set to http_proxy/https_proxy environment variables';
		echo '  host_running_test, like localhost:8080 and will be set as --proxyaddress, this is the adress returned by DCAE simulator response';
		exit 1
fi

echo 'Installing requests packages for Python'
pip install requests
echo 'Executing the Http proxy in Cache mode only'
python third_party_proxy.py --port 8080 --root /usr/src/http-cache-app/data-cache --proxyaddress $python_proxyaddress
