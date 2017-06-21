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

app.service('exportService', ['$http', '$q', function ($http, $q) {
    console.log("/////////exportService");
    this.exportToUrl = function(testsetValue, formatValue, exporturl){
        console.log("exportToUrl");
    	    	
    	var def = $q.defer();
    	var sets = [];
        var testExportRequest = {testSet: testsetValue, format: formatValue};
            	
        if (angular.equals(formatValue,"Excel")) {
       
        $http({
            url: exporturl,     method: "POST",     data: testExportRequest, //this is your json data string     headers: {
             responseType: 'arraybuffer' }).success(function (data, status, headers, config) {
                console.log("success");
            	/*sets = data;
            	def.resolve(data);*/
            	 
            	 var results = [];
                 results.data = data;
                 results.headers = headers();
                 results.status = status;
                 results.config = config;
                 def.resolve(results); 
        })
        .error(function(data){
            console.log("data");
    	 	      
       	 	def.reject("Export file not successful");
        });
        }
        else {
        	  $http.post(exporturl, testExportRequest)
            .success(function(data, status, headers, config){
                console.log("function");
            	           	
            	var results = [];
                results.data = data;
                results.headers = headers();
                results.status = status;
                results.config = config;

                def.resolve(results); 
            	//alert("Data in success without scope and q_def for scope parametes :: " + parameters);'Content-type': 'application/json',
            }) 
            .error(function(data){
                console.log("data");
           	 	//alert("Data in error :: " + data);      
           	 	def.reject("Export file not successful");
            });
        }
        return def.promise;
    };
    
    this.exportToUTMTestSet = function(almqcExport, exporturl){
    console.log("exportToUTMTestSet");    	
    	var def = $q.defer();
    	var sets = [];
        //var almqcExport =  almqcExport;       
        $http({
            url: exporturl,     method: "POST",     data: almqcExport,
             responseType: 'arraybuffer' }).success(function (data, status, headers, config) {
             console.log("success");            	 
            	 var results = [];
                 results.data = data;
                 results.headers = headers();
                 results.status = status;
                 results.config = config;
                 def.resolve(results); 
        })
        .error(function(data){
            console.log("data");
    	 	      
       	 	def.reject("Export file not successful");
        });        
        
        return def.promise;
    };
}]);
