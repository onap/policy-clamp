/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2019, 2021 AT&T Intellectual Property. All rights reserved.
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

export default class PolicyToscaService {
  static getToscaPolicyModels() {
    return fetch(window.location.pathname + 'restservices/clds/v2/policyToscaModels', { method: 'GET', credentials: 'same-origin' })
      .then(function (response) {
        console.debug("getToscaPolicyModels response received: ", response.status);
        if (response.ok) {
          return response.json();
        } else {
          console.error("getToscaPolicyModels query failed");
          return {};
        }
      })
      .catch(function (error) {
        console.error("getToscaPolicyModels error received", error);
        return {};
      });
  }

  static getToscaPolicyModelYaml(policyModelType, policyModelVersion) {
        return fetch(window.location.pathname + 'restservices/clds/v2/policyToscaModels/yaml/' + policyModelType + "/" + policyModelVersion, {
            method: 'GET',
            credentials: 'same-origin'
        })
            .then(function (response) {
                console.debug("getToscaPolicyModelYaml response received: ", response.status);
                if (response.ok) {
                    return response.json();
                } else {
                    console.error("getToscaPolicyModelYaml query failed");
                    return "";
                }
            })
            .catch(function (error) {
                console.error("getToscaPolicyModelYaml error received", error);
                return "";
            });
 }

 static getToscaPolicyModel(policyModelType, policyModelVersion) {
         return fetch(window.location.pathname + 'restservices/clds/v2/policyToscaModels/' + policyModelType + "/" + policyModelVersion, {
             method: 'GET',
             credentials: 'same-origin'
         })
             .then(function (response) {
                 console.debug("getToscaPolicyModel response received: ", response.status);
                 if (response.ok) {
                     return response.json();
                 } else {
                     console.error("getToscaPolicyModel query failed");
                     return {};
                 }
             })
             .catch(function (error) {
                 console.error("getToscaPolicyModel error received", error);
                 return {};
             });
  }

  static createPolicyModelFromToscaModel(jsonData) {
       return fetch(window.location.pathname + 'restservices/clds/v2/policyToscaModels', {
           method: 'POST',
           credentials: 'same-origin',
           headers: {
             "Content-Type": "a",
           },
           body: JSON.stringify(jsonData)
         })
         .then(function(response) {
           console.debug("createPolicyModelFromToscaModel response received: ", response.status);
           if (response.ok) {
             var message = {
               status: response.status,
               message: 'Tosca Policy Model successfully uploaded'
             };
             return message;
           } else {
             console.error("createPolicyModelFromToscaModel failed");
             return response.text();
           }
         })
         .catch(function(error) {
           console.error("createPolicyModelFromToscaModel error received", error);
           return "";
         });
     }

     static updatePolicyModelTosca(policyModelType, policyModelVersion, jsonData) {
         return fetch(window.location.pathname + 'restservices/clds/v2/policyToscaModels/' + policyModelType + '/' + policyModelVersion, {
             method: 'PUT',
             credentials: 'same-origin',
             headers: {
               "Content-Type": "a",
             },
             body: JSON.stringify(jsonData)
           })
           .then(function(response) {
             console.debug("updatePolicyModelTosca response received: ", response.status);
             if (response.ok) {
               var message = {
                 status: response.status,
                 message: 'Tosca Policy Model successfully uploaded'
               };
               return message;
             } else {
               console.error("updatePolicyModelTosca failed");
               return response.text();
             }
           })
           .catch(function(error) {
             console.error("updatePolicyModelTosca error received", error);
             return "";
           });
       }
}
