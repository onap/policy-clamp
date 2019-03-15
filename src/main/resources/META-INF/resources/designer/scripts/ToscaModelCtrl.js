/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights
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
app.controller('ToscaModelCtrl',
    ['$scope', '$rootScope', '$modalInstance', '$location', 'dialogs', 'toscaModelService',
    function($scope, $rootScope, $modalInstance, $location, dialogs, toscaModelService) {

        $scope.jsonByPolicyType = function(selectedPolicy, oldSelectedPolicy){
        	if (selectedPolicy && selectedPolicy != '') {

	        		$('#editor').empty();

		    		var toscaModel = getMsUI(selectedPolicy);
		    		if (toscaModel == null) {
		    			$modalInstance.close('closed');
		    			return;
		    		}
		    		var editorData = getMsProperty(selectedPolicy);

	  	        	JSONEditor.defaults.options.theme = 'bootstrap3';
		    		JSONEditor.defaults.options.iconlib = 'bootstrap2';
		    		JSONEditor.defaults.options.object_layout = 'grid';
		    		JSONEditor.defaults.options.disable_properties = true;
		    		JSONEditor.defaults.options.disable_edit_json = true;
		    		JSONEditor.defaults.options.disable_array_reorder = true;
		    		JSONEditor.defaults.options.disable_array_delete_last_row = true;
		    		JSONEditor.defaults.options.disable_array_delete_all_rows = false;
		    		JSONEditor.defaults.options.show_errors = 'always';

		    		if($scope.editor) { $scope.editor.destroy(); }
		    		$scope.editor = new JSONEditor(document.getElementById("editor"),
		    				      { schema: toscaModel.schema, startval: editorData });
		    		$scope.editor.watch('root.policy.recipe',function() {

		    		});
		    		$('#form1').show();

        	} else {
				$('#editor').empty();
				$('#form1').hide();
			}
        }

    	$scope.$watch('name', function() {
            if($rootScope.selectedBoxName) {
            	var policyType = $rootScope.selectedBoxName.split('_')[0].toLowerCase();
    	    	$scope.toscaModelName = policyType.toUpperCase() + " Microservice";
    	    	$scope.jsonByPolicyType($rootScope.selectedBoxName, '', '');
            }
    	});

        $scope.getEditorData = function(){
        	if(!$scope.editor){
        		return null;
        	}
        	var errors = $scope.editor.validate();
        	var editorData = $scope.editor.getValue();

        	if(errors.length) {
        		$scope.displayErrorMessage(errors);
        		return null;
        	}
        	else{
        		console.log("there are NO validation errors........");
        	}
        	return editorData;
        }

        $scope.saveToscaProps = function(){
        	var policyType = $rootScope.selectedBoxName;
            var data = $scope.getEditorData();
            if(data !== null) {
            	var msJson = JSON.parse(JSON.stringify(getMsJson(policyType)));
            	msJson["properties"] = data[0];
            	toscaModelService.saveMsProperties(msJson).then(function(pars) {
            		updateMsProperties(policyType, msJson);
            	}, function(data) {
            	});
            	if($scope.editor) { $scope.editor.destroy(); $scope.editor = null; }
            	$modalInstance.close('closed');
            }
        }

        $scope.displayErrorMessage = function(errors){
        	console.log("there are validation errors.....");
    		var all_errs = "Please address the following issues before selecting 'Done' or 'Policy Types':\n";
    		for (var i = 0; i < errors.length; i++) {
    		  if(all_errs.indexOf(errors[i].message) < 0) {
    			all_errs += '\n' + errors[i].message;
    		  }
    		}
            window.alert(all_errs);
        };

        $scope.close = function(){
			$modalInstance.close('closed');
			if($scope.editor) { $scope.editor.destroy(); $scope.editor = null; }
        }

        $scope.checkDuplicateInObject = function(propertyName, inputArray) {
        	  var seenDuplicate = false,
        	      testObject = {};

        	  inputArray.map(function(item) {
        	    var itemPropertyName = item[propertyName];
        	    if (itemPropertyName in testObject) {
        	      testObject[itemPropertyName].duplicate = true;
        	      item.duplicate = true;
        	      seenDuplicate = true;
        	    }
        	    else {
        	      testObject[itemPropertyName] = item;
        	      delete item.duplicate;
        	    }
        	  });

        	  return seenDuplicate;
        	}
}
]);