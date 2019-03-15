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
'$http',
'$q',
'cldsModelService',
'$location',
'dialogs',
function($scope, $rootScope, $uibModalInstance, $http, $q, cldsModelService, $location,
         dialogs) {
	$scope.$watch('name', function(newValue, oldValue) {

		var el = getGlobalProperty();
		if (el !== undefined) {
			for (var key in el) {
				if (key === 'dcaeDeployParameters')
				{
					$("#" + key).val(JSON.stringify(el[key]));
				} else {
					$("#" + key).val(el[key]);
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


    function noRepeats(form) {
    	
        var select = {};
        for (var i = 0; i < form.length; i++) {
	        if (form[i].hasOwnProperty("name")) {
		        if (form[i].name === 'dcaeDeployParameters') {
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
       var form = noRepeats($("#saveProps").serializeArray());
       var obj = {};
		for( var i = 0; i < form.length; ++i ) {
			var name = form[i].name;
			var value = form[i].value;
			if( name ) {
				obj[ name ] = value;
			}
		}

    	$scope.saveGlobalProperties(JSON.stringify(obj)).then(function(pars) {
	        updateGlobalProperties(obj);
		}, function(data) {
		});
        $uibModalInstance.close();
    };
	$scope.saveGlobalProperties = function(form) {
		var modelName = getLoopName();
	   	 var def = $q.defer();
	   	 var svcUrl = "/restservices/clds/v2/loop/updateGlobalProperties/" + modelName;
	   	 $http.post(svcUrl, form).success(function(data) {
	   		 def.resolve(data);
	   	 }).error(function(data) {
	   		 def.reject("Save Model not successful");
	   	 });
	       return def.promise;
	   };
} ]);
