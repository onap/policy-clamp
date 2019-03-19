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
 * 
 */
app.service('operationalPolicyService', ['$http', '$q', function ($http, $q) {
    console.log("//////OperationalPolicyService");
    
 	this.saveOpPolicyProperties = function(form) {
 		var modelName = getLoopName();
 	   	 var def = $q.defer();
 	   	 var svcUrl = "/restservices/clds/v2/loop/updateOperationalPolicies/" + modelName;
 	   	 $http.post(svcUrl, form).success(function(data) {
 	   		 def.resolve(data);
 	   	 }).error(function(data) {
 	   		 def.reject("Save Operational Policy not successful");
 	   	 });
 	       return def.promise;
 	};
}]);