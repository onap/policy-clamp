#!/bin/bash -x
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
KIBANA_CONF_FILE="/usr/share/kibana/config/kibana.yml"
SAVED_OBJECTS_ROOT="/saved-objects/"
RESTORE_CMD="/usr/local/bin/restore.py -H http://127.0.0.1:5601/ -f"
BACKUP_BIN="/usr/local/bin/backup.py"
KIBANA_START_CMD="/usr/local/bin/kibana-docker"
LOG_FILE="/tmp/load.kibana.log"
KIBANA_LOAD_CMD="/usr/local/bin/kibana-docker -H 127.0.0.1 -l $LOG_FILE"
TIMEOUT=60
WAIT_TIME=2

if [ -n "$(ls -A ${SAVED_OBJECTS_PATH})" ];
then
    echo "---- Saved objects found, restoring files."

    $KIBANA_LOAD_CMD &
    KIB_PID=$!

    # Wait for log file to be avaiable
    LOG_TIMEOUT=60
    while [ ! -f $LOG_FILE ] && [ "$LOG_TIMEOUT" -gt "0" ];
    do
        echo "Waiting for $LOG_FILE to be available..."
        sleep $WAIT_TIME
        let LOG_TIMEOUT=$LOG_TIMEOUT-$WAIT_TIME
    done

    tail -f $LOG_FILE &
    LOG_PID=$!

    # Wait for kibana to be listening
    while [ -z "$(grep "Server running at" $LOG_FILE)" ] && [ "$TIMEOUT" -gt "0" ];
    do
        echo "Waiting for kibana to start..."
        sleep $WAIT_TIME
        let TIMEOUT=$TIMEOUT-$WAIT_TIME
    done
    sleep 1

    # restore files
    for saved_objects_path in $SAVED_OBJECTS_ROOT/*
    do
        echo "Restoring content of $saved_objects_path"
        $RESTORE_CMD -C $saved_objects_path
        sleep 1
    done

    # cleanup
    kill $KIB_PID
    kill $LOG_PID
else
    echo "---- No saved object found"
    ls -A ${SAVED_OBJECTS_PATH}
fi

echo "---- Starting kibana"

$KIBANA_START_CMD

