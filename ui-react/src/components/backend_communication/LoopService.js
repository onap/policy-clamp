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

const LoopService = {
    login
};

function login(username, password) {
    let options = {
           method: 'GET'
         };
    if (username && password) {
      options = {
             method: 'GET',
             credentials: 'include',
             headers: {
                'Authorization': "Basic " + new Buffer(username + ":" + password).toString("base64")
             }
           };
    }

    return fetch(`/restservices/clds/v1/user/getUser`, options)
      .then(response => handleResponse(response))
      .then(function(data) {
          localStorage.setItem('user', data);
          console.log(data);
});
}

function handleResponse(response) {
     if (!response.ok || response.redirected === true) {
          if (response.status === 401 || response.status === 500 || response.redirected === true) {
              if (localStorage.getItem('tryBasicAuth')) {
                // login failed, go to invalud login page
                localStorage.removeItem('user');
              } else {
                // try to login with username and password
                localStorage.setItem('tryBasicAuth', true);
              }
          }
          const error = response.statusText;
          return Promise.reject(error);
      }
    return response.text();
}
export default LoopService;
