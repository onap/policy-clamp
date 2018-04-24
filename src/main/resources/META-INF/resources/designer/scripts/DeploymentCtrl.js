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

app.controller('DeploymentCtrl',
               ['$scope','$rootScope','$modalInstance','data','dialogs', 'cldsModelService',
       function( $scope,  $rootScope,  $modalInstance,  data,  dialogs,   cldsModelService) {

           function validate_and_set_deploy_parameters () {
        	   var parameters = $("#deployProperties").val();
               try {
                   parameters = JSON.parse(parameters);
                   set_deploy_parameters(parameters);
               } catch (e) {
                   console.error("Couldn't parse deploy parameters json");
               }
           }

           function set_deploy_parameters(parameters) {
               if (!'global' in elementMap) {
                   elementMap["global"] = [];
               }

               var index = elementMap["global"].findIndex(function (e) { return (typeof e == "object" && !(e instanceof Array)) && "deployParameters" == e["name"]; }); 
               if (index == -1) { 
                   elementMap["global"].push({"name": "deployParameters", "value": parameters}); 
               } else { 
                   elementMap["global"][index]["value"] =  parameters; 
               }
           }

           $scope.load_deploy_parameters = function () {
        	   var index = elementMap["global"].findIndex(function (e) { return (typeof e == "object" && !(e instanceof Array)) && "deployParameters" == e["name"]; }); 
               if (index != -1) { 
            	   $('#deployProperties').val(JSON.stringify(elementMap["global"][index]["value"]))
               }
           }

           $scope.deploy = function() {
        	   validate_and_set_deploy_parameters ();
               $modalInstance.close();
           };

           $scope.close = function() {
               $modalInstance.dismiss();
           };
       }

]);
