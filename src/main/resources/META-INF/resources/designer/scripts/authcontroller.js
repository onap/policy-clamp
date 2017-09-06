/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights
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
 * ECOMP is a trademark and service mark of AT&T Intellectual Property.
 */

'use strict';

function AuthenticateCtrl($scope, $rootScope, $window, $resource, $http, $location, $cookies, md5) {
  console.log("//////////AuthenticateCtrl");
  $scope.getInclude = function() {
    console.log("getInclude011111111");
    var invalidUser = $window.localStorage.getItem("isInvalidUser");
    var isAuth = $window.localStorage.getItem("isAuth");

    if (invalidUser != null && invalidUser == 'true') {
      console.log("Authentication failed");
      $window.localStorage.removeItem("isInvalidUser");
      window.location.href = "/designer/invalid_login.html";
    } else if (isAuth == null || isAuth == 'false') {
      return "authenticate.html";
    }
    // Reassign the login user info, to be used in menu.html
    $rootScope.loginuser = $window.localStorage.getItem("loginuser");
    return "utmdashboard.html";
  };

  $scope.authenticate = function() {
    var username = $scope.username;
    var pass = $scope.password;
    if (!username || !pass) {
      console.log("Invalid username/password");
      $window.localStorage.setItem("isInvalidUser", true);
      return;
    }
    var hashpass = md5.createHash(pass);
    var headers = username ? {
      authorization: "Basic " +
        btoa(username + ":" + hashpass)
    } : {};
    // send request to a test API with the username/password to verify the authorization
    $http.get('/restservices/clds/v1/user/testUser', {
      headers: headers
    }).success(function(data) {
      if (data) {
        $window.localStorage.setItem("isAuth", true);
        $window.localStorage.setItem("loginuser", $scope.username);
        $rootScope.loginuser = $scope.username;
      } else {
        $window.localStorage.removeItem("isInvalidUser", true);
      }
      callback && callback();
    }).error(function() {
      $window.localStorage.removeItem("isInvalidUser", true);
      callback && callback();
    });
  };

}
