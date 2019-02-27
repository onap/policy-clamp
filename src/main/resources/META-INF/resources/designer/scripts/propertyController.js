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
    for (p in cl_props["microServicePolicies"]) {
        if (cl_props["microServicePolicies"][p]["name"] == type) {
        	cl_props["microServicePolicies"][p]["properties"] = form;
        }
    }
}

function saveGlobalProperties(form) {
    cl_props["globalPropertiesJson"] = form;
}

function saveOpPolicyProperties(form) {
        cl_props["operationalPolicies"]["0"]["configurationsJson"] = form;
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

function loadSharedPropertyByService(onChangeUUID, refresh, callBack) {
      setASDCFields()
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
