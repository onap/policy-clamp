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
				$("#ridinSpinners").css("display","")
				var bool=loadSharedPropertyByService(undefined,true,callBack);
				$("#ridinSpinners").css("display","none");
				
				
			}
			
			cldsTemplateService.getSavedTemplate().then(function(pars) {
				$scope.templateNamel=[]
				for(var i=0;i<pars.length;i++){
					$scope.templateNamel.push(pars[i].value);
				}
				setTimeout(function(){
					setMultiSelect();}, 100);
			});
			
			function contains(a, obj) {
			    var i = a&& a.length>0 ? a.length : 0;
			    while (i--) {
			       if (a[i].toLowerCase() === obj.toLowerCase()) {
			           return true;
			       }
			    }
			    return false;
			}
			$scope.checkExisting=function(){
				var name = $('#modelName').val();								
				if(contains($scope.modelNamel,name)){
					$scope.nameinUse=true;
				}else{
					$scope.nameinUse=false;
				}
				specialCharacters();
			}
			 function specialCharacters (){
				$scope.spcl = false;
				if(angular.element("#modelName") && 
					angular.element("#modelName").scope().model.$error.pattern && 
					angular.element("#modelName").scope().model.$error.pattern.length>0){
					$scope.spcl =  true;
				}
			}

			$scope.setTypeModel=function(_type){
				$scope.error.flag = false;
				$scope.typeModel=_type;
			}
			
			$scope.close = function(){
				$rootScope.isNewClosed = false;
				$modalInstance.close("closed");
			};
			$scope.createNewModelOffTemplate=function(formModel){
				reloadDefaultVariables(false)
 				var modelName = document.getElementById("modelName").value;
 				var templateName=document.getElementById("templateName").value;
 				
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
        			
        			var tempImageText=pars.imageText
        			var bpmnText=pars.bpmnText
        			var authorizedToUp = pars.userAuthorizedToUpdate;
        			pars={}
        			
        			pars.imageText=tempImageText
        			pars.status= "DESIGN";
        			if (readOnly || readMOnly){
        				pars.permittedActionCd=[""];
        			} else {
        				pars.permittedActionCd=["TEST", "SUBMIT"];
        			}
        			
        			selected_template= templateName
         			selected_model = modelName;
        			
        			cldsModelService.processActionResponse(modelName, pars);
    				
    				// set model bpmn and open diagram
        			$rootScope.isPalette = true;
        			modelXML = bpmnText;
        			visibility_model();
        		},
        		function(data) {
        			//alert("getModel failed");
        		});
				allPolicies = {};
				elementMap = {};
				$modalInstance.close("closed");
			
			}
			
			$scope.cloneModel=function(){
				reloadDefaultVariables(false)
 				var modelName = document.getElementById("modelName").value;
				var originalModel=document.getElementById("modelList").value;
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
        			
        			// process data returned
        			var bpmnText = pars.bpmnText;
        			var propText = pars.propText;
        			var status = pars.status;
        			var controlNamePrefix = pars.controlNamePrefix;
        			var controlNameUuid = pars.controlNameUuid;
        			selected_template=pars.templateName;
        			typeID = pars.typeId;
        			pars.status="DESIGN";
        			if (readOnly || readMOnly){
        				pars.permittedActionCd=[""];
        			} else {
        				pars.permittedActionCd=["TEST", "SUBMIT"];
        			}
        			pars.controlNameUuid="";
        			modelEventService = pars.event;
        			//actionCd = pars.event.actionCd;
        			actionStateCd = pars.event.actionStateCd;
        			deploymentId = pars.deploymentId;
        				
        			var authorizedToUp = pars.userAuthorizedToUpdate;
        			
        			cldsModelService.processActionResponse(modelName, pars);
        			
        			// deserialize model properties
        			if ( propText == null ) {
        			} else {
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
				reloadDefaultVariables(false)
 				var modelName = document.getElementById("modelName").value;  
				
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
    			var pars = {status: "DESIGN"};
    			
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
				$scope.openModel();
			}
			$scope.openModel = function(){
				reloadDefaultVariables(false)
				if(document.getElementById("readOnly")){
					readOnly=document.getElementById("readOnly").checked;
				}
 				var modelName = document.getElementById("modelName").value;    
				
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
        			// process data returned
        			var bpmnText = pars.bpmnText;
        			var propText = pars.propText;
        			var status = pars.status;
        			controlNamePrefix = pars.controlNamePrefix;
        			// var controlNameUuid = pars.controlNameUuid;
        			var authorizedToUp = pars.userAuthorizedToUpdate;
        			typeID = pars.typeId;
        			controlNameUuid = pars.controlNameUuid;
        			selected_template=pars.templateName;
        			modelEventService = pars.event;
        			//actionCd = pars.event.actionCd;
        			actionStateCd = pars.event.actionStateCd;
        			deploymentId = pars.deploymentId;

        			if (readMOnly || readOnly){
        				pars.permittedActionCd= [""];
        			}
        			cldsModelService.processActionResponse(modelName, pars);
        			
        			// deserialize model properties
        			if ( propText == null ) {
        			} else {
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