/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
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

export default class LoopService {
	static getLoopNames() {
		const url = '/restservices/clds/v2/loop/getAllNames';
		const options = {
			method: 'GET'
		};
		return fetch(url,options)
		.then(function (response) {
			if (response.ok) {
				console.log("GetLoopNames response received: ", response.status);
				return response.json();
			} else {
				let errorMsg = "GetLoopNames failed with status code: " + response.status;
				console.error(errorMsg);
				return Promise.reject(errorMsg);
			}
		})
		.then(function (data) {
			return data;
		})
		.catch(function(error) {
			console.error("GetLoopNames error received",error);
			return Promise.reject(error);
		});
	}

	static getLoop(loopName) {
		const url = '/restservices/clds/v2/loop/' + loopName;
		const options = {
			method: 'GET',
			headers: {
				"Content-Type": "application/json"
			}
		};
		return fetch(url,options)
		.then(function (response) {
			if (response.ok) {
				console.log("GetLoop response received: ", response.status);
				return response.json();
			} else {
				let errorMsg = "GetLoop failed with status code: " + response.status;
				console.error(errorMsg);
				return Promise.reject(errorMsg);
			}
		})
		.then(function (data) {
			return data;
		})
		.catch(function(error) {
			console.error("GetLoop error received",error);
			return Promise.reject(error);
		});
	}
}
