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

app.service('cldsModelService', ['alertService','$http', '$q', function (alertService,$http, $q) {
	console.log("/////////////cldsModelService");
	function checkIfElementType(name){

		//This will open the methods located in the app.js
		if(name.toLowerCase().indexOf("stringmatch")>=0)
			StringMatchWindow();
		else if(name.toLowerCase().indexOf("tca")>=0)
			TCAWindow();
		else if(name.toLowerCase().indexOf("collector")>=0)
			CollectorsWindow();
		else if(name.toLowerCase().indexOf("policy")>=0)
			PolicyWindow();
		
	}
	this.getASDCServices = function(){
    	
    	console.log("getASDCServices");
    	var def = $q.defer();
    	var sets = [];
    	
    	var svcUrl = "/restservices/clds/v1/clds/asdc/services/";                
    	
        $http.get(svcUrl)
        .success(function(data){        	
        	def.resolve(data);   
        	
        	
        })
        .error(function(data){       	 	      
       	 	def.reject("Asdc Services not found");
       	 	
        });
        
        return def.promise;
    };
    
    this.getRunningInstances=function(resouseInput){
      console.log("getRunningInstances");
    	var def = $q.defer();
    	var sets = [];
    	
    	//Look in scripts/common_variables.html to get utmModel name
    	
    // var svcUrl = "/restservices/clds/v1/clds/model/roncl003";                
   	var svcUrl = "/restservices/clds/v1/clds/model/" + resouseInput;                
   	
       $http.get(svcUrl)
       .success(function(data){        	
       	def.resolve(data);   
       	
       	
       })
       .error(function(data){       	 	      
      	 	def.reject("Asdc Services not found");
      	 	
       });
        
       return def.promise;
    	
    	// return [{"name":"asbg0003vm001","location":"SNANTXCA","status":"Running","view":"KPI"},{"name":"asbg0003vm002","location":"SNANTXCA","status":"Running","view":"KPI"},{"name":"asbg0003vm003","location":"SNANTXCA","status":"Running","view":"KPI"},{"name":"asbg0003vm004","location":"SNANTXCA","status":"Stopped","view":"KPI"}]
    }
    
this.getASDCService = function(uuid){
    	console.log("getASDCService");
    	
    	var def = $q.defer();
    	var sets = [];
    	
    	var svcUrl = "/restservices/clds/v1/clds/asdc/services/" + uuid;                
    	
        $http.get(svcUrl)
        .success(function(data){   
        	def.resolve(data);         	
        	
        })
        .error(function(data){       	 	      
       	 	def.reject("ASDC service not found");
        });
        
        return def.promise;
    };
    this.getModel = function(modelName){
    	
    	console.log("getModel");
    	var def = $q.defer();
    	var sets = [];
    	
    	var svcUrl = "/restservices/clds/v1/clds/model/" + modelName;                
    	
        $http.get(svcUrl)
        .success(function(data){        	
        	def.resolve(data);         	
        	
        })
        .error(function(data){       	 	      
       	 	def.reject("Open Model not successful");
        });
        
        return def.promise;
    };
    this.getSavedModel=function(){
      console.log("getSavedModel");
    	var def = $q.defer();
    	var sets = [];
    	
    	var svcUrl = "/restservices/clds/v1/clds/model-names";                
    	
        $http.get(svcUrl)
        .success(function(data){        	
        	def.resolve(data);         	
        	
        })
        .error(function(data){       	 	      
       	 	def.reject("Open Model not successful");
        });
        
        return def.promise;
    };
    this.setModel = function(modelName, controlNamePrefixIn, bpmnTextIn, propTextIn){
    	
    	console.log("setModel");
    	var def = $q.defer();
    	var sets = [];

    	var svcUrl = "/restservices/clds/v1/clds/model/" + modelName;        
        var svcRequest = {name: modelName, controlNamePrefix: controlNamePrefixIn, bpmnText: bpmnTextIn, propText: propTextIn};
        
    	
        $http.put(svcUrl, svcRequest)
        .success(function(data){        	
        	def.resolve(data);         	
        	
        })
        .error(function(data){       	 	      
       	 	def.reject("Save Model not successful");
        });
        
        return def.promise;
    };
    this.processAction = function(uiAction, modelName, controlNamePrefixIn, bpmnTextIn, propTextIn,svgXmlIn,templateName){
    	
    	console.log("processAction");
    	var def = $q.defer();
    	var sets = [];
    	console.log("Generated SVG xml File...");
    	//console.log(svgXmlIn);
    	var svcUrl = "/restservices/clds/v1/clds/";        
    	var svcAction = uiAction.toLowerCase();
    	if ( svcAction == "save" || svcAction == "refresh" ) {
    		svcUrl = svcUrl + "model/" + modelName;
    	} else if ( svcAction == "test" ) {
     		svcUrl = svcUrl + "action/submit/" + modelName + "?test=true";
    	} else {
      		svcUrl = svcUrl + "action/" + svcAction + "/" + modelName;
      	}
    	
        var svcRequest = {name: modelName, controlNamePrefix: controlNamePrefixIn, bpmnText: bpmnTextIn, propText: propTextIn, imageText:svgXmlIn, templateName:templateName};      
    	console.log(svcRequest)
        $http.put(svcUrl, svcRequest)
        .success(function(data){
        	def.resolve(data);   
          alertService.alertMessage("Action Successful:"+uiAction,1)      	
        	
        })
        .error(function(data){  
        	alertService.alertMessage("Action Failure:"+uiAction,2);
       	 	//def        	alertService.alertMessage("Action Successful:"+uiAction,1);
       	 	def.reject(svcAction + " not successful");
        });
        
        return def.promise;
    };
    this.checkPermittedActionCd = function(permittedActionCd, menuText, actionCd){
      console.log("checkPermittedActionCd");
       	if ( permittedActionCd.indexOf(actionCd) > -1 ) {
       		document.getElementById(menuText).classList.remove('ThisLink');
       	} else {
       		document.getElementById(menuText).classList.add('ThisLink');
       	}
    };        
    this.processActionResponse = function(modelName, pars){
      console.log("processActionResponse");
    	// populate control name (prefix and uuid here)
       	var controlNamePrefix = pars.controlNamePrefix;
       	var controlNameUuid = pars.controlNameUuid;
       	
       
        
        var headerText = "Closed Loop Modeler - " + modelName;
        if ( controlNameUuid != null ) {
        	var actionCd = pars.event.actionCd;
        	var actionStateCd = pars.event.actionStateCd;
        	//headerText = headerText + " [" + controlNamePrefix + controlNameUuid + "] [" + actionCd + ":" + actionStateCd + "]";
        	headerText = headerText + " [" + controlNamePrefix + controlNameUuid + "]";
        	console.log("MINA PARS TEST " + headerText + " ----- " + controlNamePrefix + " ----- " + controlNameUuid + "  ----- " + pars);
        }
        	
    	document.getElementById("modeler_name").textContent = headerText;
    	setStatus(pars)
    	disableBPMNAddSVG(pars);
        this.enableDisableMenuOptions(pars);
		
       	
    };
    
    this.processRefresh=function(pars){
      console.log("processRefresh");
    	setStatus(pars);
    }
    function setStatus(pars){
      console.log("setStatus");
    	var status = pars.status;
    	// apply color to status
		var statusColor='white';
       	if(status.trim()==="DESIGN"){
       		statusColor='gray'
       	} else if(status.trim()==="DISTRIBUTED"){
       		statusColor='blue'
       	} else if(status.trim()==="ACTIVE"){
       		statusColor='green'
       	} else if(status.trim()==="STOPPED"){
       		statusColor='red'
       	} else if(status.trim()==="DELETING"){
       		statusColor='pink'
       	} else if(status.trim()==="ERROR"){
       		statusColor='orange'
       	} else if(status.trim()==="UNKNOWN"){
       		statusColor='blue'
       	} else{
       		statusColor=null;
       	}
       	

       	var statusMsg='<span style="background-color:' + statusColor + ';-moz-border-radius: 50px;	-webkit-border-radius: 50px;	border-radius: 50px;">&nbsp;&nbsp;&nbsp;'+status+'&nbsp;&nbsp;&nbsp;</span>';
       	// display status
       	if ($("#status_clds").length>=1)
       		$("#status_clds").remove();
       	$("#activity_modeler").append('<span id="status_clds" style="position: absolute;  left: 61%;top: 51px; font-size:20px;">Status: '+statusMsg+'</span>');

       	
    }
    function disableBPMNAddSVG(pars){
      console.log("disableBPMNAddSVG");
    	var svg=pars.imageText.substring(pars.imageText.indexOf("<svg"))
    	if($("#svgContainer").length>0)
       		$("#svgContainer").remove();
       	$("#js-canvas").append("<span id=\"svgContainer\">"+svg+"</span>");
        /* added code for height width viewBox */
        $("#svgContainer svg").removeAttr("height");
        $("#svgContainer svg").removeAttr('viewBox');
        $("#svgContainer svg").removeAttr('width');
        
        $("#svgContainer svg").attr('width','100%');
        $("#svgContainer svg").attr('height','100%');

       	$("#svgContainer").click(function(event){
       		console.log($(event.target).parent().html())
       		console.log($($(event.target).parent()).attr("data-element-id"))
       		var name=$($(event.target).parent()).attr("data-element-id")
       		lastElementSelected=$($(event.target).parent()).attr("data-element-id")
       		checkIfElementType(name)
       		
       	})
        $(".bjs-container").attr("hidden","");
    }
    this.enableDisableMenuOptions=function(pars){
      console.log("enableDisableMenuOptions");
    	var permittedActionCd = pars.permittedActionCd;
    	// enable menu options
    	document.getElementById('Save CL').classList.remove('ThisLink');
    	document.getElementById('Test').classList.remove('ThisLink');
    	document.getElementById('Properties CL').classList.remove('ThisLink');
    	document.getElementById('Refresh Status').classList.remove('ThisLink');
    	document.getElementById('Revert Model Changes').classList.remove('ThisLink');
    	document.getElementById('Close Model').classList.remove('ThisLink');
    	document.getElementById('Refresh ASDC').classList.remove('ThisLink');
    	document.getElementById('Running Instances').classList.remove('ThisLink');
    	
    	//disable template options for save/properties
    	document.getElementById('Save Template').classList.add('ThisLink');
    	document.getElementById('Template Properties').classList.add('ThisLink');
    	document.getElementById('Revert Template Changes').classList.add('ThisLink');
    	document.getElementById('Close Template').classList.add('ThisLink');

    	
       	// enable/disable menu options based on permittedActionCd list
       	this.checkPermittedActionCd(permittedActionCd, 'Submit', 'SUBMIT');
       	this.checkPermittedActionCd(permittedActionCd, 'Resubmit', 'RESUBMIT');
       	this.checkPermittedActionCd(permittedActionCd, 'Update', 'UPDATE');
       	this.checkPermittedActionCd(permittedActionCd, 'Delete', 'DELETE');
       	this.checkPermittedActionCd(permittedActionCd, 'Stop', 'STOP');
       	this.checkPermittedActionCd(permittedActionCd, 'Restart', 'RESTART');
    }
    
   
    this.getASDCServices().then(function(pars){
      console.log("getASDCServices");
    	var services=pars.service;
    	asdc_Services=services
    });
    
 }]);
