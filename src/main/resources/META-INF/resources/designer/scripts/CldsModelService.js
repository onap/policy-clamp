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

	    function checkIfElementType(name, isSimple) {

        //This will open the methods located in the app.js
	  	  if (isSimple){
	  		  if (undefined == name) {
	  			  return;
	  		  }else if (name.toLowerCase().indexOf("policy") >= 0){
	  				  PolicyWindow();
	  		  } else {
	  			  $rootScope.selectedBoxName = name.toLowerCase();
	  			  ToscaModelWindow();
	  		  }
	  	  } else {
	  		  if (undefined == name) {
	  			  return;
	  		  }
	  		mapping = {
			        'tca' : TCAWindow,
			        'policy' : PolicyWindow,
			        'vescollector' : VesCollectorWindow,
			        'holmes' : HolmesWindow,
			    };
			    key = name.split('_')[0].toLowerCase()
			    if (key in mapping) {
				    mapping[key]();
			    }
	  	  };
	    }
	    function handleQueryToBackend(def, svcAction, svcUrl, svcPayload) {

		    $http.put(svcUrl, svcPayload).success(
		    function(data) {

			    def.resolve(data);
			    if (typeof data.statusCodeValue === 'undefined'
			    || data.statusCodeValue === 200) {
				    alertService.alertMessage(
				    "Action Successful: " + svcAction, 1)
			    } else {
				    if (typeof data.body !== 'undefined') {
					    alertService.alertMessage("Action Failure: "
					    + svcAction + ", " + data.body.errorMessageForUi, 2);
				    } else {
					    alertService.alertMessage("Action Failure: "
					    + svcAction, 2);
				    }
				    def.reject(svcAction + " not successful");
			    }
		    }).error(
		    function(data) {

			    def.resolve(data);
			    if (typeof data.body !== 'undefined') {
				    alertService.alertMessage("Action Failure: " + svcAction
				    + ", " + data.body.errorMessageForUi, 2);
			    } else {
				    alertService
				    .alertMessage("Action Failure: " + svcAction, 2);
			    }
			    def.reject(svcAction + " not successful");
		    });
	    }
	    this.toggleDeploy = function(uiAction, modelName, controlNamePrefixIn,
	                                 bpmnTextIn, propTextIn, svgXmlIn,
	                                 templateName, typeID, controlNameUuid,
	                                 modelEventService, deploymentId) {

		    var def = $q.defer();
		    var sets = [];
		    var action = uiAction.toLowerCase();
		    var deployUrl = "/restservices/clds/v1/clds/" + action + "/"
		    + modelName;
		    var requestData = {
		        name : modelName,
		        controlNamePrefix : controlNamePrefixIn,
		        bpmnText : bpmnTextIn,
		        propText : propTextIn,
		        imageText : svgXmlIn,
		        templateName : templateName,
		        typeId : typeID,
		        controlNameUuid : controlNameUuid,
		        event : modelEventService,
		        deploymentId : deploymentId
		    };
		    handleQueryToBackend(def, action, deployUrl, requestData);
		    return def.promise;
	    };
	    this.getASDCServices = function() {

		    var def = $q.defer();
		    var sets = [];
		    var svcUrl = "/restservices/clds/v1/clds/sdc/services/";
		    $http.get(svcUrl).success(function(data) {

			    def.resolve(data);
		    }).error(function(data) {

			    def.reject("sdc Services not found");
		    });
		    return def.promise;
	    };
	    this.getASDCService = function(uuid) {

		    var def = $q.defer();
		    var sets = [];
		    var svcUrl = "/restservices/clds/v1/clds/sdc/services/" + uuid;
		    $http.get(svcUrl).success(function(data) {

			    def.resolve(data);
		    }).error(function(data) {

			    def.reject("SDC service not found");
		    });
		    return def.promise;
	    };
	    this.getModel = function(modelName) {

		    var def = $q.defer();
		    var sets = [];
		    var svcUrl = "/restservices/clds/v1/clds/model/" + modelName;
		    $http.get(svcUrl).success(function(data) {

			    def.resolve(data);
		    }).error(function(data) {

			    def.reject("Open Model not successful");
		    });
		    return def.promise;
	    };
	    this.getSavedModel = function() {

		    var def = $q.defer();
		    var sets = [];
		    var svcUrl = "/restservices/clds/v1/clds/model-names";
		    $http.get(svcUrl).success(function(data) {

			    def.resolve(data);
		    }).error(function(data) {

			    def.reject("Open Model not successful");
		    });
		    return def.promise;
	    };
	    this.setModel = function(modelName, controlNamePrefixIn, bpmnTextIn,
	                             propTextIn) {

		    var def = $q.defer();
		    var sets = [];
		    var svcUrl = "/restservices/clds/v1/clds/model/" + modelName;
		    var svcRequest = {
		        name : modelName,
		        controlNamePrefix : controlNamePrefixIn,
		        bpmnText : bpmnTextIn,
		        propText : propTextIn
		    };
		    $http.put(svcUrl, svcRequest).success(function(data) {

			    def.resolve(data);
		    }).error(function(data) {

			    def.reject("Save Model not successful");
		    });
		    return def.promise;
	    };
	    this.processAction = function(uiAction, modelName, controlNamePrefixIn,
	                                  bpmnTextIn, propTextIn, svgXmlIn,
	                                  templateName, typeID, deploymentId) {

		    var def = $q.defer();
		    var sets = [];
		    // console.log(svgXmlIn);
		    var svcUrl = "/restservices/clds/v1/clds/";
		    var svcAction = uiAction.toLowerCase();
		    if (svcAction == "save" || svcAction == "refresh") {
			    svcUrl = svcUrl + "model/" + modelName;
		    } else if (svcAction == "test") {
			    svcUrl = svcUrl + "action/submit/" + modelName + "?test=true";
		    } else {
			    svcUrl = svcUrl + "action/" + svcAction + "/" + modelName;
		    }
		    var svcRequest = {
		        name : modelName,
		        controlNamePrefix : controlNamePrefixIn,
		        bpmnText : bpmnTextIn,
		        propText : propTextIn,
		        imageText : svgXmlIn,
		        templateName : templateName,
		        typeId : typeID,
		        deploymentId : deploymentId
		    };
		    handleQueryToBackend(def, svcAction, svcUrl, svcRequest);
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
	    this.checkPermittedActionCd = function(permittedActionCd, menuText,
	                                           actionCd) {

		    if (permittedActionCd.indexOf(actionCd) > -1) {
			    document.getElementById(menuText).classList.remove('ThisLink');
		    } else {
			    document.getElementById(menuText).classList.add('ThisLink');
		    }
	    };
	    this.processActionResponse = function(modelName, pars, simple) {

		    // populate control name (prefix and uuid here)
		    var controlNamePrefix = pars.controlNamePrefix;
		    var controlNameUuid = pars.controlNameUuid;
		    deploymentId = pars.deploymentId;
		    var modelEventService = pars.modelEventService;
		    typeID = pars.typeId;
		    var headerText = "Closed Loop Modeler - " + modelName;
		    if (controlNameUuid != null) {
			    var actionCd = pars.event.actionCd;
			    var actionStateCd = pars.event.actionStateCd;
			    headerText = headerText + " [" + controlNamePrefix
			    + controlNameUuid + "]";
		    }
		    document.getElementById("modeler_name").textContent = headerText;
		    document.getElementById("templa_name").textContent = ("Template Used - " + selected_template);
		    setStatus(pars)
		    disableBPMNAddSVG(pars, simple);
		    this.enableDisableMenuOptions(pars);
	    };
	    this.processRefresh = function(pars) {

		    var newPars = pars;
		    if (typeof pars.body !== 'undefined') {
			    newPars = pars.body;
		    }
		    typeID = newPars.typeId;
		    deploymentId = newPars.deploymentId;
		    setStatus(newPars);
		    this.enableDisableMenuOptions(newPars);
		    elementMap = JSON.parse(newPars.propText);
	    }
	    function setStatus(pars) {

		    var status = pars.status;
		    // apply color to status
		    var statusColor = 'white';
		    if (status.trim() === "DESIGN") {
			    statusColor = 'gray'
		    } else if (status.trim() === "DISTRIBUTED") {
			    statusColor = 'blue'
		    } else if (status.trim() === "ACTIVE") {
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
	    function disableBPMNAddSVG(pars, simple) {

		    var svg = pars.imageText.substring(pars.imageText.indexOf("<svg"))
		    if ($("#svgContainer").length > 0)
			    $("#svgContainer").remove();
		    $("#js-canvas").append(
		    "<span id=\"svgContainer\">" + svg + "</span>");
		    /* added code for height width viewBox */
		    $("#svgContainer svg").removeAttr("height");
		    $("#svgContainer svg").removeAttr('viewBox');
		    $("#svgContainer svg").removeAttr('width');
		    $("#svgContainer svg").attr('width', '100%');
		    $("#svgContainer svg").attr('height', '100%');
		    $("#svgContainer").click(
		    function(event) {

			    // console.log($(event.target).parent().html())
			    // console.log($($(event.target).parent()).attr("data-element-id"))
			    var name = $($(event.target).parent()).attr("data-element-id")
			    lastElementSelected = $($(event.target).parent()).attr(
			    "data-element-id")
			    checkIfElementType(name, simple)
		    });
	    }
	    this.enableDisableMenuOptions = function(pars) {

		    var permittedActionCd = pars.permittedActionCd;
		    // dropdown options - always true
		    document.getElementById('Open CL').classList.remove('ThisLink');
		    if (readMOnly) {
			    // enable model options
			    document.getElementById('Properties CL').classList
			    .remove('ThisLink');
			    document.getElementById('Close Model').classList
			    .remove('ThisLink');
			    // disable models options
			    document.getElementById('Save CL').classList.add('ThisLink');
			    document.getElementById('Revert Model Changes').classList
			    .add('ThisLink');
		    } else {
			    // enable menu options
			    document.getElementById('Save CL').classList.remove('ThisLink');
			    document.getElementById('Properties CL').classList
			    .remove('ThisLink');
			    document.getElementById('Revert Model Changes').classList
			    .remove('ThisLink');
			    document.getElementById('Close Model').classList
			    .remove('ThisLink');
			    document.getElementById('Validation Test').classList
			    .remove('ThisLink');
			    document.getElementById('Refresh Status').classList
			    .remove('ThisLink');
			    document.getElementById('Refresh ASDC').classList
			    .remove('ThisLink');
		    }
		    // enable/disable menu options based on permittedActionCd
		    // list
		    this.checkPermittedActionCd(permittedActionCd, 'Validation Test',
		    'TEST');
		    this.checkPermittedActionCd(permittedActionCd, 'Submit', 'SUBMIT');
		    this.checkPermittedActionCd(permittedActionCd, 'Resubmit',
		    'RESUBMIT');
		    this.checkPermittedActionCd(permittedActionCd, 'Update', 'UPDATE');
		    this.checkPermittedActionCd(permittedActionCd, 'Stop', 'STOP');
		    this
		    .checkPermittedActionCd(permittedActionCd, 'Restart', 'RESTART');
		    this.checkPermittedActionCd(permittedActionCd, 'Delete', 'DELETE');
		    this.checkPermittedActionCd(permittedActionCd, 'Deploy', 'DEPLOY');
		    this.checkPermittedActionCd(permittedActionCd, 'UnDeploy',
		    'UNDEPLOY');
	    }
	    this.getASDCServices().then(function(pars) {

		    var obj = JSON.parse(pars);
		    var services = obj.service;
		    asdc_Services = services
	    });
    } ]);
