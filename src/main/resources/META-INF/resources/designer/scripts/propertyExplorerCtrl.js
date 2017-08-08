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
app.directive('opentext', function ($compile, dialogs, $rootScope){
    console.log("//////////opentext");
  return {
      restrict: "AE",
      link: function(scope,element,attrs){
        console.log("link");
    	  var elementHTML = '';
    	  element.bind("click", function(){
            console.log("bind");
    		
         	if(attrs.ngModel=='decisionIdentifier.textualInformation.testCaseDescription'){
         		
         		$rootScope.textAreaData = scope.decisionIdentifier.textualInformation.testCaseDescription;
                $rootScope.textAreaTitle = 'Test Case Description';
                var dlg = dialogs.create('partials/portfolios/text_area_modal.html','textAreaCtrl',{},{size:'sm',keyboard: true,backdrop: 'static'});
        		
                dlg.result.then(function(name){
                    console.log("dlg.result");
        			scope.decisionIdentifier.textualInformation.testCaseDescription = $rootScope.textAreaData;
        		},function(){
        			scope.decisionIdentifier.textualInformation.testCaseDescription = $rootScope.textAreaData;
        		});
        		
         	}else if(attrs.ngModel=='decisionIdentifier.textualInformation.testStepDescription'){
         		
         		$rootScope.textAreaData = scope.decisionIdentifier.textualInformation.testStepDescription;
                $rootScope.textAreaTitle = 'Test Step Description';
                var dlg = dialogs.create('partials/portfolios/text_area_modal.html','textAreaCtrl',{},{size:'sm',keyboard: true,backdrop: 'static'});
        		
                dlg.result.then(function(name){
                    console.log("dlg.result");
        			scope.decisionIdentifier.textualInformation.testStepDescription = $rootScope.textAreaData;
        		},function(){
        			scope.decisionIdentifier.textualInformation.testStepDescription = $rootScope.textAreaData;
        		});
        		
         	}else if(attrs.ngModel=='decisionIdentifier.textualInformation.expectedResult'){
         		
         		$rootScope.textAreaData = scope.decisionIdentifier.textualInformation.expectedResult;
                $rootScope.textAreaTitle = 'Expected Result';
                
                var dlg = dialogs.create('partials/portfolios/text_area_modal.html','textAreaCtrl',{},{size:'sm',keyboard: true,backdrop: 'static'});
        		dlg.result.then(function(name){
                    console.log("dlg.result");
        			scope.decisionIdentifier.textualInformation.expectedResult = $rootScope.textAreaData;
        		},function(){
        			scope.decisionIdentifier.textualInformation.expectedResult = $rootScope.textAreaData;
        		});
        		
         	}else if(attrs.ngModel=='pathDetails.textualInformation.testPathDescription'){
         		
         		if(scope.pathDetails.textualInformation==null){
         			scope.pathDetails.textualInformation = {};
         			scope.pathDetails.textualInformation.testPathDescription = '';
         		}
         		$rootScope.textAreaData = scope.pathDetails.textualInformation.testPathDescription;
                $rootScope.textAreaTitle = 'Test Path Description';
                var dlg = dialogs.create('partials/portfolios/text_area_modal.html','textAreaCtrl',{},{size:'sm',keyboard: true,backdrop: 'static'});
        		
                dlg.result.then(function(name){
                    console.log("dlg.result");
        			scope.pathDetails.textualInformation.testPathDescription = $rootScope.textAreaData;
        		},function(){
        			scope.decisionIdentifier.textualInformation.testPathDescription = $rootScope.textAreaData;
        		});
                
         	}else if(attrs.ngModel=='runtimePythonScript.inputParams'){
         		
         		$rootScope.textAreaData = scope.runtimePythonScript.inputParams;
                $rootScope.textAreaTitle = 'Input Parameter';
                var dlg = dialogs.create('partials/portfolios/text_area_modal.html','textAreaCtrl',{},{size:'sm',keyboard: true,backdrop: 'static'});
        		
                dlg.result.then(function(name){
                    console.log("");
        			scope.runtimePythonScript.inputParams = $rootScope.textAreaData;
        		},function(){
        			scope.runtimePythonScript.inputParams = $rootScope.textAreaData;
        		});
                
         	}
         	
    	  });
      }
  }
});

app.directive('inputInfoPropertyClass', function ($compile) {
    console.log("inputInfoPropertyClass");
  return {
      restrict: "C",
      replace: true,
      link: function(scope,element,attrs){
        console.log("link");
    	  var elementHTML = '';
    	  scope.sourceExplorer = 'PE';
    	  angular.forEach(scope.infoType.schemaElements, function(value, key){
            console.log("schemaElement");
    		  
    		  scope.schemaElement = value;
    		  
    		  if(scope.schemaElement.complexType != null){
    			  if(scope.currentElementName == ''){
    				  scope.currentElementName = scope.schemaElement.complexType.name;
    			  }
    			  
    			  scope.ParentKey = scope.parentName + '_' + scope.currentElementName;
    			  if(scope.schemaElement.repeatableHierarchicalPrefix != null){
    				  scope.ParentKey = scope.ParentKey + scope.schemaElement.repeatableHierarchicalPrefix; 
    			  }
    			  scope.showUTMViewMsgHeader = true;
    			  scope.parElement = scope.schemaElement;
    			  scope.tableStyle = 'table-level' + scope.heirarchyLevel + '-tree'; 
    			  scope.tdLabelStyle = 'td-level' + scope.heirarchyLevel + '-label-tree'; 
    			  scope.heirLevel = scope.heirarchyLevel;
    			  
    			  elementHTML = elementHTML + '<div ng-show="schemaElement.complexType != null">';
    			  elementHTML = elementHTML + '<table class="{{tableStyle}}"> <tr>';
    			  elementHTML = elementHTML + '<td class="{{tdLabelStyle}}">';
    			  elementHTML = elementHTML + '<span class="pull-left" ng-click="showUTMViewMsgHeader=!showUTMViewMsgHeader">';
    			  elementHTML = elementHTML + '<i ng-class="showUTMViewMsgHeader == true ?\'fa fa-plus-circle\':\'fa fa-minus-circle\'"></i>';
    			  elementHTML = elementHTML + '</span>';
    			  elementHTML = elementHTML + '<b>{{currentElementName}}</b>';
    			  elementHTML = elementHTML + '</td>';
    			  elementHTML = elementHTML + '</tr></table>';
    			  elementHTML = elementHTML + '<div style="margin-left: 10px" ng-class="{hidden:showUTMViewMsgHeader,chaldean:showUTMViewMsgHeader}">';
    			  elementHTML = elementHTML + '<div class="inputInfoPropertyClassMember" style="margin-left: 10px" ng-repeat="schemaElement in schemaElement.elements"></div>';
    			  elementHTML = elementHTML + '</div>';
    			  elementHTML = elementHTML + '</div>';
    			  var x = angular.element(elementHTML);
	                element.append(x);
	                $compile(x)(scope);
    		  }
	      });
    	  
      }
  }
});

app.directive('inputInfoPropertyClassMember', function ($compile) {
    console.log("inputInfoPropertyClassMember");
  return {
      restrict: "C",

      link: function(scope,element,attrs){
        console.log("link");
    	  var elementHTML = '';
    	  
    	  scope.currentElementName=scope.objectName;
    	  scope.parentName=scope.ParentKey; 
    	  scope.parentElement=scope.parElement; 
    	  scope.heirarchyLevel = scope.heirLevel + 1;
    	  
    	  if(scope.schemaElement.element.name != null){
    		  
    		  scope.elementKey=scope.parentName + '_' + scope.schemaElement.element.name;
    		  if(scope.schemaElement.repeatableHierarchicalPrefix != null){
    			  scope.elementKey = scope.elementKey + scope.schemaElement.repeatableHierarchicalPrefix;
    		  }
    		  scope.tableStyle='table-level' + scope.heirarchyLevel + '-tree'; 
    		  scope.tdLabelStyle='td-level' + scope.heirarchyLevel +'-label-tree';
    		  
    		  if(scope.schemaElement.type.complexType != null){
    			  scope.showUTMViewMsgHeader = false;
    			  
    		  }else{
    			  scope.showUTMViewMsgHeader = true;
    			  
    		  }
    		  
    		  elementHTML = elementHTML + '<div ng-show="schemaElement.element.name != null">';
    		  elementHTML = elementHTML + '<div ng-show = "schemaElement.type.complexType != null || utmSchemaExts[elementKey].checked">';
    		  elementHTML = elementHTML + '<table class="{{tableStyle}}"> ';
    		  elementHTML = elementHTML + '<tr>';
    		  elementHTML = elementHTML + '<td style="text-align: left;vertical-align: top;" class="{{tdLabelStyle}}">';
    		  elementHTML = elementHTML + '<span class="pull-left" ng-click="showUTMViewMsgHeader=!showUTMViewMsgHeader">';
    		  elementHTML = elementHTML + '<i expandable ng-class="showUTMViewMsgHeader == true ?\'fa fa-minus-circle\':\'fa fa-plus-circle\'"></i>';
    		  elementHTML = elementHTML + '{{schemaElement.element.name}}  ';
    		  elementHTML = elementHTML + '';
    		  elementHTML = elementHTML + '';
    		  elementHTML = elementHTML + '';
    		  elementHTML = elementHTML + '';
    		  elementHTML = elementHTML + '</span>';
    		  elementHTML = elementHTML + '</td>';
    		  
    		  
    		  
    		  elementHTML = elementHTML + '<td style="width: 70px">&nbsp;&nbsp;&nbsp;</td>';
    		  elementHTML = elementHTML + '<td style="width: 40px; float: left;">';
    		  if(scope.schemaElement.type.complexType == null){
    			  elementHTML = elementHTML + '<div ng-show="schemaElement.type.complexType == null">';
	    		  elementHTML = elementHTML + '<div ng-repeat="object in filteredObjects = (schemaElement.type.restriction.minExclusivesAndMinInclusivesAndMaxExclusives | filter: {name : \'enumeration\'})"></div>';
	    		  elementHTML = elementHTML + '<div ng-if="filteredObjects.length > 0" class="defaultSelect">';
	    		  elementHTML = elementHTML + '<input type="text" class="defaultVal" id="{{elementKey}}" ng-model="decisionIdentifier.elementValues[elementKey]" style="width:150px;"/>';
	    		  elementHTML = elementHTML + '<select style="width: 170px; id=;height: 20px;" id="{{elementKey}}" onchange="this.previousElementSibling.value=this.value;" ng-model="decisionIdentifier.elementValues[elementKey]" ';
	    		  elementHTML = elementHTML + 'ng-options="filteredObject.value.value as filteredObject.value.value for filteredObject in filteredObjects">';
	    		  elementHTML = elementHTML + '<option value=""></option>';
	    		  elementHTML = elementHTML + '</select>';
	    		  elementHTML = elementHTML + '</div>';
	    		  elementHTML = elementHTML + '<div ng-if="filteredObjects == null || filteredObjects.length == 0">';
	    		  elementHTML = elementHTML + '<div ng-if="schemaElement.type != null && schemaElement.type==\'boolean\'">';
	    		  elementHTML = elementHTML + '<div style="display: inline-flex">';
	    		  elementHTML = elementHTML + '<input type="radio" value="true" ng-model="decisionIdentifier.elementValues[elementKey]">True <span style="width:20px;"></span>';
	    		  elementHTML = elementHTML + '<input type="radio" value="false" ng-model="decisionIdentifier.elementValues[elementKey]">False';
	    		  elementHTML = elementHTML + '';
	    		  elementHTML = elementHTML + '';
	    		  elementHTML = elementHTML + '</div>';
	    		  elementHTML = elementHTML + '</div>';
	    		  elementHTML = elementHTML + '<div ng-if="schemaElement.type == null || schemaElement.type != \'boolean\'">';
	    		  elementHTML = elementHTML + '<input type="text"  id="{{elementKey}}"  style="width: 170px;" class="defaultVal" ng-model="decisionIdentifier.elementValues[elementKey]"/>';
	    		  elementHTML = elementHTML + '';
	    		  elementHTML = elementHTML + '</div>';
	    		  elementHTML = elementHTML + '';
	    		  elementHTML = elementHTML + '</div>';
	    		  elementHTML = elementHTML + '</div>';
    		  }
    		  
    		  elementHTML = elementHTML + '</td>';
    		  elementHTML = elementHTML + '</tr>';
    		  elementHTML = elementHTML + '<br/>';
    		  elementHTML = elementHTML + '</table>';
    		  
    		  
    		  
    		  elementHTML = elementHTML + '';
    		  elementHTML = elementHTML + '';
    		  elementHTML = elementHTML + '';
    		  elementHTML = elementHTML + '</div>';
    		  elementHTML = elementHTML + '</div>';
    		  
    		  var x = angular.element(elementHTML);
                element.append(x);
                $compile(x)(scope);
    		  
    		  
    		  if(scope.schemaElement.type.complexType != null){
	    		  var elementHTML2 = '<div ng-show="schemaElement.type.complexType != null">'
    			  elementHTML2 = elementHTML2 + '<div ng-init="parKey=parentName + \'_\' + schemaElement.element.name + (schemaElement.repeatableHierarchicalPrefix != null ? schemaElement.repeatableHierarchicalPrefix : \'\'); heirLevel=heirarchyLevel; parElement=schemaElement; ParentKey=ParentKey+\'_\'+schemaElement.element.name + (schemaElement.repeatableHierarchicalPrefix != null ? schemaElement.repeatableHierarchicalPrefix : \'\')">'
    			  elementHTML2 = elementHTML2 + '<div style="margin-left: 10px" ng-class="{hidden:!showUTMViewMsgHeader,chaldean:!showUTMViewMsgHeader}">'
    			  elementHTML2 = elementHTML2 + '<div class="{{sourceExplorer+\'_\'+parKey}}"></div>'
    			  elementHTML2 = elementHTML2 + '</div>'
    			  elementHTML2 = elementHTML2 + '</div>'
    			  elementHTML2 = elementHTML2 + '</div>';
	    		  var x = angular.element(elementHTML2);
	                element.append(x);
	                $compile(x)(scope);
	    	  }
    		  
    	  }

      }
  }
});

app.controller('propertyExplorerCtrl',['$scope','$rootScope','$location','$modalInstance','dialogs','Datafactory',function($scope, $rootScope, $location, $modalInstance, dialogs, Datafactory) {
	console.log("propertyExplorerCtrl");
    $rootScope.bpmnElementName;
	$rootScope.pathIdentifiers;
	$scope.propertyExplorerObject = {};

	$scope.pathDetailsList = [];
	$scope.utmSchemaExts = {};
	$scope.user3 ="";
	
	$scope.close = function(){
        console.log("close");
		$modalInstance.close();
	};
	
	$rootScope.initPropertyExplorer = function() {	
    console.log("initPropertyExplorer");	
		$scope.pathDetailsList = list_model_path_details[selected_model];
		if(list_model_schema_extensions[selected_model] != null && list_model_schema_extensions[selected_model].utmSchemaExtentionMap != null) {
			$scope.utmSchemaExts = list_model_schema_extensions[selected_model].utmSchemaExtentionMap;
		}
		
		 $(".resize-none").each(function() {
            console.log("resize-none");
			    $(this).val($(this).val().replace(/,/g, "\n"));
			  });
		
		if($scope.pathDetailsList == null)
			$scope.pathDetailsList = [];
		
		if ($rootScope.bpmnElementName != null) {
			if ($scope.pathDetailsList != null && $scope.pathDetailsList.length > 0) {
				var isPathDetailsAvailable = false;
				for (var i = 0; i < $scope.pathDetailsList.length; i++) {
					if ($scope.pathDetailsList[i].conditionalNode == $rootScope.bpmnElementName) {
						isPathDetailsAvailable = true;
					}
				}

				if (!isPathDetailsAvailable) {
					$scope.addPathDetails();
				}
			} else if ($scope.pathDetailsList == null) {
				$scope.addPathDetails();
			}
		}
	};

	$scope.addPathDetails = function() {
        console.log("addPathDetails");
		$scope.pathDetails = {};
		$scope.pathDetails.conditionalNode = $rootScope.bpmnElementName;
		$scope.pathDetailsList.push($scope.pathDetails);
		$scope.addDecisionIdentifier($scope.pathDetails);
		list_model_path_details[selected_model] = $scope.pathDetailsList;		
	};
	
	

	$scope.addDecisionIdentifier = function(pathDtls) {
        console.log("addDecisionIdentifier");
		$scope.decisionIdentifier = {};
		$scope.decisionIdentifier.textualInformation={};
		//$scope.decisionIdentifier.textualInformation.use=true;
		if (pathDtls.decisionIdentifiers != null) {
			if(angular.isUndefined($scope.decisionIdentifier.textualInformation.use))
				$scope.decisionIdentifier.textualInformation.use = true;			
			pathDtls.decisionIdentifiers.push($scope.decisionIdentifier);
		} else {
			pathDtls.decisionIdentifiers = [];
			if(angular.isUndefined($scope.decisionIdentifier.textualInformation.use))
				$scope.decisionIdentifier.textualInformation.use = true;			
			pathDtls.decisionIdentifiers.push($scope.decisionIdentifier);
		}
	};

	$scope.isElementCheckedinExplorer = function(elementId) {
        console.log("isElementCheckedinExplorer");
		console.log("Enter into property explorer");
		if (elementId.indexOf("_decisionValue") != -1) {
			var elementIdSplit = elementId.split("_decisionValue");
			var elementCheckBoxId = elementIdSplit[0] + "_checkbox";
			var elementCheckBoxValue = document.getElementById(elementCheckBoxId);
			if (elementCheckBoxValue != "" && elementCheckBoxValue.checked) {
				return true;
			}
			return false;
		}
		return false;
	};

	$scope.moreDecisions = function(pathDtls) {
        console.log("moreDecisions");
		//alert("PropertyExplorerCtrl entering moreDecisions");
		$scope.addDecisionIdentifier(pathDtls);
		//alert("PropertyExplorerCtrl exiting moreDecisions");
	};

	$scope.morePaths = function() {
        console.log("morePaths");
		var elementCount=0;
		$scope.pathDetailsList = list_model_path_details[selected_model];
		if ($scope.pathDetailsList == null){
			if($rootScope.pathIdentifiers.length>0){
				$scope.pathDetailsList = [];
				$scope.addPathDetails();
			}
			else
				dialogs.error('Error','Please define atleast 1 path in model to proceed.');
		}
		else
		{
		for(var i=0;i<$scope.pathDetailsList.length;i++){
			if($rootScope.bpmnElementName == $scope.pathDetailsList[i].conditionalNode){
				elementCount++;
			}
		}
		if(elementCount < $rootScope.pathIdentifiers.length)
			$scope.addPathDetails();
		else
			dialogs.error('Error','The number of paths defined for this conditional node cannot be more than the number of Path Identifiers defined in the Model.');
	}
	};

	$rootScope.initPropertyExplorer();
	
	$scope.sendDbData = function(dbToolRequestList){
        console.log("sendDbData");
		if(dbToolRequestList!=null){
			Datafactory.setDbDataList(dbToolRequestList);
		}
		else{
			dbToolRequestList=[];
			Datafactory.setDbDataList(dbToolRequestList);
		}
	};

	$scope.sendAssertData = function(xmlAsserterRequest){
        console.log("sendAssertData");
		if(xmlAsserterRequest!=null){
			Datafactory.setXmlAsserter(xmlAsserterRequest);
		}
		else{
			xmlAsserterRequest={};
			Datafactory.setXmlAsserter(xmlAsserterRequest);
		}
		
	};	

	$scope.sendRuntimePythonScriptData = function(pathDtls){
        console.log("sendRuntimePythonScriptData");
		if(pathDtls!=null){
			Datafactory.setRuntimePythonScriptList(pathDtls);
		}
		else{
			pathDtls=[];
			Datafactory.setRuntimePythonScriptList(pathDtls);
		}
		
	};
}]);
