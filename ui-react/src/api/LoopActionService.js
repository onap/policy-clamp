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

export default class LoopActionService{

	static performAction(cl_name, uiAction) {
		console.info("LoopActionService perform action: " + uiAction + " closedloopName=" + cl_name);
		const svcAction = uiAction.toLowerCase();
 		return fetch(window.location.pathname + "restservices/clds/v2/loop/" + svcAction + "/" + cl_name, {
 				method: 'PUT',
 				credentials: 'same-origin'
 			})
 		.then(function (response) {
 			if (response.ok) {
 				return response.json();
 			} else {
 				return Promise.reject("Perform action failed with code:" + response.status);
 			}
 		})
 		.then(function (data) {
			console.info("Action Successful: " + uiAction);
 			return data;
 		})
 		.catch(function(error) {
 			console.info("Action Failure: " + uiAction);
 			return Promise.reject(error);
 		});
 	}


	static refreshStatus(cl_name) {
		console.info("Refresh the status for closedloopName=" + cl_name);

		return fetch(window.location.pathname + "restservices/clds/v2/loop/getstatus/" + cl_name, {
			method: 'GET',
			credentials: 'same-origin'
		})
		.then(function (response) {
			if (response.ok) {
				return response.json();
			} else {
				return Promise.reject("Refresh status failed with code:" + response.status);
			}
		})
		.then(function (data) {
			console.info ("Refresh status Successful");
			return data;
		})
		.catch(function(error) {
			console.info ("Refresh status failed:", error);
			return Promise.reject(error);
		});
 	}
}
