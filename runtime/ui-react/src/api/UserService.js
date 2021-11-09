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

export default class UserService {
  static notLoggedUserName = 'Anonymous';

  static login() {
    return fetch(window.location.pathname + 'restservices/clds/v1/user/getUser', {
      method: 'GET',
      credentials: 'same-origin'
    })
      .then(function (response) {
        console.debug("getUser response received, status code:", response.status);
        if (response.ok) {
          return response.text();
        } else {
          console.error("getUser response is nok");
          return UserService.notLoggedUserName;
        }
      })
      .then(function (data) {
        console.info("User connected:", data)
        return data;
      })
      .catch(function (error) {
        console.warn("getUser error received, user set to: ", UserService.notLoggedUserName);
        console.error("getUser error:", error);
        return UserService.notLoggedUserName;
      });
  }

  static getUserInfo() {
    return fetch(window.location.pathname + 'restservices/clds/v2/clampInformation', {
      method: 'GET',
      credentials: 'same-origin'
    })
      .then(function (response) {
        console.debug("getUserInfo response received, status code:", response.status);
        if (response.ok) {
          return response.json();
        } else {
          return {}
        }
      })
      .then(function (data) {
        console.info("User info received:", data)
        return data;
      })
      .catch(function (error) {
        console.warn("getUserInfo error received, user set to: ", UserService.notLoggedUserName);
        console.error("getUserInfo error:", error);
        return {};
      });
  }
}
