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
	  		  }else if (name.toLowerCase().indexOf("policy") >= 0){
	  				  PolicyWindow();
	  		  } else {
	  			  $rootScope.selectedBoxName = name.toLowerCase();
	  			  ToscaModelWindow();
	  		  }
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
	    this.getModel = function(modelName) {
		    var def = $q.defer();
		    var sets = [];
		    var svcUrl = "/restservices/clds/v1/clds/model/" + modelName;
		    $http.get(svcUrl).success(function(data) {
		    	var clModel = '{"name": "ClosedLoopTest","lastComputedState":"DESIGN","svgRepresentation": "representation","globalPropertiesJson": [{"name":"service","value":["4cc5b45a-1f63-4194-8100-cd8e14248c92"]},{"name":"vf","value":["07e266fc-49ab-4cd7-8378-ca4676f1b9ec"]},{"name":"actionSet","value":["vnfRecipe"]},{"name":"location","value":["DC1"]},{"name":"deployParameters","value":{"location_id":"","service_id":"","policy_id":"AUTO_GENERATED_POLICY_ID_AT_SUBMIT"}}], "blueprint": "yaml","lastComputedState": "DESIGN","operationalPolicies": [ {"name": "OpPolicyTest", "configurationsJson": { "policy1": [{"name": "pname","value": "policy1"},{"name": "pid","value": "0"},{"name": "timeout","value": "345"},{"policyConfigurations": [[{"name": "recipe","value": ["restart"]},{"name": "maxRetries","value": ["3"]},{"name": "retryTimeLimit","value": ["180"]},{"name": "_id","value": ["6TtHGPq"]},{"name": "parentPolicy","value": [""]},{"name": "actor","value": ["APPC"]},{"name": "recipeInput","value": [""]},{"name": "recipeLevel","value": ["VM"]},{"name": "targetResourceId","value": ["07e266fc-49ab-4cd7-8378-ca4676f1b9ec"]},{"name": "targetResourceIdOther","value": [""]},{"name": "enableGuardPolicy","value": ["on"]},{"name": "guardPolicyType","value": ["GUARD_YAML"]},{"name": "guardTargets","value": [".*"]},{"name": "minGuard","value": ["1"]},{"name": "maxGuard","value": ["1"]},{"name": "limitGuard","value": ["1"]},{"name": "timeUnitsGuard","value": ["minute"]},{"name": "timeWindowGuard","value": ["10"]},{"name": "guardActiveStart","value": ["00:00:01-05:00"]},{"name": "guardActiveEnd","value": ["00:00:00-05:00"]}]]}]} }],"microServicePolicies": [{"name": "tca","properties": "", "shared": true,"policyTosca": "tosca","jsonRepresentation": {"schema":{"title":"DCAE TCA Config","type":"object","required":["name","eventName"],"properties":{"name":{"propertyOrder":101,"title":"Name","type":"string","default":"New_Set"},"eventName":{"propertyOrder":102,"title":"EventName","type":"string","enum":["event1","event2"]},"clSchemaType":{"propertyOrder":103,"title":"Control Loop Schema Type","type":"string","enum":["","type1","type2"]},"threshold":{"propertyOrder":104,"title":"Threshold","format":"tabs","type":"array","items":{"type":"object","title":"Threshold","required":["metric","operator"],"properties":{"metric":{"propertyOrder":1001,"title":"Metric","type":"string","enum":["metric1","metric2"]},"operator":{"propertyOrder":1003,"default":">","title":"Operator","type":"string","enum":[">","<","=","<=",">="]}, "clEventStatus":{"propertyOrder":1004,"title":"Closed Loop Event Status","type":"string","enum":["","ONSET","ABATED"]}}}}}}}}],"loopLogs": [{ } ] }';
		    	cl_props = JSON.parse(clModel);
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
	    this.processActionResponse = function(modelName, pars) {

		    // populate control name (prefix and uuid here)
		    var headerText = "Closed Loop Modeler - " + modelName;
		    setStatus();
		    disableBPMNAddSVG(pars);
		    enableDisableMenuOptions();
	    };
	    this.processRefresh = function(pars) {

		    var newPars = pars;
		    if (typeof pars.body !== 'undefined') {
			    newPars = pars.body;
		    }
		    typeID = newPars.typeId;
		    deploymentId = newPars.deploymentId;
		    setStatus(newPars);
		    enableDisableMenuOptions();
	    }
	    function setStatus() {

		    var status = getStatus();
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
	    function disableBPMNAddSVG(pars) {

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
			    checkIfElementType(name)
		    });
	    }
	    enableDisableMenuOptions = function() {
	    	//var status = getStatus();
	    	var status = "DESIGN";
		    // dropdown options - always true
	    	enableDefaultMenu();
		    // enable/disable menu options based on permittedActionCd
		    // list
	    	enableActionMenu(status);
	    }
    } ]);
