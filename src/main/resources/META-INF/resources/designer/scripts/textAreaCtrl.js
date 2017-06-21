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

app.controller('textAreaCtrl',['$scope','$rootScope','dialogs','Datafactory','$modalInstance',function($scope, $rootScope,dialogs, Datafactory,$modalInstance) {
	console.log("//////////textAreaCtrl");
	$scope.init = function(){
		console.log("init");
		$scope.textAreaModel=$rootScope.textAreaData;
		$scope.textAreaTitle=$rootScope.textAreaTitle;
	};
	
	$scope.init();
	
	$scope.close = function() {
		console.log("close");
		$rootScope.textAreaData = $('textarea#mytextarea').val();
		$modalInstance.close("closed");
	};
	
}]);

app.directive('focusMe', function($timeout, $parse) {
	console.log("focusMe");
	  return {
	    link: function(scope, element, attrs) {
	    	console.log("link");
	      var model = $parse(attrs.focusMe);
	      scope.$watch(model, function(value) {
	      	console.log("model");
	          $timeout(function() {
	          	console.log("setTimeout(function() {}, 10);");
	            element[0].focus(); 
	          });
	      });
	    }
	  };
});
