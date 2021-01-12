#!/bin/bash
# ============LICENSE_START=======================================================
#  Copyright (c) 2021 Nordix Foundation.
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

# the directory of the script
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
echo ${DIR}

if [ "$#" -lt 2 ]; then
    echo "CLAMP and MariaDB IPs should be passed as two parameters. CLAMP IP goes first."
    exit 1
else
    CLAMP=$1
    echo "CLAMP IP: ${CLAMP}"
    MARIADB=$2
    echo "MariaDB IP: ${MARIADB}"
fi

GIT_TOP=$(git rev-parse --show-toplevel)
POLICY_CLAMP_VERSION_EXTRACT=$(xmllint --xpath '/*[local-name()="project"]/*[local-name()="version"]/text()' "${GIT_TOP}"/pom.xml)
CLAMP_IMAGE=policy-clamp:${POLICY_CLAMP_VERSION_EXTRACT:0:3}-SNAPSHOT-latest

docker run \
	-p 9090:9090 \
	-p 6969:6969 \
	-e "CLAMP_HOST=${CLAMP}" \
	-v ${DIR}/config/clamp/bin/policy-clamp.sh:/opt/app/policy/clamp/bin/policy-clamp.sh \
	-v ${DIR}/config/clamp/etc/defaultConfig.json:/opt/app/policy/clamp/etc/defaultConfig.json \
	--add-host mariadb:${MARIADB} \
	--name policy-clamp \
	-d  \
	nexus3.onap.org:10001/onap/${CLAMP_IMAGE}
