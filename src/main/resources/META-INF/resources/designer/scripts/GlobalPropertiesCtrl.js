/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2017-2018 AT&T Intellectual Property. All rights
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
app.controller('GlobalPropertiesCtrl', [
'$scope',
'$rootScope',
'$uibModalInstance',
'cldsModelService',
'$location',
'dialogs',
'cldsTemplateService',
function($scope, $rootScope, $uibModalInstance, cldsModelService, $location,
         dialogs, cldsTemplateService) {
	$scope.$watch('name', function(newValue, oldValue) {

		setASDCFields()

		var el = elementMap["global"];
		if (el !== undefined) {
			for (var i = 0; i < el.length; i++) {
				if (el[i].name === 'deployParameters')
				{
					// This is a special case, that value is not a string but a JSON
					$("#" + el[i].name).val(JSON.stringify(el[i].value));
				} else {
					$("#" + el[i].name).val(el[i].value);
				}
			}
		}
		setMultiSelect();
		if (readMOnly) {
			$("#savePropsBtn").attr("disabled", "");
			$('select[multiple] option').each(function() {
				var input = $('input[value="' + $(this).val() + '"]');
				input.prop('disabled', true);
				input.parent('li').addClass('disabled');
			});
			$('input[value="multiselect-all"]').prop('disabled', true).parent(
			'li').addClass('disabled');
			($("select:not([multiple])")).multiselect("disable");
		}
	});
	$scope.retry = function() {
		console.log("retry");
	}
	$scope.close = function() {
		console.log("close");
		$uibModalInstance.close("closed");
	};
    $scope.convertDeployParametersJsonToString = function() {
        var index = elementMap["global"].findIndex(function(e) {
	        return (typeof e == "object" && !(e instanceof Array))
	        && "deployParameters" == e["name"];
        });
        if (index != -1) {
	        $('#deployParameters').val(JSON.stringify(elementMap["global"][index].value));
        }
    }
    
    function noRepeats(form) {
        var select = {};
        for (var i = 0; i < form.length; i++) {
	        if (form[i].hasOwnProperty("name")) {
		        if (form[i].name === 'deployParameters') {
					// This is a special case, that value MUST not be a string but a JSON
		        	select[form[i].name]=JSON.parse(form[i].value);
		        } else {
		        	if (select[form[i].name] === undefined)
				        select[form[i].name] = []
		        	select[form[i].name].push(form[i].value);
		        }
	        }
        }
        var arr = []
        for (s in select) {
	        var f = {}
	        f.name = s
	        f.value = select[s]
	        if (!(s == "service" && f.value == "")) {
		        arr.push(f)
	        }
        }
        return arr
    }
    
    $scope.submitForm = function() {
        saveGlobalProperties(noRepeats($("#saveProps").serializeArray()))
        //module reset, based on property updates
        if (elementMap["global"]) {
	        $.each(Object.keys(elementMap), function(i, v) {
		        if ((v.match(/^Policy/)) && asDiff) {
			        elementMap[v] = {};
		        }
		        if ((v.match(/^TCA/)) && (vfDiff || serDiff)) {
			        elementMap[v] = {};
		        }
	        });
        }
        $uibModalInstance.close();
    }
  
} ]);
