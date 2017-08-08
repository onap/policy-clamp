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
app.directive('fileModel', ['$parse', function ($parse) {
    console.log("////////fileModel");
    return {
        restrict: 'A',
        link: function(scope, element, attrs) {   
        console.log("link");     	
            var model = $parse(attrs.fileModel);
        	//alert("uploadFileToUrl directive model :: " + model);
            var modelSetter = model.assign;
            
            element.bind('change', function(){
                console.log("change");
                scope.$apply(function(){
                console.log("apply");               	
                    modelSetter(scope, element[0].files[0]);
                });
            });
        }
    };
}]);


app.service('fileUpload', ['$http', '$q', function ($http, $q) {
    console.log("fileUpload");
    this.uploadFileToUrl = function(file, uploadUrl){
        console.log("uploadFileToUrl");
    	//alert("uploadFileToUrl file :: " + file + " :: url::" + uploadUrl);
    	
    	var def = $q.defer();
    	var pars = [];
    	
        var fd = new FormData();
        fd.append('requestFile', file);
        $http.post(uploadUrl, fd, {
            transformRequest: angular.identity,            
            headers: {'Content-Type': undefined}
        })
        .success(function(data){
        console.log("success");        	
        	pars = data;
        	def.resolve(data);        	
        })
        .error(function(data){
        console.log("error");       	 	      
       	 	def.reject("Upload file not successful");
        });
        
        return def.promise;
    };
    
    this.uploadFile = function(path,inputFile,uploadURL){
        console.log("uploadFile");
    	var def = $q.defer();
    	var pars = [];
    	
        var fd = new FormData();
        fd.append('requestFile', inputFile);
        fd.append('path',path)
        $http.post(uploadURL, fd, {
            transformRequest: angular.identity,            
            headers: {'Content-Type': undefined}
        })
        .success(function(data){ 
        console.log("success");       	
        	pars = data;
        	def.resolve(data);        	
        })
        .error(function(data){
        console.log("error");       	 	      
       	 	def.reject("Upload file not successful");
        });
        
        return def.promise;
    	
    };
}]);