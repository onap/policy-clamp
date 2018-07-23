/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2017-2018 AT&T Intellectual Property. All rights
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
'use strict';
angular.module('clds-app').controller(
'AuthenticateCtrl',
[
'$scope',
'$rootScope',
'$window',
'$resource',
'$http',
'$location',
'$cookies',
function($scope, $rootScope, $window, $resource, $http, $location, $cookies) {
	console.log("//////////AuthenticateCtrl");
	$scope.getInclude = function() {
		console.log("getInclude011111111");
		var invalidUser = $window.localStorage.getItem("invalidUser");
		var isAuth = $window.localStorage.getItem("isAuth");
		if (invalidUser == 'true')
			return "invalid_login.html";
		else if (isAuth == null || isAuth == 'false') {
			return "authenticate.html";
		}
		return "utmdashboard.html";
	};
	$scope.authenticate = function() {
		// send request to a test API for authentication/authorization check
		$http.get('/restservices/clds/v1/user/getUser', {}).success(
		function(data) {
			if (data) {
				$window.localStorage.setItem("isAuth", true);
				$rootScope.loginuser = data;
			}
			window.localStorage.removeItem("invalidUser");
		}).error(function() {
			$window.localStorage.setItem("invalidUser", true);
		});
	};
} ]);