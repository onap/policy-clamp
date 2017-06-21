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

app.service('soapRequestService', ['$http', '$q', function ($http, $q) {
    console.log("////////////soapRequestService");
	this.changetimeoutMode = function(timeoutMode){
        console.log("changetimeoutMode");
		console.log("timeoutmode:"+timeoutMode);
		if(timeoutMode == "Default")
			return false;
		else
			return true;
	};
	
	
	this.generateTst = function(tstInput, generateTSTUrl){
        console.log("generateTst");
		var def = $q.defer();
    	
        $http.post(generateTSTUrl, tstInput)
        .success(function(data){
            console.log("success");
        	def.resolve(data);         	
        	
        })
        .error(function(data){ 
        console.log("error");      	 	      
       	 	def.reject("GenerateTST not successful");
        });
        
        return def.promise;
	};
	
	
	this.generateTSTMultiple = function(tstInputList, generateTSTUrl){
        console.log("generateTSTMultiple");
		var def = $q.defer();
    	
        $http.post(generateTSTUrl, tstInputList)
        .success(function(data){
            console.log("success");
        	def.resolve(data);         	
        	
        })
        .error(function(data){ 
        console.log("error");      	 	      
       	 	def.reject("GenerateTST multiple not successful");
        });
        
        return def.promise;
	};
	
	this.downloadTst = function(tstId, tstName,downloadTSTUrl){
        console.log("downloadTst");
		var def = $q.defer();
		
		var downloadInput={};
		
		downloadInput.tstId=tstId;
		downloadInput.tstName=tstName;
    	
		$http({
            url: downloadTSTUrl,     method: "POST",     data: downloadInput,
             responseType: 'arraybuffer' }).success(function (data, status, headers, config) {
             console.log("success");            	 
            	 var results = [];
            	 
            	 
            	 results.data = new Blob([data], {type: 'application/octet-stream'});
                 console.log( results.data);
                 results.headers = headers();
                 results.status = status;
                 results.config = config;
                 def.resolve(results); 
                 console.log( "Result From UTM Server : " + results.data);
        })
        .error(function(data){
        console.log("error");       	 	      
       	 	def.reject("DownloadTST not successful");
        });
        
        return def.promise;
	};
	
}]);
