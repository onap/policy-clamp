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

var elementKeys = [];

app.directive('inputInfoUpgradeClass', function ($compile) {
    console.log("////UpgradeSchemaCtrl");
  return {
      restrict: "C",
      replace: true,
      link: function(scope,element,attrs){
          var elementHTML = '';
          angular.forEach(scope.infoType.schemaElements, function(value, key){
              
              scope.schemaElement = value;
              
              if(scope.schemaElement.complexType != null){
                  if(scope.currentElementName == ''){
                      scope.currentElementName = scope.schemaElement.complexType.name;
                  }
                  
                  scope.ParentKey = scope.parentName + '_' + scope.currentElementName;
                  if(scope.schemaElement.repeatableHierarchicalPrefix != null){
                      scope.ParentKey = scope.ParentKey + scope.schemaElement.repeatableHierarchicalPrefix; 
                  }
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
                  elementHTML = elementHTML + '<td class="td-tree"></td>';
                  elementHTML = elementHTML + '<td class="td-default_value-tree"> </td>';
                  elementHTML = elementHTML + '</tr></table>';
                  elementHTML = elementHTML + '<div style="margin-left: 10px" ng-class="{hidden:showUTMViewMsgHeader,chaldean:showUTMViewMsgHeader}">';
                  elementHTML = elementHTML + '<div class="inputInfoUpgradeClassMember" style="margin-left: 10px" ng-repeat="schemaElement in schemaElement.elements"></div>';
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

app.directive('inputInfoUpgradeClassMember', function ($compile) {
  return {
      restrict: "C",

      link: function(scope,element,attrs){
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
              elementHTML = elementHTML + '<table class="{{tableStyle}}"> ';
              elementHTML = elementHTML + '<tr>';
              elementHTML = elementHTML + '<td style="text-align: left;vertical-align: top;" class="{{tdLabelStyle}}">';
              elementHTML = elementHTML + '<span class="pull-left" ng-click="showUTMViewMsgHeader=!showUTMViewMsgHeader">';
              elementHTML = elementHTML + '<div style="display:inline">';
              elementHTML = elementHTML + '<input type="radio" name={{radioName}} id="{{elementKey}}" value={{schemaElement.element.name}}>';
              elementHTML = elementHTML + '</div>';
              elementHTML = elementHTML + '<i expandable ng-class="showUTMViewMsgHeader == true ?\'fa fa-minus-circle\':\'fa fa-plus-circle\'"></i>';
              elementHTML = elementHTML + '{{schemaElement.element.name}}  ';
              elementHTML = elementHTML + '';
              elementHTML = elementHTML + '';
              elementHTML = elementHTML + '';
              elementHTML = elementHTML + '';
              elementHTML = elementHTML + '</span>';
              elementHTML = elementHTML + '<div ng-init="complexMapElements(elementKey,schemaElement,radioName)"></div>';
              elementHTML = elementHTML + '</td>';
              
              elementHTML = elementHTML + '</tr>';
              elementHTML = elementHTML + '</table>';
              elementHTML = elementHTML + '';
              elementHTML = elementHTML + '';
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

app.controller(
                'UpgradeSchemaCtrl',
                [
                        '$scope',
                        '$rootScope',
                        '$modalInstance',
                        'dialogs',
                        function($scope, $rootScope, $modalInstance,dialogs) {

                            $rootScope.Currentmappedvalues = [];
                            $scope.utmSchemaExts = {};
                            
                            $scope.callFromMap=false;
                            $scope.oldMapValFlag=false;
                            
                            $scope.complexMappedValuesOld = {};
                            $scope.complexMappedValuesNew = {};
                            var allCurrentElementKeyArray=[];
                            
                            $scope.checkedValues = {};
                            var checkedElementValueArray=[];
                            
                            
                            $scope.complexMapElements = function(elementKey,schemaElement,radioName) {
                                if (schemaElement.complexType != null || (schemaElement.type != null && schemaElement.type.complexType != null)) {
                                    if(radioName=="oldChk")
                                        $scope.complexMappedValuesOld[elementKey]=schemaElement;
                                    else if(radioName=="newChk")
                                        $scope.complexMappedValuesNew[elementKey]=schemaElement;
                                }
                                if(elementKey != null)
                                    allCurrentElementKeyArray.push(elementKey);
                            
                            };
                            
                            $scope.mapElements = function() {
                                
                                var oldVal = $('input[name=oldChk]:checked')
                                        .val();
                                var newVal = $('input[name=newChk]:checked')
                                        .val();
                                var oldId = $('input[name=oldChk]:checked')
                                        .attr('id');
                                var newId = $('input[name=newChk]:checked')
                                        .attr('id');
                                $scope.mappedvalues = {};
                                
                                $scope.checkedValues.oldVal=oldVal;
                                $scope.checkedValues.newVal=newVal;
                                
                                checkedElementValueArray.push($scope.checkedValues);
                                
                                
                                $scope.oldMappedvaluesKeyArray = [];
                                $scope.newMappedvaluesKeyArray = [];
                                $scope.oldmappedvaluesArray = [];
                                $scope.newMappedvaluesArray = [];
                                
                                if($scope.complexMappedValuesOld[oldId] != null && $scope.complexMappedValuesNew[newId] != null){
                                    $scope.matchType='';
                                    $scope.matchType=$scope.compareElements($scope.complexMappedValuesOld[oldId],$scope.complexMappedValuesNew[newId]);
                                    if($scope.matchType == "true"){
                                        console.log("Element Type Matches and eligible for upgrade schema");
                                        
                                        $scope.callFromMap=true;
                                        for (var i = 0; i < $scope.complexMappedValuesOld[oldId].type.elements.length; i++) {
                                            $scope.oldMapValFlag=true;
                                            getElementkeys(oldId, $scope.complexMappedValuesOld[oldId].type.elements[i]);
                                        }
                                        
                                        for (var j = 0; j < $scope.complexMappedValuesNew[newId].type.elements.length; j++) {
                                            $scope.oldMapValFlag=false;
                                            getElementkeys(newId, $scope.complexMappedValuesNew[newId].type.elements[j]);
                                        }
                                        
                                        for (var k = 0; k < $scope.oldmappedvaluesArray.length; k++) {
                                            
                                            $scope.mappedvalues = {};
                                            
                                            $scope.mappedvalues.oldvalue = $scope.oldmappedvaluesArray[k];
                                            $scope.mappedvalues.newvalue = $scope.newMappedvaluesArray[k];
                                            $scope.mappedvalues.oldidvalue = $scope.oldMappedvaluesKeyArray[k];
                                            $scope.mappedvalues.newidvalue = $scope.newMappedvaluesKeyArray[k];
                                            $rootScope.Currentmappedvalues
                                                    .push($scope.mappedvalues);
                                        }
                                    }
                                    else if($scope.matchType == "false")    {
                                        
                                        dialogs.error('Invalid Selection Error','The mapping of the selected elements is invalid. Please select valid complex elements for Upgrade Schema');
                                            
                                    }
                                    
                                    
                                }
                                else if(($scope.complexMappedValuesOld[oldId] == null && $scope.complexMappedValuesNew[newId] != null) || ($scope.complexMappedValuesOld[oldId] != null && $scope.complexMappedValuesNew[newId] == null)){
                                        
                                    dialogs.error('Invalid Selection Error','The mapping of the selected elements is invalid. Please select valid complex elements for Upgrade Schema');
                                }
                                else{
                                    
                                $scope.mappedvalues.oldvalue = oldVal;
                                $scope.mappedvalues.newvalue = newVal;
                                $scope.mappedvalues.oldidvalue = oldId;
                                $scope.mappedvalues.newidvalue = newId;
                                $rootScope.Currentmappedvalues
                                        .push($scope.mappedvalues);
                                }
                                $rootScope.checkRepeatable = false;

                            };
                            
                            //Utility Method to compare Object Structure of Complex Type Elements before upgrade schema
                            $scope.compareElements = function(oldElement, newElement) {
                                
                                if (oldElement.type.complexType !=null && newElement.type.complexType !=null) {
                                    if (oldElement.type.elements.length==newElement.type.elements.length) {
                                        for (var i = 0; i < oldElement.type.elements.length; i++) {
                                            if(oldElement.type.elements[i].type.complexType !=null && newElement.type.elements[i].type.complexType !=null){
                                                $scope.compareElements(oldElement.type.elements[i], newElement.type.elements[i]);
                                            
                                            }else if(oldElement.type.elements[i].type.complexType ==null && newElement.type.elements[i].type.complexType !=null){
                                                $scope.matchType="false";
                                                return $scope.matchType;
                                            }
                                            if($scope.matchType == "false")
                                                return $scope.matchType;
                                            }
                                        for (var i = 0; i < newElement.type.elements.length; i++) {
                                            if(newElement.type.elements[i].type.complexType !=null && oldElement.type.elements[i].type.complexType !=null){
                                                $scope.compareElements(newElement.type.elements[i], oldElement.type.elements[i]);
                                            }else if(newElement.type.elements[i].type.complexType ==null && oldElement.type.elements[i].type.complexType !=null){
                                                $scope.matchType="false";
                                                return $scope.matchType;
                                            }
                                            if($scope.matchType == "false")
                                                return $scope.matchType;
                                            }
                                        $scope.matchType="true";
                                        return $scope.matchType;
                                    }
                                    else
                                        $scope.matchType="false";
                                        return $scope.matchType;
                                        
                                }
                                
                            };


                            $scope.checkRepeatableElement = function(
                                    elementKey, key) {

                                if (elementKey != key)
                                    $rootScope.checkRepeatable = true;

                            };

                            $scope.upgradeSchema = function() {
                                //console.log("List Model Path Details before Upgrade Schema :: " + JSON.stringify(list_model_path_details[selected_model]));
                                
                                $scope.callFromMap=false;
                                $rootScope.isHorR = true;
                                
                                $rootScope.repeatableHeirarchicalElementMap = map_model_repeatable_heirarchical_elements[selected_model];
                                
                                //Checking Repeatable Hierarchical elements mapping and changing elementkey if repeatable hierarchical is mapped
                                for (var key in $rootScope.repeatableHeirarchicalElementMap) {
                                    for(var i=0;i<allCurrentElementKeyArray.length;i++){
                                        if(allCurrentElementKeyArray[i].indexOf(key) > -1)
                                            elementKeys.push(allCurrentElementKeyArray[i]);
                                    }
                                    for (var j = 0; j< checkedElementValueArray.length; j++) {
                                        var currentCheckedMappedvalue = checkedElementValueArray[j];    
                                        if (key.indexOf(currentCheckedMappedvalue.oldVal) > -1){
                                            
                                            var newObject=JSON.stringify($rootScope.repeatableHeirarchicalElementMap);
                                            
                                            var oldvalue=currentCheckedMappedvalue.oldVal;
                                            var newvalue=currentCheckedMappedvalue.newVal;
                                            
                                            var modObject= newObject.replace(oldvalue, newvalue);
                                        
                                            $rootScope.repeatableHeirarchicalElementMap=angular.fromJson(modObject);
                                            
                                            /*for (var k = 0; k < elementKeys.length; k++) {
                                                
                                                if (elementKeys[k].indexOf(currentCheckedMappedvalue.oldVal) > -1){
                                                    
                                                elementKeys[k]=elementKeys[k].replace(oldvalue, newvalue);
                                                    
                                                }
                                                
                                            }*/
                                            
                                        }
                                    }
                                    
                                }
                                
                                
                                $scope.oldSchemaLocation = $rootScope.wsdlInfo.schemaLocation;
                                $rootScope.wsdlInfo = $rootScope.updateWsdlInfo;
                                $rootScope.wsdlInfo.schemaUpgradedFlag = true;
                                $rootScope.wsdlInfo.oldSchemaLocation = $scope.oldSchemaLocation;
                                
                                $rootScope.serviceInfo = $rootScope.updateServiceInfo;
                                $rootScope.schemaLocation = $rootScope.updateWsdlInfo.schemaLocation;
                                $rootScope.serviceInput = $rootScope.updateServiceInput;
                                $rootScope.serviceInputPartInfo = $rootScope.updateServiceInputPartInfo;
                                
                                $rootScope.inputSchemaServiceInputPartInfo=[];
                                $rootScope.inputSchemaServiceOutputPartInfo=[];
                                $rootScope.inputSchemaServicefaultPartInfo=[];
                                angular.copy($rootScope.serviceInputPartInfo, $rootScope.inputSchemaServiceInputPartInfo);
                                angular.copy($rootScope.serviceOutputPartInfo, $rootScope.inputSchemaServiceOutputPartInfo);
                                angular.copy($rootScope.servicefaultPartInfo, $rootScope.inputSchemaServicefaultPartInfo);

                                //Form all the element keys of the Upgraded Schema so that to know the attibutes removed                                
                                for (var i = 0; i < $rootScope.serviceInputPartInfo.length; i++) {
                                    for (var j = 0; j < $rootScope.serviceInputPartInfo[i].schemaElements.length; j++) {
                                        getElementkeys(
                                                'ServiceInput',
                                                $rootScope.serviceInputPartInfo[i].schemaElements[j]);
                                    }
                                }
                                $rootScope.serviceOutput = $rootScope.updateServiceOutput;
                                $rootScope.serviceOutputPartInfo = $rootScope.updateServiceOutputPartInfo;
                                for (var i = 0; i < $rootScope.serviceOutputPartInfo.length; i++) {
                                    for (var j = 0; j < $rootScope.serviceOutputPartInfo[i].schemaElements.length; j++) {
                                        getElementkeys(
                                                'ServiceOutput',
                                                $rootScope.serviceOutputPartInfo[i].schemaElements[j]);
                                    }
                                }
                                $rootScope.servicefault = $rootScope.updateServicefault;
                                $rootScope.servicefaultPartInfo = $rootScope.updateServicefaultPartInfo;
                                for (var i = 0; i < $rootScope.servicefaultPartInfo.length; i++) {
                                    for (var j = 0; j < $rootScope.servicefaultPartInfo[i].schemaElements.length; j++) {
                                        getElementkeys(
                                                'ServiceFault',
                                                $rootScope.servicefaultPartInfo[i].schemaElements[j]);
                                    }
                                }
                                console.log("mapped values of current"+ JSON.stringify($rootScope.Currentmappedvalues));
                                
                                //For each model in the project
                                // a) For the mapped elements
                                //       i) replace the old ids with new ids for the Schema Extensions
                                //      ii) replace the old ids with new ids for the Path Details
                                // b) For the deleted attributes in the Upgraded schema 
                                //       i) Remove the ids from Schema Extensions
                                //      ii) Remove the ids from Path Details
                                for(var modelIndex=0; modelIndex < $rootScope.models.length; modelIndex++) {
                                    var current_model = $rootScope.models[modelIndex];
                                    $scope.utmSchemaExts = list_model_schema_extensions[current_model].utmSchemaExtentionMap;
                                    $scope.pathDetailsArray = list_model_path_details[current_model];
                                
                                    for (var i = 0; i < $rootScope.Currentmappedvalues.length; i++) {
                                        $scope.mappedvalues = $rootScope.Currentmappedvalues[i];
    
                                        if($scope.utmSchemaExts != null) {
                                            $scope.utmSchemaExts[$scope.mappedvalues.newidvalue] = $scope.utmSchemaExts[$scope.mappedvalues.oldidvalue];
                                            if($scope.mappedvalues.newidvalue != $scope.mappedvalues.oldidvalue)
                                                delete $scope.utmSchemaExts[$scope.mappedvalues.oldidvalue];
                                        }
                                        
                                        if($scope.pathDetailsArray != null && $scope.pathDetailsArray.length > 0 ) {
                                            for (var k = 0; k < $scope.pathDetailsArray.length; k++) {
                                                $scope.pathDetails = $scope.pathDetailsArray[k];
                                                
                                                if ($scope.pathDetails != null) {
                                                    for (var j = 0; j < $scope.pathDetails.decisionIdentifiers.length; j++) {
                                                        if($scope.pathDetails.decisionIdentifiers[j].elementValues[$scope.mappedvalues.oldidvalue] != null)
                                                            $scope.pathDetails.decisionIdentifiers[j].elementValues[$scope.mappedvalues.newidvalue] = $scope.pathDetails.decisionIdentifiers[j].elementValues[$scope.mappedvalues.oldidvalue];
                                                        if($scope.mappedvalues.newidvalue != $scope.mappedvalues.oldidvalue)
                                                            delete $scope.pathDetails.decisionIdentifiers[j].elementValues[$scope.mappedvalues.oldidvalue];
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    
                                    $scope.schemenExts = angular.copy($scope.utmSchemaExts);
    
                                    //If an attribute is removed from upgraded schema, remove that attribute related details from SchemaExtensions
                                    Object.keys($scope.schemenExts).forEach(
                                        function(key) {
                                            var key_isavailable = false;
                                            for (var j = 0; j < elementKeys.length; j++) {
                                                
                                                /*for (var m = 0; m < checkedElementValueArray.length; m++) {
                                                    
                                                    var currentCheckedMappedvalue = checkedElementValueArray[m];
                                                    if ((key.indexOf(currentCheckedMappedvalue.oldVal) > -1) && (currentCheckedMappedvalue.oldVal != currentCheckedMappedvalue.newVal)){
                                                    
                                                        var oldvalue=currentCheckedMappedvalue.oldVal;
                                                        var newvalue=currentCheckedMappedvalue.newVal;
                                                        
                                                        key= key.replace(oldvalue, newvalue);
                                                        
                                                        elementKeys.push(key);
                                                    
                                                        //list_model_schema_extensions[current_model].utmSchemaExtentionMap=angular.fromJson(newUTMSchemaExtentionMapObject);
                                                    }
                                                }*/
                                                
                                                if (elementKeys[j] === key) {
                                                    key_isavailable = true;
                                                }
                                            }
                                            if (!key_isavailable) {
                                                //Implement this later. Commented this as this is wiping out all the Repeatable/Heirarchical values
                                                //delete $scope.utmSchemaExts[key];
                                            }
                                        }
                                    );
                                    
                                    //If an attribute is removed from upgraded schema, remove that attribute related details from PathDetails
                                    if($scope.pathDetailsArray != null && $scope.pathDetailsArray.length > 0 ) {
                                        for (var k = 0; k < $scope.pathDetailsArray.length; k++) {
                                            $scope.pathDetails = $scope.pathDetailsArray[k];
    
                                            for (var j = 0; j < $scope.pathDetails.decisionIdentifiers.length; j++) {
                                                $scope.decisionElementValues = angular.copy($scope.pathDetails.decisionIdentifiers[j].elementValues);
                                                Object.keys($scope.decisionElementValues).forEach(
                                                    function(key) {
                                                        var key_isavailable = false;
                                                        for (var l = 0; l < elementKeys.length; l++) {
                                                            
                                                            /*for (var m = 0; m < checkedElementValueArray.length; m++) {
                                                                
                                                                var currentCheckedMappedvalue = checkedElementValueArray[m];
                                                                if ((key.indexOf(currentCheckedMappedvalue.oldVal) > -1) && (currentCheckedMappedvalue.oldVal != currentCheckedMappedvalue.newVal)){
                                                                
                                                                    var oldvalue=currentCheckedMappedvalue.oldVal;
                                                                    var newvalue=currentCheckedMappedvalue.newVal;
                                                                    
                                                                    key= key.replace(oldvalue, newvalue);
                                                                
                                                                    //list_model_schema_extensions[current_model].utmSchemaExtentionMap=angular.fromJson(newUTMSchemaExtentionMapObject);
                                                                }
                                                            }*/
                                                            if (elementKeys[l] === key) {
                                                                key_isavailable = true;
                                                            }
                                                        }
                                                        if (!key_isavailable) {
                                                            //Implement this later. Commented this as this is wiping out all the Repeatable/Heirarchical values
                                                            //delete $scope.pathDetails.decisionIdentifiers[j].elementValues[key];
                                                        }
                                                    }
                                                );
                                            }
                                        }
                                    }
    
                                    //console.log("List Model Path Details after Upgrade Schema :: " + JSON.stringify(list_model_path_details[current_model]));
                                    //console.log("UTMSchema Extension after Upgrade Schema :: "    + JSON.stringify($scope.utmSchemaExts));
                                    
                                }
                                $modalInstance.close("closed");
                            };
                            
                            $scope.close = function() {

                                $modalInstance.close("closed");
                            };
                            
                            
                            function getElementkeys(parentname, schemaelement) {
                                if (schemaelement.complexType != null) {
                                    var parentkey = parentname + "_" + schemaelement.complexType.name;
                                    for (var i = 0; i < schemaelement.elements.length; i++) {
                                        getElementkeys(parentkey, schemaelement.elements[i]);
                                    }
                                }
                                if (schemaelement.element != null && schemaelement.element.name != null) {
                                    var elementKey = parentname + '_' + schemaelement.element.name;
                                    
                                    if(!$scope.callFromMap){
                                        elementKeys.push(elementKey);
                                    }
                                    else{
                                        if($scope.oldMapValFlag){
                                            $scope.oldmappedvaluesArray.push(schemaelement.element.name);
                                            $scope.oldMappedvaluesKeyArray.push(elementKey);
                                        }
                                        else{
                                            
                                            $scope.newMappedvaluesArray.push(schemaelement.element.name);
                                            $scope.newMappedvaluesKeyArray.push(elementKey);
                                        }
                                    }
                                        
                                    
                                    
                                }
                                if (schemaelement.type != null && schemaelement.type.complexType != null) {
                                    var parentkey = parentname + '_' + schemaelement.element.name;
                                    for (var i = 0; i < schemaelement.type.elements.length; i++) {
                                        getElementkeys(parentkey, schemaelement.type.elements[i]);
                                    }
                                }
                            }

                        }]);

