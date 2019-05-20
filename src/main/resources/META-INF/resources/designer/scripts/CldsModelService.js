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
.service(
'cldsModelService',
[
	'alertService',
	'$http',
	'$q',
	'$rootScope',
	function(alertService, $http, $q, $rootScope) {

		function checkIfElementType(name) {
		//This will open the methods located in the app.js
			if (undefined == name) {
				return;
			}else if (name === "OperationalPolicy"){
				PolicyWindow();
			} else {
				$rootScope.selectedBoxName = name;
				ToscaModelWindow();
			}
		}
		this.toggleDeploy = function(uiAction, modelName) {
			var svcAction = uiAction.toLowerCase();
			var deployUrl = "/restservices/clds/v2/loop/" + svcAction + "Loop/" + modelName;
			var def = $q.defer();
			var sets = [];
			$http.put(deployUrl).success(
				function(data) {
					def.resolve(data);
					alertService.alertMessage("Action Successful: " + svcAction, 1)
					// update deploymentID, lastUpdatedStatus
					setLastComputedState(data.lastComputedState);
					setDeploymentStatusURL(data.dcaeDeploymentStatusUrl);
					setDeploymentID(data.dcaeDeploymentId);
					setStatus(data.lastComputedState);
					enableDisableMenuOptions();
			}).error(
				function(data) {
					def.resolve(data);
					alertService.alertMessage("Action Failure: " + svcAction, 2);
					def.reject(svcAction + " not successful");
			});
			return def.promise;
		}
		this.getModel = function(modelName) {
		    var def = $q.defer();
		    var sets = [];
		    var svcUrl = "/restservices/clds/v2/loop/" + modelName;
		    $http.get(svcUrl).success(function(data) {
		    	cl_props = data;
		    	setStatus(data.lastComputedState);
			    def.resolve(data);
		    }).error(function(data) {
			    def.reject("Open Model not successful");
		    });
		    return def.promise;
	    };
	    this.getSavedModel = function() {
		    var def = $q.defer();
		    var sets = [];
		    var svcUrl = "/restservices/clds/v2/loop/getAllNames";
		    $http.get(svcUrl).success(function(data) {

			    def.resolve(data);
		    }).error(function(data) {

			    def.reject("Open Model not successful");
		    });
		    return def.promise;
	    };
	    this.processAction = function(uiAction, modelName) {
		    var def = $q.defer();
		    var sets = [];
		    var svcAction = uiAction.toLowerCase();
		    var svcUrl = "/restservices/clds/v2/loop/" + svcAction + "/" + modelName;

			$http.put(svcUrl).success(
				function(data) {
					def.resolve(data);
					alertService.alertMessage("Action Successful: " + svcAction, 1)
					// update deploymentID, lastUpdatedStatus
					setLastComputedState(data.lastComputedState);
					setDeploymentStatusURL(data.dcaeDeploymentStatusUrl);
					setDeploymentID(data.dcaeDeploymentId);
			}).error(
				function(data) {
					def.resolve(data);
					alertService.alertMessage("Action Failure: " + svcAction, 2);
					def.reject(svcAction + " not successful");
			});
		    
		    return def.promise;
	    };
	    this.manageAction = function(modelName, typeId, typeName) {

		    var def = $q.defer();
		    var sets = [];
		    var config = {
		        url : "/restservices/clds/v1/clds/getDispatcherInfo",
		        method : "GET",
		        params : {
		            model : modelName,
		            typeId : typeId,
		            typeName : typeName
		        }
		    };
		    $http(config).success(function(data) {

			    def.resolve(data);
		    }).error(function(data) {

			    def.reject("manage action not successful");
		    });
		    return def.promise;
	    };
	    this.processActionResponse = function(modelName) {
	    	// populate control name (prefix and uuid here)
	    	$("#loop_name").text(getLoopName());
		    manageCLImage(modelName);
		    enableDisableMenuOptions();
	    };
	    this.refreshStatus = function(modelName) {
		    var def = $q.defer();
		    var sets = [];
		    var svcUrl = "/restservices/clds/v2/loop/getstatus/" + modelName;
		    $http.get(svcUrl).success(function(data) {
		    	setStatus(data.lastComputedState);
			    def.resolve(data);
		    }).error(function(data) {
			    def.reject("Refresh Status not successful");
		    });
		    return def.promise;
		    enableDisableMenuOptions();
	    }
	    function setStatus(status) {
		    // apply color to status
		    var statusColor = 'white';
		    if (status.trim() === "DESIGN") {
			    statusColor = 'gray'
		    } else if (status.trim() === "DISTRIBUTED") {
			    statusColor = 'blue'
		    } else if (status.trim() === "SUBMITTED") {
			    statusColor = 'green'
		    } else if (status.trim() === "STOPPED") {
			    statusColor = 'red'
		    } else if (status.trim() === "DELETING") {
			    statusColor = 'pink'
		    } else if (status.trim() === "ERROR") {
			    statusColor = 'orange'
		    } else if (status.trim() === "UNKNOWN") {
			    statusColor = 'blue'
		    } else {
			    statusColor = null;
		    }
		    var statusMsg = '<span style="background-color:'
		    + statusColor
		    + ';-moz-border-radius: 50px;  -webkit-border-radius: 50px;  border-radius: 50px;">&nbsp;&nbsp;&nbsp;'
		    + status + '&nbsp;&nbsp;&nbsp;</span>';
		    // display status
		    if ($("#status_clds").length >= 1)
			    $("#status_clds").remove();
		    $("#activity_modeler")
		    .append(
		    '<span id="status_clds" style="position: absolute;  left: 61%;top: 151px; font-size:20px;">Status: '
		    + statusMsg + '</span>');
	    }
	    function manageCLImage(modelName) {
	    	getModelImage(modelName).then(function(pars) {
		    	var svg = pars;
			    if ($("#svgContainer").length > 0)
				    $("#svgContainer").remove();
			    $("#loop-content").append(
			    "<span id=\"svgContainer\">" + svg + "</span>");
			    $("#svgContainer svg").attr('width', '100%');
			    $("#svgContainer svg").attr('height', '100%');
			    $("#svgContainer").click(
			    function(event) {
				    var name = $($($($(event.target).parent()).parent()).parent()).attr("data-element-id")
				    checkIfElementType(name)
			    });
			}, function(data) {
			});
	    }
	    enableDisableMenuOptions = function() {
		    enableDefaultMenu();
	    	enableAllActionMenu();
	    }
	    getModelImage = function(modelName) {
		    var def = $q.defer();
		    var sets = [];
		    var svcUrl = "/restservices/clds/v2/loop/svgRepresentation/" + modelName;
		    $http.get(svcUrl).success(function(data) {
			    def.resolve(data);
		    }).error(function(data) {
			    def.reject("Not able to get the model image");
		    });
		    return def.promise;
	    };
    } ]);
