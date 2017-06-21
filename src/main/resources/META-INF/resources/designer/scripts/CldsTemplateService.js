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

app.service('cldsTemplateService', ['alertService', '$http', '$q', function (alertService, $http, $q) {
    this.getTemplate = function(templateName){
    	
    	console.log("///////////////cldsTemplateService");
    	var def = $q.defer();
    	var sets = [];
    	
    	var svcUrl = "/restservices/clds/v1/cldsTempate/template/" + templateName;                
    	
        $http.get(svcUrl)
        .success(function(data){ 
        console.log("success");       	
        	def.resolve(data);         	
        	
        })
        .error(function(data){  
        console.log("error");     	 	      
       	 	def.reject("Open Model not successful");
        });
        
        return def.promise;
    };
    this.getSavedTemplate=function(){
        console.log("getSavedTemplate");
    	var def = $q.defer();
    	var sets = [];
    	
    	var svcUrl = "/restservices/clds/v1/cldsTempate/template-names";                
    	
        $http.get(svcUrl)
        .success(function(data){
        console.log("success");        	
        	def.resolve(data);         	
        	
        })
        .error(function(data){
        console.log("error");       	 	      
       	 	def.reject("Open Model not successful");
        });
        
        return def.promise;
    };
    this.setTemplate = function(templateName, controlNamePrefixIn, bpmnTextIn, propTextIn){
    	
    	console.log("setTemplate");
    	var def = $q.defer();
    	var sets = [];

    	var svcUrl = "/restservices/clds/v1/clds/model/" + modelName;        
        var svcRequest = {name: templateName, controlNamePrefix: controlNamePrefixIn, bpmnText: bpmnTextIn, propText: propTextIn};
        
    	
        $http.put(svcUrl, svcRequest)
        .success(function(data){ 
        console.log("success");       	
        	def.resolve(data);         	
        	
        })
        .error(function(data){ 
        console.log("error");      	 	      
       	 	def.reject("Save Model not successful");
        });
        
        return def.promise;
    };
    this.processAction = function(uiAction, templateName, controlNamePrefixIn, bpmnTextIn, propTextIn,svgXmlIn){
    	
    	console.log("processAction");
    	var def = $q.defer();
    	var sets = [];
    	console.log("Generated SVG xml File...");
    	console.log(propTextIn);
    	var svcUrl = "/restservices/clds/v1/cldsTempate/template/"+templateName;        
    		
        var svcRequest = {name: templateName, controlNamePrefix: controlNamePrefixIn, bpmnText: bpmnTextIn, propText: propTextIn, imageText:svgXmlIn};      
        $http.put(svcUrl, svcRequest)
        .success(function(data){
        console.log("success");        	
        	def.resolve(data);         	
        	alertService.alertMessage("Action Successful:"+uiAction,1)

        })
        .error(function(data){
        console.log("error");       	 	      
       	 	def.reject(" not successful");
        	alertService.alertMessage("Action Failure:"+uiAction,2)

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
    this.processActionResponse = function(templateName, pars){
        console.log("processActionResponse");
    	// populate control name (prefix and uuid here)
       	var controlNamePrefix = pars.controlNamePrefix;
       	var controlNameUuid = pars.controlNameUuid;
        
        var headerText = "Closed Loop Modeler - " + templateName;
        if ( controlNameUuid != null ) {
        	var actionCd = pars.event.actionCd;
        	var actionStateCd = pars.event.actionStateCd;
//        	headerText = headerText + " [" + controlNamePrefix + controlNameUuid + "] [" + actionCd + ":" + actionStateCd + "]";
        	headerText = headerText + " [" + controlNamePrefix + controlNameUuid + "]";
        }
        	
    	document.getElementById("modeler_name").textContent = headerText;

       	
       	// enable menu options
       	document.getElementById('Save Template').classList.remove('ThisLink');
    	document.getElementById('Template Properties').classList.remove('ThisLink');
    	document.getElementById('Test').classList.remove('ThisLink');
        if(!pars.newTemplate){
            document.getElementById('Revert Template Changes').classList.remove('ThisLink');    
        }    	
    	document.getElementById('Close Template').classList.remove('ThisLink');
    	
    	document.getElementById('Refresh Status').classList.remove('ThisLink');
    	//disable save/properties for model
    	document.getElementById('Save CL').classList.add('ThisLink');
    	document.getElementById('Properties CL').classList.add('ThisLink');
    	document.getElementById('Revert Model Changes').classList.add('ThisLink');
    	document.getElementById('Close Model').classList.add('ThisLink');
    	document.getElementById('Refresh ASDC').classList.add('ThisLink');
    	document.getElementById('Running Instances').classList.add('ThisLink');

       	
    };        
 }]);
