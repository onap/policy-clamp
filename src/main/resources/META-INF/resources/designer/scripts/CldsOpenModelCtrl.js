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

app.controller('CldsOpenModelCtrl',
	['$scope', '$rootScope', '$modalInstance','cldsModelService', '$location', 'dialogs','cldsTemplateService',
		function($scope, $rootScope, $modalInstance, cldsModelService, $location,dialogs,cldsTemplateService) {
			console.log("/////////CldsOpenModelCtrl");
			$scope.typeModel='template';
			$scope.error = {
				flag : false,
				message: ""
 			};	
			
			cldsModelService.getSavedModel().then(function(pars) {
				
				$scope.modelNamel=[]
				for(var i=0;i<pars.length;i++){
					$scope.modelNamel.push(pars[i].value);		 
				}
				setTimeout(function(){
		        console.log("setTimeout");

		     setMultiSelect(); }, 100);
				
			});

			$scope.paramsRetry = function() {
				//$("#paramsWarn").hide();
				var currentValue = $("#service").val() == null ? previous : $("#service").val();
				$("#ridinSpinners").css("display","")
				loadSharedPropertyByService(currentValue,true,callBack);
				$("#ridinSpinners").css("display","none")
			};
			 $scope.paramsCancel =function() {
				loadSharedPropertyByServiceProperties(callBack);
				$("#paramsWarnrefresh").hide();
				
			};

			function completeClose(){
					//if(flag)	{
						$scope.close();
					//}
				}
			
			function callBack(flag){
					if(flag)	{
						$scope.close();
					}
				}
			$scope.refreshASDC=function(){
				console.log("refreshASDC");
				$("#ridinSpinners").css("display","")
				var bool=loadSharedPropertyByService(undefined,true,callBack);
				$("#ridinSpinners").css("display","none");
				
				
			}
			
			cldsTemplateService.getSavedTemplate().then(function(pars) {

				
				$scope.templateNamel=[]
				for(var i=0;i<pars.length;i++){
					$scope.templateNamel.push(pars[i].value);
					
				}
				
			});
			function contains(a, obj) {
				console.log("contains");
			    var i = a&& a.length>0 ? a.length : 0;
			    while (i--) {
			       if (a[i].toLowerCase() === obj.toLowerCase()) {
			           return true;
			       }
			    }
			    return false;
			}
			$scope.checkExisting=function(){
				console.log("checkExisting");
				var name = $('#modelName').val();								
				if(contains($scope.modelNamel,name)){
					$scope.nameinUse=true;
				}else{
					$scope.nameinUse=false;
				}
				specialCharacters();
			}
			 function specialCharacters (){
			 	console.log("specialCharacters");
				$scope.spcl = false;
				if(angular.element("#modelName") && 
					angular.element("#modelName").scope().model.$error.pattern && 
					angular.element("#modelName").scope().model.$error.pattern.length>0){
					$scope.spcl =  true;
				}
			}

			$scope.setTypeModel=function(_type){
				$scope.error.flag = false;
				console.log("setTypeModel");
				$scope.typeModel=_type;
			}
			
			$scope.close = function(){
				console.log("close");
				$rootScope.isNewClosed = false;
				$modalInstance.close("closed");
			};
			$scope.createNewModelOffTemplate=function(formModel){
				console.log("createNewModelOffTemplate");
				console.log(formModel);
				reloadDefaultVariables(false)
 				var modelName = document.getElementById("modelName").value;
 				var templateName=document.getElementById("templateName").value;
				console.log("openModel: modelName=" + modelName);      
				console.log("Template: templateName=" + templateName); 
				if(!modelName){
 					$scope.error.flag =true;
 					$scope.error.message = "Please enter any closed template name for proceeding";
 				    return false;
 				}
				// init UTM items
				$scope.utmModelsArray = [];
				$scope.selectedParent = {};
				$scope.currentUTMModel = {};
				$scope.currentUTMModel.selectedParent = {};
				$rootScope.oldUTMModels =[];
				$rootScope.projectName="clds_default_project";		
				var utmModels = {};
				utmModels.name = modelName;
				utmModels.subModels = [];
				$rootScope.utmModels = utmModels;
				

				cldsTemplateService.getTemplate( templateName ).then(function(pars) {
        			console.log("openModel: pars=" + pars);
        			
        			var tempImageText=pars.imageText
        			var bpmnText=pars.bpmnText
        			pars={}
        			
        			pars.imageText=tempImageText
        			pars.status= "DESIGN";
        			pars.permittedActionCd= ["SUBMIT"];
        			cldsModelService.processActionResponse(modelName, pars);
        			
        			
        			selected_template= templateName
         			selected_model = modelName;
    				
    				// set model bpmn and open diagram
        			$rootScope.isPalette = true;
        			modelXML = bpmnText;
        			visibility_model();
        		},
        		function(data) {
        			//alert("getModel failed");
        		});
       
				$modalInstance.close("closed");
			
			}
			
			$scope.cloneModel=function(){
				console.log("cloneModel");
				reloadDefaultVariables(false)
 				var modelName = document.getElementById("modelName").value;
				var originalModel=document.getElementById("modelList").value;
				console.log("openModel: modelName=" + modelName);   
				if(!modelName){
 					$scope.error.flag =true;
 					$scope.error.message = "Please enter any name for proceeding";
 				    return false;
 				}   
				
				// init UTM items
				$scope.utmModelsArray = [];
				$scope.selectedParent = {};
				$scope.currentUTMModel = {};
				$scope.currentUTMModel.selectedParent = {};
				$rootScope.oldUTMModels =[];
				$rootScope.projectName="clds_default_project";		
				var utmModels = {};
				utmModels.name = modelName;
				utmModels.subModels = [];
				$rootScope.utmModels = utmModels;
				

				cldsModelService.getModel( originalModel ).then(function(pars) {
        			console.log("openModel: pars=" + pars);
        			
        			// process data returned
        			var bpmnText = pars.bpmnText;
        			var propText = pars.propText;
        			var status = pars.status;
        			var controlNamePrefix = pars.controlNamePrefix;
        			var controlNameUuid = pars.controlNameUuid;
        			selected_template=pars.templateName;
        			pars.status="DESIGN";
        			pars.controlNameUuid="";
        			cldsModelService.processActionResponse(modelName, pars);
        			
        			// deserialize model properties
        			if ( propText == null ) {
            			console.log("openModel: propText is null");
        			} else {
            			console.log("openModel: propText=" + propText);
        				elementMap =  JSON.parse(propText);
        			}

         			selected_model = modelName;
    				
    				// set model bpmn and open diagram
        			$rootScope.isPalette = true;
        			modelXML = bpmnText;
        			visibility_model();
        		},
        		function(data) {
        			//alert("getModel failed");
        		});
       
				$modalInstance.close("closed");
			}
			$scope.createNewModel=function(){
				console.log("createNewModel");
				reloadDefaultVariables(false)
 				var modelName = document.getElementById("modelName").value;
				console.log("openModel: modelName=" + modelName);      
				
				// BEGIN env
				// init UTM items
				$scope.utmModelsArray = [];
				$scope.selectedParent = {};
				$scope.currentUTMModel = {};
				$scope.currentUTMModel.selectedParent = {};
				$rootScope.oldUTMModels =[];
				$rootScope.projectName="clds_default_project";		
				var utmModels = {};
				utmModels.name = modelName;
				utmModels.subModels = [];
				$rootScope.utmModels = utmModels;
				
    			// enable appropriate menu options
    			var pars = {status: "DESIGN", permittedActionCd: ["SUBMIT"]};
    			
				cldsModelService.processActionResponse(modelName, pars);
    			
				selected_model = modelName;

				// set model bpmn and open diagram
    			$rootScope.isPalette = true;

    	        var initialDiagram =
    	                '<?xml version="1.0" encoding="UTF-8"?>' +
    	                '<bpmn:definitions xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" ' +
    	                'xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" ' +
    	                'xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI" ' +
    	                'xmlns:dc="http://www.omg.org/spec/DD/20100524/DC" ' +
    	                'targetNamespace="http://bpmn.io/schema/bpmn" ' +
    	                'id="Definitions_1">' +
    	                '<bpmn:process id="Process_1" isExecutable="false">' +
    	                '<bpmn:startEvent id="StartEvent_1"/>' +
    	                '</bpmn:process>' +
    	                '<bpmndi:BPMNDiagram id="BPMNDiagram_1">' +
    	                '<bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="Process_1">' +
    	                '<bpmndi:BPMNShape id="_BPMNShape_StartEvent_2" bpmnElement="StartEvent_1">' +
    	                '<dc:Bounds x="50" y="162" width="36" height="36" />' +
    	                '</bpmndi:BPMNShape>' +
    	                '</bpmndi:BPMNPlane>' +
    	                '</bpmndi:BPMNDiagram>' +
    	                '</bpmn:definitions>';
    	                
    			modelXML = initialDiagram;
    			visibility_model();
    			$modalInstance.close("closed");
			}
			$scope.revertChanges=function(){
				console.log("revertChanges");
				$scope.openModel();
			}
			$scope.openModel = function(){
				console.log("openModel");
				reloadDefaultVariables(false)
				var readonly;
				if(document.getElementById("readOnly")){
					readOnly=document.getElementById("readOnly").checked;	
				}
				
				console.log("readonly seen ")
				console.log(readOnly)
 				var modelName = document.getElementById("modelName").value;
				console.log("openModel: modelName=" + modelName);      
				
				// init UTM items
				$scope.utmModelsArray = [];
				$scope.selectedParent = {};
				$scope.currentUTMModel = {};
				$scope.currentUTMModel.selectedParent = {};
				$rootScope.oldUTMModels =[];
				$rootScope.projectName="clds_default_project";		
				var utmModels = {};
				utmModels.name = modelName;
				utmModels.subModels = [];
				$rootScope.utmModels = utmModels;
				

				cldsModelService.getModel( modelName ).then(function(pars) {
        			console.log("openModel: pars=" + pars);
        			console.log(pars)
        			// process data returned
        			var bpmnText = pars.bpmnText;
        			var propText = pars.propText;
        			var status = pars.status;
        			var controlNamePrefix = pars.controlNamePrefix;
        			var controlNameUuid = pars.controlNameUuid;
        			selected_template=pars.templateName
        			cldsModelService.processActionResponse(modelName, pars);
        			
        			// deserialize model properties
        			if ( propText == null ) {
            			console.log("openModel: propText is null");
        			} else {
            			console.log("openModel: propText=" + propText);
        				elementMap =  JSON.parse(propText);
        			}

         			selected_model = modelName;
    				
    				// set model bpmn and open diagram
        			$rootScope.isPalette = true;
        			modelXML = bpmnText;
        			visibility_model();
        		},
        		function(data) {
        			//alert("getModel failed");
        		});
       
				$modalInstance.close("closed");
			};

			setMultiSelect();
		}
	]
);
