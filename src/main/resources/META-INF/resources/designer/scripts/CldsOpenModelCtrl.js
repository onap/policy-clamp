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
app
.controller(
'CldsOpenModelCtrl',
[
'$scope',
'$rootScope',
'$modalInstance',
'$window',
'$uibModalInstance',
'cldsModelService',
'$location',
'dialogs',
'cldsTemplateService',
function($scope, $rootScope, $modalInstance, $window, $uibModalInstance, cldsModelService, $location,
         dialogs, cldsTemplateService) {
	$scope.typeModel = 'template';
	$scope.error = {
	flag : false,
	message : ""
	};
	cldsModelService.getSavedModel().then(function(pars) {
		$scope.modelNamel = []
		for (var i = 0; i < pars.length; i++) {
			$scope.modelNamel.push(pars[i].value);
		}
		setTimeout(function() {
			setMultiSelect();
		}, 100);
	});
	$scope.paramsRetry = function() {
		// $("#paramsWarn").hide();
		var currentValue = $("#service").val() == null ? previous : $(
		"#service").val();
		$("#ridinSpinners").css("display", "")
		loadSharedPropertyByService(currentValue, true, callBack);
		$("#ridinSpinners").css("display", "none")
	};
	$scope.paramsCancel = function() {
		loadSharedPropertyByServiceProperties(callBack);
		$("#paramsWarnrefresh").hide();
	};
	function completeClose() {
		// if(flag) {
		$scope.close();
		// }
	}
	function callBack(flag) {
		if (flag) {
			$scope.close();
		}
	}
	$scope.refreshASDC = function() {
		$("#ridinSpinners").css("display", "")
		var bool = loadSharedPropertyByService(undefined, true, callBack);
		$("#ridinSpinners").css("display", "none");
	}
	cldsTemplateService.getSavedTemplate().then(function(pars) {
		$scope.templateNamel = []
		for (var i = 0; i < pars.length; i++) {
			$scope.templateNamel.push(pars[i].value);
		}
		setTimeout(function() {
			setMultiSelect();
		}, 100);
	});
	function contains(a, obj) {
		var i = a && a.length > 0 ? a.length : 0;
		while (i--) {
			if (a[i].toLowerCase() === obj.toLowerCase()) {
				return true;
			}
		}
		return false;
	}
	$scope.checkExisting=function(checkVal, errPatt, num){
		var name = checkVal;
		if (!errPatt && (checkVal!== undefined)){
			if(contains($scope.modelNamel,name)){
				$scope["nameinUse"+num]=true;
				return true;
			}else{
				$scope["nameinUse"+num]=false;
				return false;
			}
		} else {
			$scope["nameinUse"+num]=false;
			return false;
		}
	}
	function specialCharacters() {
		$scope.spcl = false;
		if (angular.element("#modelName")
		&& angular.element("#modelName").scope().model.$error.pattern
		&& angular.element("#modelName").scope().model.$error.pattern.length > 0) {
			$scope.spcl = true;
		}
	}
	$scope.setTypeModel = function(_type) {
		$scope.error.flag = false;
		$scope.typeModel = _type;
	}
	$scope.close = function() {
		$rootScope.isNewClosed = false;
		$uibModalInstance.close("closed");
	};
	$scope.closeDiagram=function(){
		$window.location.reload();
	}
	$scope.revertChanges = function() {
		$scope.openModel();
	}
	$scope.openModel = function() {
		reloadDefaultVariables(false)
		if (document.getElementById("readOnly")) {
			readMOnly = document.getElementById("readOnly").checked;
		}
		var modelName = document.getElementById("modelName").value;
		// init UTM items
		$scope.utmModelsArray = [];
		$scope.selectedParent = {};
		$scope.currentUTMModel = {};
		$scope.currentUTMModel.selectedParent = {};
		$rootScope.oldUTMModels = [];
		$rootScope.projectName = "clds_default_project";
		var utmModels = {};
		utmModels.name = modelName;
		utmModels.subModels = [];
		utmModels.type = 'Model';
		$rootScope.utmModels = utmModels;
		cldsModelService.getModel(modelName).then(function(pars) {
			// process data returned
			var propText = pars.propText;
			var status = pars.status;
			controlNamePrefix = pars.controlNamePrefix;
			var authorizedToUp = pars.userAuthorizedToUpdate;
			typeID = pars.typeId;
			controlNameUuid = pars.controlNameUuid;
			selected_template = pars.templateName;
			modelEventService = pars.event;
			actionStateCd = pars.event.actionStateCd;
			deploymentId = pars.deploymentId;
			if (readMOnly) {
				pars.permittedActionCd = [ "" ];
			}

			// deserialize model properties
			if (propText == null) {
			} else {
				elementMap = JSON.parse(propText);
			}
			var simple = elementMap.simpleModel;
			$rootScope.isSimpleModel = simple;
			selected_model = modelName;
			cldsModelService.processActionResponse(modelName, pars, simple);
			// set model bpmn and open diagram
			$rootScope.isPalette = true;
		}, function(data) {
		});
		$uibModalInstance.close("closed");
	};
	setMultiSelect();
} ]);
