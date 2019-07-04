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
class LoopCache
{
	constructor(loopJson) {
		this.loopJsonCache=loopJson;
	}
	
	updateMsProperties(type, newMsProperties) {
	    if (newMsProperties["name"] == type) {
	        for (p in this.loopJsonCache["microServicePolicies"]) {
	            if (this.loopJsonCache["microServicePolicies"][p]["name"] == type) {
	                this.loopJsonCache["microServicePolicies"][p] = newMsProperties;
	            }
	        }
	    }
	}
	
	 updateGlobalProperties(newGlobalProperties) {
	    this.loopJsonCache["globalPropertiesJson"] = newGlobalProperties;
	}
	
	 updateOpPolicyProperties(newOpProperties) {	
	   this.loopJsonCache["operationalPolicies"] = newOpProperties;
	}
	
	 getLoopName() {
	    return this.loopJsonCache["name"];
	}
	
	 getOperationalPolicyProperty() {
	    return JSON.parse(JSON.stringify(this.loopJsonCache["operationalPolicies"]["0"]["configurationsJson"]));
	}
	
	 getOperationalPolicies() {
	    return JSON.parse(JSON.stringify(this.loopJsonCache["operationalPolicies"]));
	}
	
	 getGlobalProperty() {
	    return JSON.parse(JSON.stringify(this.loopJsonCache["globalPropertiesJson"]));
	}
	
	 getDeploymentProperties() {
	    return JSON.parse(JSON.stringify(this.loopJsonCache["globalPropertiesJson"]["dcaeDeployParameters"]));
	}
	
	 getMsJson(type) {
	    var msProperties = this.loopJsonCache["microServicePolicies"];
	    for (p in msProperties) {
	        if (msProperties[p]["name"] == type) {
	           return JSON.parse(JSON.stringify(msProperties[p]));
	        }
	    }
	    return null;
	}
	
	 getMsProperty(type) {
	    var msProperties = this.loopJsonCache["microServicePolicies"];
	    for (p in msProperties) {
	        if (msProperties[p]["name"] == type) {
	        	if (msProperties[p]["properties"] !== null && msProperties[p]["properties"] !== undefined) {
	        		return JSON.parse(JSON.stringify(msProperties[p]["properties"]));
	        	}
	        }
	    }
	    return null;
	}
	
	 getMsUI(type) {
	    var msProperties = this.loopJsonCache["microServicePolicies"];
	    for (p in msProperties) {
	        if (msProperties[p]["name"] == type) {
	        	return JSON.parse(JSON.stringify(msProperties[p]["jsonRepresentation"]));
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
export default LoopCache;
