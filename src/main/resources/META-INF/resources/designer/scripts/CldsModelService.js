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
	  			  $rootScope.selectedBoxName = name;
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
		    var svcUrl = "/restservices/clds/v2/loop/" + modelName;
		    //var svcUrl = "/restservices/clds/v1/clds/model/" + modelName;
		    $http.get(svcUrl).success(function(data) {
		    	//var clModel = '{"name": "ClosedLoopTest","lastComputedState":"DESIGN","svgRepresentation": "representation","globalPropertiesJson": [{"name":"service","value":["4cc5b45a-1f63-4194-8100-cd8e14248c92"]},{"name":"vf","value":["07e266fc-49ab-4cd7-8378-ca4676f1b9ec"]},{"name":"actionSet","value":["vnfRecipe"]},{"name":"location","value":["DC1"]},{"name":"deployParameters","value":{"location_id":"","service_id":"","policy_id":"AUTO_GENERATED_POLICY_ID_AT_SUBMIT"}}], "blueprint": "yaml","lastComputedState": "DESIGN","operationalPolicies": [ {"name": "OpPolicyTest", "configurationsJson": { "policy1": [{"name": "pname","value": "policy1"},{"name": "pid","value": "0"},{"name": "timeout","value": "345"},{"policyConfigurations": [[{"name": "recipe","value": ["restart"]},{"name": "maxRetries","value": ["3"]},{"name": "retryTimeLimit","value": ["180"]},{"name": "_id","value": ["6TtHGPq"]},{"name": "parentPolicy","value": [""]},{"name": "actor","value": ["APPC"]},{"name": "recipeInput","value": [""]},{"name": "recipeLevel","value": ["VM"]},{"name": "targetResourceId","value": ["07e266fc-49ab-4cd7-8378-ca4676f1b9ec"]},{"name": "targetResourceIdOther","value": [""]},{"name": "enableGuardPolicy","value": ["on"]},{"name": "guardPolicyType","value": ["GUARD_YAML"]},{"name": "guardTargets","value": [".*"]},{"name": "minGuard","value": ["1"]},{"name": "maxGuard","value": ["1"]},{"name": "limitGuard","value": ["1"]},{"name": "timeUnitsGuard","value": ["minute"]},{"name": "timeWindowGuard","value": ["10"]},{"name": "guardActiveStart","value": ["00:00:01-05:00"]},{"name": "guardActiveEnd","value": ["00:00:00-05:00"]}]]}]} }],"microServicePolicies": [{"name": "tca","properties": "", "shared": true,"policyTosca": "tosca","jsonRepresentation": {"schema":{"title":"DCAE TCA Config","type":"object","required":["name","eventName"],"properties":{"name":{"propertyOrder":101,"title":"Name","type":"string","default":"New_Set"},"eventName":{"propertyOrder":102,"title":"EventName","type":"string","enum":["event1","event2"]},"clSchemaType":{"propertyOrder":103,"title":"Control Loop Schema Type","type":"string","enum":["","type1","type2"]},"threshold":{"propertyOrder":104,"title":"Threshold","format":"tabs","type":"array","items":{"type":"object","title":"Threshold","required":["metric","operator"],"properties":{"metric":{"propertyOrder":1001,"title":"Metric","type":"string","enum":["metric1","metric2"]},"operator":{"propertyOrder":1003,"default":">","title":"Operator","type":"string","enum":[">","<","=","<=",">="]}, "clEventStatus":{"propertyOrder":1004,"title":"Closed Loop Event Status","type":"string","enum":["","ONSET","ABATED"]}}}}}}}}],"loopLogs": [{ } ] }';
		    	//cl_props = JSON.parse(clModel);
		    	cl_props = data;
			    def.resolve(data);
		    }).error(function(data) {

			    def.reject("Open Model not successful");
		    });
		    return def.promise;
	    };
	    this.getSavedModel = function() {

		    var def = $q.defer();
		    var sets = [];
		    //var svcUrl = "/restservices/clds/v1/clds/model-names";
		    var svcUrl = "/restservices/clds/v2/loop/getAllNames";
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
	    this.processActionResponse = function(modelName) {

		    // populate control name (prefix and uuid here)
		    var headerText = "Closed Loop Modeler - " + modelName;
		    setStatus();
		    manageCLImage(modelName);
		    enableDisableMenuOptions();
	    };
	    this.processRefresh = function(pars) {

		    var newPars = pars;
		    if (typeof pars.body !== 'undefined') {
			    newPars = pars.body;
		    }
		    typeID = newPars.typeId;
		    deploymentId = newPars.deploymentId;
		    setStatus();
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
	    function manageCLImage(modelName) {
	    	getModelImage(modelName).then(function(pars) {
		    	//var svg='<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/2000/svg"> <defs> <marker viewBox="0 0 20 20" markerWidth="10" markerHeight="10" orient="auto" refX="11" refY="10" id="markerSjdype4kmnw"> <path d="M 1 5 L 11 10 L 1 15 Z" style="stroke-width: 1; stroke-linecap: round; stroke-dasharray: 10000, 1;" fill="#000000" /> </marker> <marker viewBox="0 0 20 20" markerWidth="20" markerHeight="20" orient="auto" refX="6" refY="6" id="markerSjdype4kmny"> <circle cx="6" cy="6" r="3.5" style="stroke-width: 1; stroke-linecap: round; stroke-dasharray: 10000, 1;" fill="#ffffff" stroke="#000000" /> </marker> <marker viewBox="0 0 20 20" markerWidth="20" markerHeight="20" orient="auto" refX="8.5" refY="5" id="markerSjdype4kmo0"> <path d="m 1 5 l 0 -3 l 7 3 l -7 3 z" style="stroke-width: 1; stroke-linecap: butt; stroke-dasharray: 10000, 1;" fill="#ffffff" stroke="#000000" /> </marker> <marker viewBox="0 0 20 20" markerWidth="10" markerHeight="10" orient="auto" refX="11" refY="10" id="markerSjdype4kmo2"> <path d="M 1 5 L 11 10 L 1 15" style="stroke-width: 1; stroke-linecap: round; stroke-dasharray: 10000, 1;" fill="#ffffff" stroke="#000000" /> </marker> <marker viewBox="0 0 20 20" markerWidth="10" markerHeight="10" orient="auto" refX="-1" refY="10" id="markerSjdype4kmo4"> <path d="M 0 10 L 8 6 L 16 10 L 8 14 Z" style="stroke-width: 1; stroke-linecap: round; stroke-dasharray: 10000, 1;" fill="#ffffff" stroke="#000000" /> </marker> <marker viewBox="0 0 20 20" markerWidth="10" markerHeight="10" orient="auto" refX="-5" refY="10" id="markerSjdype4kmo6"> <path d="M 1 4 L 5 16" style="stroke-width: 1; stroke-linecap: round; stroke-dasharray: 10000, 1;" fill="#000000" stroke="#000000" /> </marker> </defs><g fill-opacity="1" color-rendering="auto" color-interpolation="auto" text-rendering="auto" stroke="black" stroke-linecap="square" stroke-miterlimit="10" shape-rendering="auto" stroke-opacity="1" fill="black" stroke-dasharray="none" font-weight="normal" stroke-width="1" font-family="&apos;Dialog&apos;" font-style="normal" data-element-id="start-circle" stroke-linejoin="miter" font-size="12px" image-rendering="auto" stroke-dashoffset="0"><!--Generated by the Batik Graphics2D SVG Generator--> <defs id="genericDefs"/> <g> <g shape-rendering="geometricPrecision" text-rendering="optimizeQuality" stroke-width="2"> <circle fill="none" r="17" cx="34" cy="41"/> </g> </g> </g> <g fill-opacity="1" color-rendering="auto" color-interpolation="auto" text-rendering="auto" stroke="black" stroke-linecap="square" stroke-miterlimit="10" shape-rendering="auto" stroke-opacity="1" fill="black" stroke-dasharray="none" font-weight="normal" stroke-width="1" font-family="&apos;Dialog&apos;" font-style="normal" data-element-id="Arrow_d0b22360-770d-4b05-b81c-cf148b121341" stroke-linejoin="miter" font-size="12px" image-rendering="auto" stroke-dashoffset="0"><!--Generated by the Batik Graphics2D SVG Generator--> <defs id="genericDefs"/> <g> <g shape-rendering="geometricPrecision" text-rendering="optimizeQuality" stroke-width="2"> <line y2="41" fill="none" x1="51" x2="170" y1="41"/> <polygon fill="none" points=" 166 38 166 44 172 41"/>             <polygon points=" 166 38 166 44 172 41" stroke="none"/> </g> </g> </g> <g fill-opacity="1" color-rendering="auto" color-interpolation="auto" text-rendering="auto" stroke="black" stroke-linecap="square" stroke-miterlimit="10" shape-rendering="auto" stroke-opacity="1" fill="black" stroke-dasharray="none" font-weight="normal" stroke-width="1" font-family="&apos;Dialog&apos;" font-style="normal" data-element-id="VES" stroke-linejoin="miter" font-size="12px" image-rendering="auto" stroke-dashoffset="0"><!--Generated by the Batik Graphics2D SVG Generator--> <defs id="genericDefs"/> <g> <g shape-rendering="geometricPrecision" text-rendering="optimizeQuality" stroke-width="2"> <rect fill="none" x="174" width="123" y="1" height="82"/> </g> <g fill-opacity="0" fill="rgb(0,0,0)" text-rendering="optimizeQuality" shape-rendering="geometricPrecision" stroke="rgb(0,0,0)" stroke-opacity="0" stroke-width="2"> <rect x="174" width="123" y="1" height="82" stroke="none"/> </g> <g font-family="sans-serif" shape-rendering="geometricPrecision" text-rendering="optimizeQuality"> <text x="223" xml:space="preserve" y="42" stroke="none">VES</text> <line y2="83" fill="none" stroke-width="2" x1="194" x2="194" y1="1"/> </g> </g> </g> <g fill-opacity="1" color-rendering="auto" color-interpolation="auto" text-rendering="auto" stroke="black" stroke-linecap="square" stroke-miterlimit="10" shape-rendering="auto" stroke-opacity="1" fill="black" stroke-dasharray="none" font-weight="normal" stroke-width="1" font-family="&apos;Dialog&apos;" font-style="normal" data-element-id="Arrow_d4351927-d77a-4e04-82d1-cd8f2b008137" stroke-linejoin="miter" font-size="12px" image-rendering="auto" stroke-dashoffset="0"><!--Generated by the Batik Graphics2D SVG Generator--> <defs id="genericDefs"/> <g> <g text-rendering="optimizeQuality" stroke-width="2" shape-rendering="geometricPrecision" font-family="sans-serif"> <line y2="41" fill="none" x1="297" x2="416" y1="41"/> <polygon fill="none" points=" 412 38 412 44 418 41"/> <polygon points=" 412 38 412 44 418 41" stroke="none"/> </g> </g> </g>    <g fill-opacity="1" color-rendering="auto" color-interpolation="auto" text-rendering="auto" stroke="black" stroke-linecap="square" stroke-miterlimit="10" shape-rendering="auto" stroke-opacity="1" fill="black" stroke-dasharray="none" font-weight="normal" stroke-width="1" font-family="&apos;Dialog&apos;" font-style="normal" data-element-id="TCA" stroke-linejoin="miter" font-size="12px" image-rendering="auto" stroke-dashoffset="0"><!--Generated by the Batik Graphics2D SVG Generator--> <defs id="genericDefs"/> <g> <g text-rendering="optimizeQuality" stroke-width="2" shape-rendering="geometricPrecision" font-family="sans-serif"> <rect fill="none" x="420" width="123" y="1" height="82"/> </g> <g fill-opacity="0" fill="rgb(0,0,0)" text-rendering="optimizeQuality" font-family="sans-serif" shape-rendering="geometricPrecision" stroke="rgb(0,0,0)" stroke-opacity="0" stroke-width="2"> <rect x="420" width="123" y="1" height="82" stroke="none"/> </g> <g font-family="sans-serif" shape-rendering="geometricPrecision" text-rendering="optimizeQuality"> <text x="470" xml:space="preserve" y="42" stroke="none">TCA</text> <line y2="62" fill="none" stroke-width="2" x1="420" x2="543" y1="62"/> </g> </g> </g>   <g fill-opacity="1" color-rendering="auto" color-interpolation="auto" text-rendering="auto" stroke="black" stroke-linecap="square" stroke-miterlimit="10" shape-rendering="auto" stroke-opacity="1" fill="black" stroke-dasharray="none" font-weight="normal" stroke-width="1" font-family="&apos;Dialog&apos;" font-style="normal" data-element-id="Arrow_5ae2966f-f971-4c3b-8237-efa551beb531" stroke-linejoin="miter" font-size="12px" image-rendering="auto" stroke-dashoffset="0"><!--Generated by the Batik Graphics2D SVG Generator--> <defs id="genericDefs"/> <g> <g text-rendering="optimizeQuality" stroke-width="2" shape-rendering="geometricPrecision" font-family="sans-serif"> <line y2="41" fill="none" x1="543" x2="662" y1="41"/> <polygon fill="none" points=" 658 38 658 44 664 41"/> <polygon points=" 658 38 658 44 664 41" stroke="none"/> </g> </g> </g> <g fill-opacity="1" color-rendering="auto" color-interpolation="auto" text-rendering="auto" stroke="black" stroke-linecap="square" stroke-miterlimit="10" shape-rendering="auto" stroke-opacity="1" fill="black" stroke-dasharray="none" font-weight="normal" stroke-width="1" font-family="&apos;Dialog&apos;" font-style="normal" data-element-id="Policy" stroke-linejoin="miter" font-size="12px" image-rendering="auto" stroke-dashoffset="0"><!--Generated by the Batik Graphics2D SVG Generator--> <defs id="genericDefs"/> <g> <g text-rendering="optimizeQuality" stroke-width="2" shape-rendering="geometricPrecision" font-family="sans-serif"> <rect fill="none" x="666" width="123" y="1" height="82"/> </g> <g fill-opacity="0" fill="rgb(0,0,0)" text-rendering="optimizeQuality" font-family="sans-serif" shape-rendering="geometricPrecision" stroke="rgb(0,0,0)" stroke-opacity="0" stroke-width="2"> <rect x="666" width="123" y="1" height="82" stroke="none"/> </g> <g font-family="sans-serif" shape-rendering="geometricPrecision" text-rendering="optimizeQuality"> <text x="710" xml:space="preserve" y="42" stroke="none">Policy</text> <line y2="1" fill="none" stroke-width="2" x1="666" x2="727" y1="42"/> </g> </g> </g> <g fill-opacity="1" color-rendering="auto" color-interpolation="auto" text-rendering="auto" stroke="black" stroke-linecap="square" stroke-miterlimit="10" shape-rendering="auto" stroke-opacity="1" fill="black" stroke-dasharray="none" font-weight="normal" stroke-width="1" font-family="&apos;Dialog&apos;" font-style="normal" data-element-id="Arrow_ed5ccaed-6d8c-44e9-8245-85f66523cb44" stroke-linejoin="miter" font-size="12px" image-rendering="auto" stroke-dashoffset="0"><!--Generated by the Batik Graphics2D SVG Generator--> <defs id="genericDefs"/> <g> <g text-rendering="optimizeQuality" stroke-width="2" shape-rendering="geometricPrecision" font-family="sans-serif"> <line y2="41" fill="none" x1="789" x2="908" y1="41"/> <polygon fill="none" points=" 904 38 904 44 910 41"/> <polygon points=" 904 38 904 44 910 41" stroke="none"/> </g> </g> </g> <g fill-opacity="1" color-rendering="auto" color-interpolation="auto" text-rendering="auto" stroke="black" stroke-linecap="square" stroke-miterlimit="10" shape-rendering="auto" stroke-opacity="1" fill="black" stroke-dasharray="none" font-weight="normal" stroke-width="1" font-family="&apos;Dialog&apos;" font-style="normal" data-element-id="circle_stop" stroke-linejoin="miter" font-size="12px" image-rendering="auto" stroke-dashoffset="0"><!--Generated by the Batik Graphics2D SVG Generator--> <defs id="genericDefs"/> <g> <g text-rendering="optimizeQuality" stroke-width="4" shape-rendering="geometricPrecision" font-family="sans-serif"> <circle fill="none" r="17" cx="929" cy="41"/> </g> </g> </g> </svg>';
		    	var svg = pars;
			    if ($("#svgContainer").length > 0)
				    $("#svgContainer").remove();
			    $("#js-canvas").append(
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
		    //var status = getStatus();
	    	//enableActionMenu(status);
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
