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
		return fetch(window.location.pathname + 'restservices/clds/v2/loop/getAllNames', { method: 'GET', credentials: 'same-origin' })
			.then(function (response) {
				console.debug("GetLoopNames response received: ", response.status);
				if (response.ok) {
					return response.json();
				} else {
					console.error("GetLoopNames query failed");
					return {};
				}
			})
			.catch(function (error) {
				console.error("GetLoopNames error received", error);
				return {};
			});
	}

	static createLoop(loopName, templateName) {
		return fetch(window.location.pathname + 'restservices/clds/v2/loop/create/' + loopName + '?templateName=' + templateName, {
			method: 'POST',
			headers: {
				"Content-Type": "application/json"
			},
			credentials: 'same-origin'
		})
			.then(function (response) {
				console.debug("CreateLoop response received: ", response.status);
				return response.json();
			})
			.catch(function (error) {
				console.error("CreateLoop error received", error);
				return "";
			});
	}

	static getLoop(loopName) {
		return fetch(window.location.pathname + 'restservices/clds/v2/loop/' + loopName, {
			method: 'GET',
			headers: {
				"Content-Type": "application/json"
			},
			credentials: 'same-origin'
		})
			.then(function (response) {
				console.debug("GetLoop response received: ", response.status);
				if (response.ok) {
					return response.json();
				} else {
					console.error("GetLoop query failed");
					return {};
				}
			})
			.catch(function (error) {
				console.error("GetLoop error received", error);
				return {};
			});
	}

	static setMicroServiceProperties(loopName, jsonData) {
		return fetch(window.location.pathname + 'restservices/clds/v2/loop/updateMicroservicePolicy/' + loopName, {
			method: 'POST',
			credentials: 'same-origin',
			headers: {
				"Content-Type": "application/json"
			},
			body: JSON.stringify(jsonData)
		})
			.then(function (response) {
				console.debug("updateMicroservicePolicy response received: ", response.status);
				if (response.ok) {
					return response.text();
				} else {
					console.error("updateMicroservicePolicy query failed");
					return "";
				}
			})
			.catch(function (error) {
				console.error("updateMicroservicePolicy error received", error);
				return "";
			});
	}
	
	static setOperationalPolicyProperties(loopName, jsonData) {
		return fetch(window.location.pathname + 'restservices/clds/v2/loop/updateOperationalPolicies/' + loopName, {
			method: 'POST',
			credentials: 'same-origin',
			headers: {
				"Content-Type": "application/json"
			},
			body: JSON.stringify(jsonData)
		})
			.then(function (response) {
				console.debug("updateOperationalPolicies response received: ", response.status);
				if (response.ok) {
					return response.text();
				} else {
					console.error("updateOperationalPolicies query failed");
					return "";
				}
			})
			.catch(function (error) {
				console.error("updateOperationalPolicies error received", error);
				return "";
			});
	}

	static updateGlobalProperties(loopName, jsonData) {
		return fetch(window.location.pathname + 'restservices/clds/v2/loop/updateGlobalProperties/' + loopName, {
			method: 'POST',
			credentials: 'same-origin',
			headers: {
				"Content-Type": "application/json"
			},
			body: JSON.stringify(jsonData)
		})
			.then(function (response) {
				console.debug("updateGlobalProperties response received: ", response.status);
				if (response.ok) {
					return response.text();
				} else {
					console.error("updateGlobalProperties query failed");
					return "";
				}
			})
			.catch(function (error) {
				console.error("updateGlobalProperties error received", error);
				return "";
			});
	}

	static refreshOperationalPolicyJson(loopName,operationalPolicyName) {
		return fetch(window.location.pathname + 'restservices/clds/v2/loop/refreshOperationalPolicyJsonSchema/' + loopName + '/' + operationalPolicyName, {
			method: 'PUT',
			headers: {
				"Content-Type": "application/json"
			},
			credentials: 'same-origin'
		})
			.then(function (response) {
				console.debug("Refresh Operational Policy Json Schema response received: ", response.status);
				if (response.ok) {
					return response.json();
				} else {
					console.error("Refresh Operational Policy Json Schema query failed");
					return {};
				}
			})
			.catch(function (error) {
				console.error("Refresh Operational Policy Json Schema error received", error);
				return {};
			});
	}

		static refreshMicroServicePolicyJson(loopName,microServicePolicyName) {
    		return fetch(window.location.pathname + 'restservices/clds/v2/loop/refreshMicroServicePolicyJsonSchema/' + loopName + '/' + microServicePolicyName, {
    			method: 'PUT',
    			headers: {
    				"Content-Type": "application/json"
    			},
    			credentials: 'same-origin'
    		})
    			.then(function (response) {
    				console.debug("Refresh Operational Policy Json Schema response received: ", response.status);
    				if (response.ok) {
    					return response.json();
    				} else {
    					console.error("Refresh Operational Policy Json Schema query failed");
    					return {};
    				}
    			})
    			.catch(function (error) {
    				console.error("Refresh Operational Policy Json Schema error received", error);
    				return {};
    			});
    }

	static addOperationalPolicyType(loopName, policyType, policyVersion) {
		return fetch(window.location.pathname + 'restservices/clds/v2/loop/addOperationaPolicy/' + loopName + '/policyModel/' + policyType +'/' + policyVersion , {
			method: 'PUT',
			headers: {
				"Content-Type": "application/json"
			},
			credentials: 'same-origin'
		})
				.then(function (response) {
				console.debug("Add Operational Policy response received: ", response.status);
				if (response.ok) {
					return response.json();
				} else {
					return response.text();
				}
			})
			.catch(function (error) {
				console.error("Add Operational Policy query failed");
				throw new Error(error);
			})
	}

	static removeOperationalPolicyType(loopName, policyType, policyVersion, policyName) {
		return fetch(window.location.pathname + 'restservices/clds/v2/loop/removeOperationaPolicy/' + loopName + '/policyModel/' + policyType +'/' + policyVersion + '/' + policyName , {
			method: 'PUT',
			headers: {
				"Content-Type": "application/json"
			},
			credentials: 'same-origin'
		})
				.then(function (response) {
					console.debug("Remove Operational Policy response received: ", response.status);
				if (response.ok) {
					return response.json();
				} else {
					console.error("Remove Operational Policy query failed");
					return {};
				}
			})
			.catch(function (error) {
				console.error("Remove Operational Policy error received", error);
				return {};
			});
	}
}
