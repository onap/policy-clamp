#!/bin/bash
###
# ============LICENSE_START=======================================================
# ONAP CLAMP
# ================================================================================
# Copyright (C) 2020 AT&T Intellectual Property. All rights
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

baseDir=$(git rev-parse --show-toplevel)

if [[ ! -d $baseDir ]]
then
	echo "[ERROR] failed to determine git base directory"
	exit 1
fi

tmpSrcFileList=/tmp/upldateLibIndex.$$.list
reactUiBaseDir="${baseDir}/ui-react"
reactLibIndexFile="ui-react-lib/libIndex.js"
exclusionList="ui-react-lib/libExportExclusions.dat"


if [[ ! -d "$reactUiBaseDir" ]]
then
	echo "[ERROR] reacUiBaseDir=$reacUiBaseDir is not accessible"
	exit 1
fi

if [[ ! -d "$baseDir/$reactLibBaseDir" ]]
then
	echo "[ERROR] reactLibBaseDir=$baseDir/$reactLibBaseDir is not accessible"
	exit 1
fi

if [[ ! -r "$baseDir/$reactLibIndexFile" ]]
then
	echo "[ERROR] file reactLibIndexFile=$baseDir/$reactLibIndexFile is not accessible"
	exit 1
fi


if ! cd $reactUiBaseDir
then
	echo "[ERROR] could not cd to reactUiBaseDir=$reactUiBaseDir"
	exit 1
fi

find ./src -name \*.js | egrep -v "__snapshot|\.test\." 2>/dev/null | sed 's/.js$//' > $tmpSrcFileList

if [[ ! -s $tmpSrcFileList ]]
then
	echo "[ERROR] no source files found in reactUiBaseDir=$reactUiBaseDir"
	rm -f $tmpSrcFileList
	exit 1
fi

export nErrors=0

# Verify that any .js file found within ui-react/src other than test related files
# is also referenced in ui-react-lib/libIndex.js

for srcFileName in `<$tmpSrcFileList`
do
	if [[ -r "$baseDir/$exclusionList" ]]
	then
		if grep $srcFileName $baseDir/$exclusionList >/dev/null 2>&1
		then
			continue
		fi
	fi

	if ! grep $srcFileName "$baseDir/$reactLibIndexFile" > /dev/null 2>&1
	then
		echo "[ERROR] file=${srcFileName}.js is not declared in $reactLibIndexFile"
		echo "[ERROR] and not found in exclsionList=${exclusionList}."
		echo "[ERROR] Please either add it to $reactLibIndexFile"
		echo "[ERROR] or to the exclusion list in ${exclusionList}."
		echo ""
		(( nErrors++ ))
	fi
done

# Verify for each entry in ui-react-lib/libIndex.js, that the referenced source file exists
# in ui-react/src; if not, developer probably forgot to remove it from libIndex.js.

egrep '^export ' $baseDir/$reactLibIndexFile |\
sed -e "s+.*\./src+./src+" -e "s+'.*+.js+" >  $tmpSrcFileList

for srcFileName in `<$tmpSrcFileList`
do
	if [[ ! -r "$srcFileName" ]]
	then
		echo "[ERROR] source file=$srcFileName in libIndex.js is not accessible"
		(( nErrors++ ))
	fi
done

rm -f $tmpSrcFileList

if (( nErrors == 0 ))
then
	echo "[INFO] $reactLibIndexFile passes sanity check"
	exit 0
fi

exit $nErrors
