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
const loopActionService = {
	submit
};


function submit(uiAction) {
	const cl_name = "";
	console.log("clActionServices perform action: " + uiAction + " closedloopName="
		+ cl_name);
	const svcAction = uiAction.toLowerCase();
	const svcUrl = "/restservices/clds/v2/loop/" + svcAction + "/" + cl_name;

	let options = {
		method: 'GET'
	};
	return sendRequest(svcUrl, svcAction, options);
}

function sendRequest(svcUrl, svcAction) {
	fetch(svcUrl, options)
		.then(
			response => {
				alertService.alertMessage("Action Successful: " + svcAction, 1)
			}).error(error => {
				alertService.alertMessage("Action Failure: " + svcAction, 2);
				return Promise.reject(error);
			});

	return response.json();
};

export default loopActionService;