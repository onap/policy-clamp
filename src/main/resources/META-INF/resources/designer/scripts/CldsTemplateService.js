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
    	

    	var def = $q.defer();
    	var sets = [];
    	
    	var svcUrl = "/restservices/clds/v1/cldsTempate/template/" + templateName;                
    	
        $http.get(svcUrl)
        .success(function(data){ 
  	
        	def.resolve(data);         	
        	
        })
        .error(function(data){  
 	 	      
       	 	def.reject("Open Model not successful");
        });
        
        return def.promise;
    };
    this.getSavedTemplate=function(){

    	var def = $q.defer();
    	var sets = [];
    	
    	var svcUrl = "/restservices/clds/v1/cldsTempate/template-names";                
    	
        $http.get(svcUrl)
        .success(function(data){
         	
        	def.resolve(data);         	
        	
        })
        .error(function(data){
     	 	      
       	 	def.reject("Open Model not successful");
        });
        
        return def.promise;
    };
    this.setTemplate = function(templateName, controlNamePrefixIn, bpmnTextIn, propTextIn){
    	

    	var def = $q.defer();
    	var sets = [];

    	var svcUrl = "/restservices/clds/v1/clds/model/" + modelName;        
        var svcRequest = {name: templateName, controlNamePrefix: controlNamePrefixIn, bpmnText: bpmnTextIn, propText: propTextIn};
        
    	
        $http.put(svcUrl, svcRequest)
        .success(function(data){ 
   	
        	def.resolve(data);         	
        	
        })
        .error(function(data){ 
   	 	      
       	 	def.reject("Save Model not successful");
        });
        
        return def.promise;
    };
    this.processAction = function(uiAction, templateName, controlNamePrefixIn, bpmnTextIn, propTextIn,svgXmlIn){
    	
 
    	var def = $q.defer();
    	var sets = [];
    	var svcUrl = "/restservices/clds/v1/cldsTempate/template/"+templateName;        
    		
        var svcRequest = {name: templateName, controlNamePrefix: controlNamePrefixIn, bpmnText: bpmnTextIn, propText: propTextIn, imageText:svgXmlIn};      
        $http.put(svcUrl, svcRequest)
        .success(function(data){
     	
        	def.resolve(data);         	
        	alertService.alertMessage("Action Successful:"+uiAction,1)

        })
        .error(function(data){
     	 	      
       	 	def.reject(" not successful");
        	alertService.alertMessage("Action Failure:"+uiAction,2)

        });
        
        return def.promise;
    };
    this.checkPermittedActionCd = function(permittedActionCd, menuText, actionCd){

       	if ( permittedActionCd.indexOf(actionCd) > -1 ) {
       		document.getElementById(menuText).classList.remove('ThisLink');
       	} else {
       		document.getElementById(menuText).classList.add('ThisLink');
       	}
    };        
    this.processActionResponse = function(templateName, pars){

    	// populate control name (prefix and uuid here)
       	var controlNamePrefix = pars.controlNamePrefix;
       	var controlNameUuid = pars.controlNameUuid;
        var permittedActionCd = pars.permittedActionCd;
        
        var headerText = "Closed Loop Modeler - " + templateName;
        if ( controlNameUuid != null ) {
        	var actionCd = pars.event.actionCd;
        	var actionStateCd = pars.event.actionStateCd;
//        	headerText = headerText + " [" + controlNamePrefix + controlNameUuid + "] [" + actionCd + ":" + actionStateCd + "]";
        	headerText = headerText + " [" + controlNamePrefix + controlNameUuid + "]";
        }
        	
    	document.getElementById("modeler_name").textContent = headerText;

       	//dropdown options -- always true
    	document.getElementById('Open Template').classList.remove('ThisLink');
    	document.getElementById('Open CL').classList.remove('ThisLink');
    	document.getElementById('Save CL').classList.add('ThisLink');
    	document.getElementById('Properties CL').classList.add('ThisLink');
    	document.getElementById('Revert Model Changes').classList.add('ThisLink');
    	document.getElementById('Close Model').classList.add('ThisLink');
  		
       	// enable menu options
    	if (readTOnly){
    		//enable temp options (read only, Open Template)
      		document.getElementById('Template Properties').classList.remove('ThisLink');
      		document.getElementById('Close Template').classList.remove('ThisLink');
      		
      		//disable temp menu options (read only, Open Template)
      		document.getElementById('Create Template').classList.add('ThisLink');
      		document.getElementById('Save Template').classList.add('ThisLink');
      		document.getElementById('Revert Template Changes').classList.add('ThisLink');
      		
      		//disable save/properties for model
        	document.getElementById('Refresh ASDC').classList.add('ThisLink');
    	} else {
    		//enable temp options
    		document.getElementById('Create Template').classList.remove('ThisLink');
    		document.getElementById('Save Template').classList.remove('ThisLink');
        	document.getElementById('Template Properties').classList.remove('ThisLink');
        	document.getElementById('Validation Test').classList.remove('ThisLink');
            if(!pars.newTemplate){
                document.getElementById('Revert Template Changes').classList.remove('ThisLink');    
            }    	
        	document.getElementById('Close Template').classList.remove('ThisLink');
        	
        	document.getElementById('Refresh Status').classList.remove('ThisLink');
        	
        	//disable model options
        	document.getElementById('Refresh ASDC').classList.add('ThisLink');
        	

    	}
    	if (readMOnly){
   			document.getElementById('Create CL').classList.add('ThisLink');
   		  } else {
   			document.getElementById('Create CL').classList.remove('ThisLink');
   		  }
    	
    	// enable/disable menu options based on permittedActionCd list
  		this.checkPermittedActionCd(permittedActionCd, 'Validation Test', 'TEST');
        this.checkPermittedActionCd(permittedActionCd, 'Submit', 'SUBMIT');
        this.checkPermittedActionCd(permittedActionCd, 'Resubmit', 'RESUBMIT');
        this.checkPermittedActionCd(permittedActionCd, 'Update', 'UPDATE');
        this.checkPermittedActionCd(permittedActionCd, 'Stop', 'STOP');
        this.checkPermittedActionCd(permittedActionCd, 'Restart', 'RESTART');
        this.checkPermittedActionCd(permittedActionCd, 'Delete', 'DELETE');
        this.checkPermittedActionCd(permittedActionCd, 'Deploy', 'DEPLOY');
        this.checkPermittedActionCd(permittedActionCd, 'UnDeploy', 'UNDEPLOY');

    };        
 }]);
