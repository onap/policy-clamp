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

app.controller('CldsOpenTemplateCtrl',
	['$scope', '$rootScope', '$modalInstance','$window','cldsTemplateService', '$location', 'dialogs',
		function($scope, $rootScope, $modalInstance,$window, cldsTemplateService, $location,dialogs) {
			console.log("////////////CldsOpenTemplateCtrl");	
			$scope.error = {
				flag : false,
				message: ""
 			};					
			cldsTemplateService.getSavedTemplate().then(function(pars) {
				//alert("lol")
				//////////mySelect.empty();
				$scope.modelNamel=[]
				for(var i=0;i<pars.length;i++){
					$scope.modelNamel.push(pars[i].value);
					
					//console.log($scope.modelNamel[i])
				}
				setTimeout(function(){
		        console.log("setTimeout");

		     setMultiSelect(); }, 100);
				
				
			});
			function contains(a, obj) {
				console.log("contains");
			    var i = a.length;
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
				//alert(name)
				//console.log($scope.modelNamel)
				if(contains($scope.modelNamel,name)){
					$scope.nameinUse=true;
				}else{
					$scope.nameinUse=false;
				}

			}
			
			$scope.closeDiagram=function(){
				console.log("closeDiagram");
				$window.location.reload();
			}
			
			
			
			$scope.createNewTemplate=function(){
				console.log("createNewTemplate");
				reloadDefaultVariables(true)
				if($(".bjs-container").is("[hidden]")){
					$(".bjs-container").removeAttr("hidden");
					$("#svgContainer").remove();
				}
 				var modelName = document.getElementById("modelName").value;
 				if(!modelName){
 					$scope.error.flag =true;
 					$scope.error.message = "Please enter any name for proceeding";
 				    return false;
 				}
 				
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
				
    			// enable appropriate menu options - non are available for templates...
    			//var pars = {status: "DESIGN", permittedActionCd: ["DISTRIBUTE"]};
				var pars={}
		        pars.controlNamePrefix=""
		        pars.controlNameUuid=""
		        pars.event={}
		        pars.event.actionStateCD=""
		        pars.newTemplate = true
				cldsTemplateService.processActionResponse(modelName, pars);
    			
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
				$scope.openTemplate();
			}
			$scope.close=function(){
				console.log("close");
				$rootScope.isNew = false;
				$modalInstance.close("closed");
			}
			$scope.openTemplate = function() {
				console.log("openTemplate");
				reloadDefaultVariables(true)
				if($(".bjs-container").is("[hidden]")){
					$(".bjs-container").removeAttr("hidden");
					$("#svgContainer").remove();
				}
 				var modelName = document.getElementById("modelName").value;
				console.log("openModel: modelName=" + modelName);    
				if($scope.modelNamel.includes(document.getElementById("modelName").value)){

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
				

				cldsTemplateService.getTemplate( modelName ).then(function(pars) {
        			console.log("openModel: pars=");
        			console.log(pars)
        			// process data returned
        			var bpmnText = pars.bpmnText;
        			var propText = pars.propText;
        			var status = pars.status;
        			var controlNamePrefix = pars.controlNamePrefix;
        			var controlNameUuid = pars.controlNameUuid;
        			
        			cldsTemplateService.processActionResponse(modelName, pars);
        			
        			// deserialize model properties
        			console.log("prop text")
        			console.log(propText)
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
        			console.log("data");
        			//alert("getModel failed");
        		});
       
				$modalInstance.close("closed");
			};
			
		}
	]
);
