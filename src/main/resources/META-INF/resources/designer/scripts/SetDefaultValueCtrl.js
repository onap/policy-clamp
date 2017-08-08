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

app.directive('inputInfoDVClass', function ($compile) {
    console.log("/////////////inputInfoDVClass");
  return {
      restrict: "C",
      replace: true,
      link: function(scope,element,attrs){
        console.log("link");
    	  var elementHTML = '';
    	  scope.sourceExplorer = 'SDV';
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
    			  elementHTML = elementHTML + '<div class="inputInfoDVClassMember" style="margin-left: 10px" ng-repeat="schemaElement in schemaElement.elements"></div>';
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

app.directive('inputInfoDVClassMember', function ($compile) {
    console.log("inputInfoDVClassMember");
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
    		  elementHTML = elementHTML + '<div id="elementKey">';
    		  //console.log(scope.utmSchemaExts);
    		  elementHTML = elementHTML + '<div ng-show = "schemaElement.type.complexType != null || utmSchemaExts[elementKey].checked">';
    		  elementHTML = elementHTML + '<table class="{{tableStyle}}"> ';
    		  elementHTML = elementHTML + '<tr>';
    		  elementHTML = elementHTML + '<td style="text-align: left;vertical-align: top;" class="{{tdLabelStyle}}">';
    		  elementHTML = elementHTML + '<span class="pull-left" ng-click="showUTMViewMsgHeader=!showUTMViewMsgHeader">';
    		  elementHTML = elementHTML + '<i expandable ng-class="showUTMViewMsgHeader == true ?\'fa fa-minus-circle\':\'fa fa-plus-circle\'"></i>';
    		  elementHTML = elementHTML + '{{schemaElement.element.name}}  ';
    		  elementHTML = elementHTML + '';
    		  elementHTML = elementHTML + '</span>';
    		  elementHTML = elementHTML + '</td>';
    		  
    		  elementHTML = elementHTML + '<td style="width: 70px"></td>';
    		  elementHTML = elementHTML + '<td style="width: 40px; float: left;">';
    		  if(scope.schemaElement.type.complexType == null){
    			  elementHTML = elementHTML + '<div ng-show="schemaElement.type.complexType == null">';
    			  elementHTML = elementHTML + '<div ng-repeat="object in filteredObjects = (schemaElement.type.restriction.minExclusivesAndMinInclusivesAndMaxExclusives | filter: {name : \'enumeration\'})"></div>';
	    		  elementHTML = elementHTML + '<div ng-if="filteredObjects.length > 0" class="defaultSelect">';
	    		  elementHTML = elementHTML + '<input type="text" id="{{elementKey}}" class="defaultVal" ng-model="utmSchemaExts[elementKey].defaultValue" style="width:220px;"/>';
	    		  elementHTML = elementHTML + '<select  style="width: 240px;" id="{{elementKey}}" onchange="this.previousElementSibling.value=this.value;"  ng-model="utmSchemaExts[elementKey].defaultValue" ng-options="filteredObject.value.value as filteredObject.value.value for filteredObject in filteredObjects">';
	    		  elementHTML = elementHTML + '<option value=""></option>';
	    		  elementHTML = elementHTML + '</select>';
	    		  elementHTML = elementHTML + '</div>';
	    		  elementHTML = elementHTML + '<div ng-if="filteredObjects == null || filteredObjects.length == 0">';
	    		  elementHTML = elementHTML + '<div ng-if="schemaElement.type != null && schemaElement.type==\'boolean\'">';
	    		  elementHTML = elementHTML + '<div style="display: inline-flex">';
	    		  elementHTML = elementHTML + '<input type="radio" name="{{elementKey}}" id="{{elementKey}}" value="true" ng-model="utmSchemaExts[elementKey].defaultValue">True <span style="width:20px;"></span>';
	    		  elementHTML = elementHTML + '<input type="radio" name="{{elementKey}}" id="{{elementKey}}" value="false" ng-model="utmSchemaExts[elementKey].defaultValue">False';
	    		  elementHTML = elementHTML + '</div>';
	    		  elementHTML = elementHTML + '</div>';
	    		  elementHTML = elementHTML + '<div ng-if="schemaElement.type == null || schemaElement.type != \'boolean\'">';
	    		  elementHTML = elementHTML + '<input type="text"  id="{{elementKey}}"  style="width: 240px;" class="defaultVal" ng-model="utmSchemaExts[elementKey].defaultValue"/>';
	    		  elementHTML = elementHTML + '</div>';
	    		  elementHTML = elementHTML + '</div>';
	    		  elementHTML = elementHTML + '</div>';
    		  }
    		  
    		  elementHTML = elementHTML + '</td>';
    		  
    		  elementHTML = elementHTML + '</tr>';
    		  elementHTML = elementHTML + '<br/>';
    		  elementHTML = elementHTML + '</table>';
    		  elementHTML = elementHTML + '</div>';
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
