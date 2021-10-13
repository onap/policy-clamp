#!/bin/bash

#============LICENSE_START=======================================================
#Copyright (C) 2021 Nordix Foundation.
#================================================================================
#Licensed under the Apache License, Version 2.0 (the "License");
#you may not use this file except in compliance with the License.
#You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
#Unless required by applicable law or agreed to in writing, software
#distributed under the License is distributed on an "AS IS" BASIS,
#WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#See the License for the specific language governing permissions and
#limitations under the License.
#============LICENSE_END=========================================================

LOGFILE=$1
if [[ ! -f $LOGFILE ]]; then
  echo "The file '$LOGFILE' in not provided."
  echo "Please provide log file to process."
  exit 1
fi

echo "File being processed: " $LOGFILE
RES=$(awk -F "," 'NR>1 { total += $15 } END { print total/NR }' $LOGFILE)
echo "Average Latency: " $RES
LC=$(awk 'END{print NR}' $LOGFILE)
echo "Total Requests:" $LC
echo "Requests/sec:" $((LC/5))

