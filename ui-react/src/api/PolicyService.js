/*-
 * ============LICENSE_START=======================================================
 * ONAP POLICY-CLAMP
 * ================================================================================
 * Copyright (C) 2021 AT&T Intellectual Property. All rights reserved.
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

export default class PolicyService {
  static getPoliciesList() {
    return fetch(window.location.pathname + 'restservices/clds/v2/policies', {
        method: 'GET',
        credentials: 'same-origin'
        })
        .then(function(response) {
            console.debug("getPoliciesList response received: ", response.status);
            if (response.ok) {
                console.info("getPoliciesList query successful");
                return response.json();
            } else {
                return response.text().then(responseBody => {
                    throw new Error("HTTP " + response.status + "," + responseBody);
                })
            }
        })
        .catch(function(error) {
            console.error("getPoliciesList error occurred ", error);
            alert("getPoliciesList error occurred " + error);
            return undefined;
        })
  }
  static createNewPolicy(policyModelType, policyModelVersion, policyName, policyVersion, policyJson) {
    return fetch(window.location.pathname + 'restservices/clds/v2/policies/' + policyModelType + '/'
                    + policyModelVersion + '/' + policyName + '/' + policyVersion, {
            method: 'POST',
            credentials: 'same-origin',
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(policyJson)
        })
        .then(function (response) {
            console.debug("createNewPolicy response received: ", response.status);
            if (response.ok) {
                console.info("createNewPolicy query successful");
                return response.text();
            } else {
               return response.text().then(responseBody => {
                    throw new Error("HTTP " + response.status + "," + responseBody);
                })
            }
        })
        .catch(function (error) {
            console.error("createNewPolicy error occurred ", error);
            alert ("createNewPolicy error occurred " + error);
            return undefined;
        });
  }
  static deletePolicy(policyModelType, policyModelVersion, policyName, policyVersion) {
    return fetch(window.location.pathname + 'restservices/clds/v2/policies/' + policyModelType + '/'
        + policyModelVersion + '/' + policyName + '/' + policyVersion, {
            method: 'DELETE',
            credentials: 'same-origin'
        })
        .then(function (response) {
            console.debug("deletePolicy response received: ", response.status);
            if (response.ok) {
                console.info("deletePolicy query successful");
                return response.text();
            } else {
                return response.text().then(responseBody => {
                    throw new Error("HTTP " + response.status + "," + responseBody);
                })
            }
        })
        .catch(function (error) {
            console.error("deletePolicy error occurred ", error);
            alert ("deletePolicy error occurred " + error);
            return undefined;
        });
  }
}
