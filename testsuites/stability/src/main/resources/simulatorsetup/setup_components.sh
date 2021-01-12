#!/bin/bash
# ============LICENSE_START=======================================================
#  Copyright (c) 2019 - 2020 Nordix Foundation.
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

# get the PAP_HOST from the command line
read -p 'PAP_HOST : ' PAP_HOST

# setting hostname to environment variable
echo "export API_HOST=`hostname -I | awk '{print $1}'`" >> ~/.bashrc
echo "export PAP_HOST=$PAP_HOST" >> ~/.bashrc
source ~/.bashrc

# the temp directory used, within $DIR
# omit the -p parameter to create a temporal directory in the default location
WORK_DIR=`mktemp -d -p "$DIR"`
echo ${WORK_DIR}

cd ${WORK_DIR}

# check if tmp dir was created
if [[ ! "$WORK_DIR" || ! -d "$WORK_DIR" ]]; then
  echo "Could not create temp dir"
  exit 1
fi

# bring down maven
mkdir maven
cd maven
curl -O http://apache.claz.org/maven/maven-3/3.3.9/binaries/apache-maven-3.3.9-bin.tar.gz
tar -xzvf apache-maven-3.3.9-bin.tar.gz
ls -l
export PATH=${PATH}:${WORK_DIR}/maven/apache-maven-3.3.9/bin
${WORK_DIR}/maven/apache-maven-3.3.9/bin/mvn -v
cd ..

git clone http://gerrit.onap.org/r/oparent
git clone --depth 1 https://gerrit.onap.org/r/policy/models -b master

cd models/models-sim/models-sim-dmaap
${WORK_DIR}/maven/apache-maven-3.3.9/bin/mvn clean install -DskipTests  --settings ${WORK_DIR}/oparent/settings.xml
bash ./src/main/package/docker/docker_build.sh

cd ../policy-models-sim-pdp
${WORK_DIR}/maven/apache-maven-3.3.9/bin/mvn clean install -DskipTests  --settings ${WORK_DIR}/oparent/settings.xml
bash ./src/main/package/docker/docker_build.sh

cd ${DIR}
rm -rf ${WORK_DIR}

docker run -p 3306:3306 -v ${DIR}/config/db:/docker-entrypoint-initdb.d --name mariadb  --env-file ${DIR}/config/db/db.conf -d --rm mariadb:10.2.14 --lower-case-table-names=1 --wait_timeout=28800
docker run -p 3904:3904 -d --name message-router --rm dmaap/simulator:latest
sleep 10
docker run --link message-router:message-router -d --name pdp-simulator --rm pdp/simulator:latest
docker run -p 6969:6969 --link mariadb:mariadb --name policy-api -d --rm nexus3.onap.org:10001/onap/policy-api
