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

app.controller('GlobalPropertiesCtrl',
	['$scope', '$rootScope', '$modalInstance','cldsModelService', '$location', 'dialogs','cldsTemplateService',
		function($scope, $rootScope, $modalInstance, cldsModelService, $location,dialogs,cldsTemplateService) {
		console.log("////////GlobalPropertiesCtrl");							
			//cldsModelService.getASDCServices().then(function(pars) {
			$scope.$watch('name', function(newValue, oldValue) {
				console.log("name");
				if(readOnly){
					$("select,input,.nav-tabs .close").attr("disabled","");
				}
				var services=asdc_Services
				console.log(asdc_Services)
				setASDCFields()
				for(k in services){
		 			$("#service").append("<option value="+k+">"+services[k]+"</option>")
		 		}
		 		var el = elementMap["global"];
		 		console.log(elementMap["global"])
		 		if (el !== undefined) {
		 			for (var i = 0; i < el.length; i++) {
	 					$("#" + el[i].name).val(el[i].value);
		 			}
		 		}
				setMultiSelect();
			});
			$scope.retry = function(){
				console.log("retry");
				
			}
			$scope.close = function() {
				console.log("close");
				$modalInstance.close("closed");
			};
		}
	]
);
