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
    return fetch(window.location.pathname + 'restservices/clds/v2/policies/list', { method: 'GET', credentials: 'same-origin' })
      .then(function (response) {
        console.debug("getPoliciesList response received: ", response.status);
        if (response.ok) {
          return response.json();
        } else {
          console.error("getPoliciesList query failed");
          return {};
        }
      })
      .catch(function (error) {
        console.error("getPoliciesList error received", error);
        return {};
      });
  }
}
