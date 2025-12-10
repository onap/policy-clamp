#!/bin/sh
# ============LICENSE_START====================================================
# Copyright (C) 2023-2025 Nordix Foundation.
# =============================================================================
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
usage() {
    echo args: [-t timeout] [-c command] hostname1 port1 hostname2 port2 ... >&2
    exit 1
}
tmout=300
cmd=
while getopts c:t: opt
do
    case "$opt" in
        c)
            cmd="$OPTARG"
            ;;
        t)
            tmout="$OPTARG"
            ;;
        *)
            usage
            ;;
    esac
done
nargs=$((OPTIND-1))
shift "$nargs"
even_args=$(($#%2))
if [ $# -lt 2 ] || [ "$even_args" -ne 0 ]
then
    usage
fi
while [ $# -ge 2 ]
do
    export host="$1"
    export port="$2"
    shift
    shift
    echo "Checking if REST port $port is open on $host ..."
    while [ "$tmout" -gt 0 ]
    do
        if command -v docker > /dev/null 2>&1
        then
            docker ps --format "table {{ .Image }}\t{{ .Names }}\t{{ .Status }}"
        fi
        curl "http://$host:$port" > /dev/null 2>&1
        rc=$?
        if [ $rc -eq 0 ]
        then
            break
        else
            tmout=$((tmout-5))
            sleep 5
        fi
    done
    if [ $rc -ne 0 ]; then
        echo "REST port $port cannot be detected on host $host"
        exit $rc
    fi
done
$cmd
exit 0

