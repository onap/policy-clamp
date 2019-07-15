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

export default class LoopCache {
	loopJsonCache;

	constructor(loopJson) {
		this.loopJsonCache=loopJson;
	}

	updateMicroServiceProperties(type, newMsProperties) {
		if (newMsProperties["name"] === type) {
			for (var policy in this.loopJsonCache["microServicePolicies"]) {
				if (this.loopJsonCache["microServicePolicies"][policy]["name"] === type) {
					this.loopJsonCache["microServicePolicies"][policy] = newMsProperties;
				}
			}
		}
	}

	updateGlobalProperties(newGlobalProperties) {
		this.loopJsonCache["globalPropertiesJson"] = newGlobalProperties;
	}

	updateOperationalPolicyProperties(newOpProperties) {
		this.loopJsonCache["operationalPolicies"] = newOpProperties;
	}

	getLoopName() {
		return this.loopJsonCache["name"];
	}

	getOperationalPolicyConfigurationJson() {
		return JSON.parse(JSON.stringify(this.loopJsonCache["operationalPolicies"]["0"]["configurationsJson"]));
	}

	getOperationalPolicies() {
		return JSON.parse(JSON.stringify(this.loopJsonCache["operationalPolicies"]));
	}

	getGlobalProperties() {
		return JSON.parse(JSON.stringify(this.loopJsonCache["globalPropertiesJson"]));
	}

	getDcaeDeploymentProperties() {
		return JSON.parse(JSON.stringify(this.loopJsonCache["globalPropertiesJson"]["dcaeDeployParameters"]));
	}

	getMicroServicesJsonForType(type) {
		var msProperties = this.loopJsonCache["microServicePolicies"];
		for (var policy in msProperties) {
			if (msProperties[policy]["name"] === type) {
				return JSON.parse(JSON.stringify(msProperties[policy]));
			}
		}
		return null;
	}

	getMicroServiceProperties(type) {
		var msProperties = this.loopJsonCache["microServicePolicies"];
		for (var policy in msProperties) {
			if (msProperties[policy]["name"] === type) {
				if (msProperties[policy]["properties"] !== null && msProperties[policy]["properties"] !== undefined) {
					return JSON.parse(JSON.stringify(msProperties[policy]["properties"]));
				}
			}
		}
		return null;
	}

	getMicroServiceJsonRepresentationForType(type) {
		var msProperties = this.loopJsonCache["microServicePolicies"];
		for (var policy in msProperties) {
			if (msProperties[policy]["name"] === type) {
				return JSON.parse(JSON.stringify(msProperties[policy]["jsonRepresentation"]));
			}
		}
		return null;
	}

	getResourceDetailsVfProperty() {
		return this.loopJsonCache["modelPropertiesJson"]["resourceDetails"]["VF"];
	}

	getResourceDetailsVfModuleProperty() {
		return this.loopJsonCache["modelPropertiesJson"]["resourceDetails"]["VFModule"];
	}

	getLoopLogsArray() {
		return this.loopJsonCache.loopLogs;
	}

	getComponentStates() {
		return this.loopJsonCache.components;
	}
}
