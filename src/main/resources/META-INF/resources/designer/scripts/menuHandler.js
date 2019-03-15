/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights
 *                             reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END============================================
 * ===================================================================
 * 
 */


function enableActionMenu(status) {
	var actionMap = '{"DESIGN": ["Submit","Delete"],"RUNNING": ["Stop","UnDeploy"],"SUBMITTED": ["Submit","Delete","Deploy"],"DEPLOYED": ["UnDeploy","Stop"],"UNDEPLOY": ["Deploy","Submit","Restart","Delete"],"STOPPED":["UnDeploy","Restart"]}';
	var actionMapJson = JSON.parse(actionMap);
	var actionArr = actionMapJson[status];
	disableAllActionMenu();
	if (actionArr != null && actionArr.length > 0) {
		for (var i=0; i < actionArr.length; i++) {
		    document.getElementById(actionArr[i]).classList
		    .remove('ThisLink');
		}
	}
}

function disableAllActionMenu() {
	var allActions = ["Submit","Stop","Restart","Delete","Deploy","UnDeploy"];
	for (var i=0; i < allActions.length; i++) {
	    document.getElementById(allActions[i]).classList
	    .add('ThisLink');
	}
}

function enableAllActionMenu() {
	var allActions = ["Submit","Stop","Restart","Delete","Deploy","UnDeploy"];
	for (var i=0; i < allActions.length; i++) {
	    document.getElementById(allActions[i]).classList
	    .remove('ThisLink');
	}
}

function enableDefaultMenu() {
	 document.getElementById('Open CL').classList.remove('ThisLink');
	 document.getElementById('Wiki').classList.remove('ThisLink');
	 document.getElementById('Contact Us').classList.remove('ThisLink');
	    if (readMOnly) {
		    // enable model options
		    document.getElementById('Properties CL').classList
		    .remove('ThisLink');
		    document.getElementById('Close Model').classList
		    .remove('ThisLink');
	    } else {
		    document.getElementById('Properties CL').classList
		    .remove('ThisLink');
		    document.getElementById('Close Model').classList
		    .remove('ThisLink');
		    document.getElementById('Refresh Status').classList
		    .remove('ThisLink');
	    }
}

