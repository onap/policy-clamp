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

if [ $# -eq 1 ]
	then
		echo 'input parameter is set (proxy http)';
		export http_proxy=$1
		export https_proxy=$1
	else
		echo 'input parameter is not set (proxy http)';
fi

echo 'Installing requests packages for Python'
pip install requests
echo 'Executing the Http proxy in Cache mode only'
python third_party_proxy.py --port 8080 --root /usr/src/http-cache-app/data-cache
