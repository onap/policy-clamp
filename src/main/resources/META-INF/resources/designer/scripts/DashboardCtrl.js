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
angular
.module('clds-app')
.controller(
'DashboardCtrl',
[
'$scope',
'$rootScope',
'$resource',
'$http',
'$timeout',
'$location',
'$interval',
function($scope, $rootScope, $resource, $http, $timeout, $location, $interval) {
	console.log("//////////////DashboardCtrl");
	$scope.autosaveInterval = 1800000;
	$rootScope.isModel = false;
	$rootScope.isPalette = false;
	$rootScope.isTestSet = false;
	$rootScope.isRequirementCoverage = false;
	$rootScope.ispropertyExplorer = false;
	$rootScope.parameters;
	$scope.orientation = "horizontal";
	$rootScope.ispropertyExplorer = false;
	$rootScope.isActive = true;
	$rootScope.models = [];
	$scope.selectedParent = {};
	$rootScope.utmModels = {};
	$rootScope.selectedModelName;
	$rootScope.dialogName = "";
	$interval(function() {
		console.log("interval");
	}, $scope.autosaveInterval);
	$rootScope.onSelectActivityModel = function(obj) {
		console.log("onSelectActivityModel");
		$rootScope.isPalette = true;
		$rootScope.isTestSet = false;
		$rootScope.isRequirementCoverage = false;
		$rootScope.ispropertyExplorer = false;
		$("#activity_modeler").prev().css("display", "block");
		$('div').find('.k-expand-next').click();
		if (obj == undefined) {
			document.getElementById("modeler_name").textContent = "Activity Modeler";
		} else {
			selected_model = obj;
			document.getElementById("modeler_name").textContent = "Activity Modeler"
			+ "  - " + selected_model;
		}
		$rootScope.modelName = selected_model;
		$rootScope.modeltestset = list_model_test_sets[selected_model];
		if (list_model_schema_extensions[selected_model] == null) {
			if (list_model_schema_extensions[$rootScope.utmModels.name] != null) {
				list_model_schema_extensions[selected_model] = jQuery.extend(
				true, {},
				list_model_schema_extensions[$rootScope.utmModels.name]);
			} else {
				list_model_schema_extensions[selected_model] = {};
			}
		}
		$rootScope.initProjectExplorer();
		changecolor(selected_model);
	};
	$scope.selectActivityTestSet = function() {
		console.log("selectActivityTestSet");
		$rootScope.isPalette = false;
		$rootScope.isRequirementCoverage = false;
		$rootScope.isTestset = true;
		document.getElementById("modeler_name").textContent = "UTM Test Set";
		$('div').find('.k-collapse-next').click();
		$rootScope.modeltestset = list_model_test_sets[selected_model];
		$rootScope.$apply();
	};
	$scope.showPalette = function() {
		console.log("showPalette");
		$rootScope.isModel = true;
	};
	$rootScope.filterRouter = 'partials/DashboardFilters.html';
	$scope.isActivePROD = true;
	$scope.isActiveQC = false;
	$rootScope.reload = function(env) {
		console.log("reload");
	};
	$scope.showTDSSView = true;
} ]);
function changecolor(selected_model) {
	console.log("changecolor");
	var i = 0;
	// var modelNames =[];
	$(".models").each(function(i) {
		console.log("each");
		var model_value = $(this).text().trim();
		// modelName.push(model_value);
		if (model_value == selected_model || model_value == "") {
			$(this).addClass("selectedcolor");
		} else {
			$(this).removeClass("selectedcolor");
		}
		i++;
	});
}
