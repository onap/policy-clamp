#!/bin/sh

###
# ============LICENSE_START=======================================================
# ONAP CLAMP
# ================================================================================
# Copyright (C) 2019 AT&T Intellectual Property. All rights
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

docker-compose -f ../docker/clamp/docker-compose.yml up -d db

if [ "$1" = "test" ]; then
    while ! (docker logs clamp_db_1  2>&1 | grep "socket: '/var/run/mysqld/mysqld.sock'  port: 3306  mariadb.org binary distribution" > /dev/null);
    do   
      echo "Waiting Mysql to be up with CLDSDB4 db loaded before loading the TEST DATA ..."
      sleep 3
    done
    docker exec -it clamp_db_1 /docker-entrypoint-initdb.d/dump/load-fake-data.sh
fi; 
