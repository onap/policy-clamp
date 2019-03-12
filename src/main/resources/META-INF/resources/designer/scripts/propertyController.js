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

function saveMsProperties(type, form) {
	 var newMsProperties = cl_props["microServicePolicies"];
    for (p in newMsProperties) {
        if (newMsProperties[p]["name"] == type) {
        	newMsProperties[p]["properties"] = form;
        }
    }

	 var def = $q.defer();
	 var sets = [];
	 var svcUrl = "/restservices/clds/v2/loop/updateMicroservicePolicies/" + modelName;
	 var svcRequest = {
			 loopName : modelName,
			 newMicroservicePolicies : newMsProperties
	 };
	 $http.post(svcUrl, svcRequest).success(function(data) {
		 def.resolve(data);
	 }).error(function(data) {
		 def.reject("Save Model not successful");
	 });
    cl_props["microServicePolicies"] = newMsProperties;
    return def.promise;
}

function saveGlobalProperties(form) {
	 var def = $q.defer();
	 var sets = [];
	 var svcUrl = "/restservices/clds/v2/loop/globalProperties/" + modelName;
	 var svcRequest = {
			 loopName : modelName,
			 newGlobalPolicies : form
	 };
	 $http.post(svcUrl, svcRequest).success(function(data) {
		 def.resolve(data);
	 }).error(function(data) {
		 def.reject("Save Model not successful");
	 });
    cl_props["globalPropertiesJson"] = form;
    return def.promise;
}

function saveOpPolicyProperties(form) {
	var newOpProperties = cl_props["operationalPolicies"];
	newOpProperties["0"]["configurationsJson"]= form;
	
	var def = $q.defer();
	 var sets = [];
	 var svcUrl = "/restservices/clds/v2/loop/updateOperationalPolicies/" + modelName;
	 var svcRequest = {
			 loopName : modelName,
			 newGlobalPolicies : newOpProperties
	 };
	 $http.post(svcUrl, svcRequest).success(function(data) {
		 def.resolve(data);
	 }).error(function(data) {
		 def.reject("Save Model not successful");
   });
	
   cl_props["operationalPolicies"] = newOpProperties;
   return def.promise;
}

function getOperationalPolicyProperty() {
    return cl_props["operationalPolicies"]["0"]["configurationsJson"];
}

function getGlobalProperty() {
    return cl_props["globalPropertiesJson"];
}

function getMsProperty(type) {
    var msProperties = cl_props["microServicePolicies"];
    for (p in msProperties) {
        if (msProperties[p]["name"] == type) {
           return msProperties[p]["properties"];
        }
    }
    return null;
}

function getMsUI(type) {
    var msProperties = cl_props["microServicePolicies"];
    for (p in msProperties) {
        if (msProperties[p]["name"] == type) {
           return msProperties[p]["jsonRepresentation"];
        }
    }
    return null;
}

function getStatus() {
    return cl_props["lastComputedState"];
}

function getDeploymentID() {
    return cl_props["dcaeDeploymentId"];
}

function getDeploymentStatusURL() {
    return cl_props["dcaeDeploymentStatusUrl"];
}
module.exports = { getOperationalPolicyProperty,getGlobalProperty,getMsProperty,getMsUI,getStatus,getDeploymentID,getDeploymentStatusURL };