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

function updateMsProperties(type, newMsProperties) {
    var newMsProperties = JSON.parse(JSON.stringify(cl_props["microServicePolicies"]));
    for (p in newMsProperties) {
        if (newMsProperties[p]["name"] == type) {
        	cl_props["microServicePolicies"][p] = newMsProperties;
        }
    }
}

function updateGlobalProperties(newGlobalProperties) {
    cl_props["globalPropertiesJson"] = newGlobalProperties;
}

function updateOpPolicyProperties(newOpProperties) {	
   cl_props["operationalPolicies"] = newOpProperties;
}

function getLoopName() {
    return cl_props["name"];
}

function getOperationalPolicyProperty() {
    return JSON.parse(JSON.stringify(cl_props["operationalPolicies"]["0"]["configurationsJson"]));
}

function getOperationalPolicies() {
    return JSON.parse(JSON.stringify(cl_props["operationalPolicies"]));
}

function getGlobalProperty() {
    return JSON.parse(JSON.stringify(cl_props["globalPropertiesJson"]));
}

function getDeploymentProperties() {
    return JSON.parse(JSON.stringify(cl_props["globalPropertiesJson"]["dcaeDeployParameters"]));
}

function getMsJson(type) {
    var msProperties = cl_props["microServicePolicies"];
    for (p in msProperties) {
        if (msProperties[p]["name"] == type) {
           return JSON.parse(JSON.stringify(msProperties[p]));
        }
    }
    return null;
}

function getMsProperty(type) {
    var msProperties = cl_props["microServicePolicies"];
    for (p in msProperties) {
        if (msProperties[p]["name"] == type) {
        	if (msProperties[p]["properties"] !== null && msProperties[p]["properties"] !== undefined) {
        		return JSON.parse(JSON.stringify(msProperties[p]["properties"]));
        	}
        }
    }
    return null;
}

function getMsUI(type) {
    var msProperties = cl_props["microServicePolicies"];
    for (p in msProperties) {
        if (msProperties[p]["name"] == type) {
        	return JSON.parse(JSON.stringify(msProperties[p]["jsonRepresentation"]));
        }
    }
    return null;
}

function getLastUpdatedStatus() {
    return cl_props["lastComputedState"];
}

function setLastUpdatedStatus(status) {
    cl_props["lastComputedState"] =  status;
}

function getDeploymentID() {
    return cl_props["dcaeDeploymentId"];
}

function setDeploymentID(deploymentId) {
    cl_props["dcaeDeploymentId"] = deploymentId;
}

function getDeploymentStatusURL() {
    return cl_props["dcaeDeploymentStatusUrl"];
}

function setDeploymentStatusURL(deploymentStatusURL) {
    cl_props["dcaeDeploymentStatusUrl"] = deploymentStatusURL;
}
module.exports = { getOperationalPolicyProperty,getGlobalProperty,getMsProperty,getMsUI,getLastUpdatedStatus,getDeploymentID,getDeploymentStatusURL };