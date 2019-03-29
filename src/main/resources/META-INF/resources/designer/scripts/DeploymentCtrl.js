/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2018 AT&T Intellectual Property. All rights
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
app
.controller(
'DeploymentCtrl',
[
'$scope',
'$rootScope',
'$uibModalInstance',
'$http',
'$q',
'data',
'dialogs',
'cldsModelService',
function($scope, $rootScope, $uibModalInstance, $http, $q, data, dialogs, cldsModelService) {
	function validate_and_set_deploy_parameters() {
			var form = $("#deployForm")[0];
			var obj = {};
			for( var i = 0; i < form.length; ++i ) {
				var name = form[i].name;
				var value = form[i].value;
				if( name ) {
					obj[ name ] = value;
				}
			}

			var el = getGlobalProperty();
			el["dcaeDeployParameters"] = obj;
			$scope.saveGlobalProperties(JSON.stringify(el)).then(function(pars) {
				updateGlobalProperties(el);
			}, function(data) {
			});
	}

	$scope.load_deploy_parameters = function() {
		var el = getDeploymentProperties();
		for (var key in el) {
			var propertyValue = el[key];
			$('#deployForm').append(
				$('<label for="' + key + '" class="control-label">' + key + '  </label>'));
			$('#deployForm').append(
				$('<input style="width: 100%; clear: both;" class="form-control" name="'
				+ key + '"></input>').val(propertyValue).html(propertyValue));
		}
	}
	$scope.deploy = function() {
		validate_and_set_deploy_parameters();
		$uibModalInstance.close();
	};
	$scope.close = function() {
		$uibModalInstance.dismiss();
	};
	$scope.saveGlobalProperties = function(form) {
		var modelName = getLoopName();
	   	 var def = $q.defer();
	   	 var svcUrl = "/restservices/clds/v2/loop/updateGlobalProperties/" + modelName;
	   	 $http.post(svcUrl, form).success(function(data) {
	   		 def.resolve(data);
	   	 }).error(function(data) {
	   		 def.reject("Save Global properties not successful");
	   	 });
	       return def.promise;
	   };
} ]);
