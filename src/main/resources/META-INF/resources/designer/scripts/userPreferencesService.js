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

app.service('userPreferencesService', ['$http', '$q', function ($http, $q) {
	console.log("////////userPreferencesService");
	this.insertUserPreferences = function(userPreference, userPreferencesURL){
		console.log("insertUserPreferences");
		var def = $q.defer();
    	
        $http.post(userPreferencesURL, userPreference)
        .success(function(data){
        	console.log("data");
        	def.resolve(data);         	
        	
        })
        .error(function(data){  
        console.log("error");     	 	      
       	 	def.reject("insert user-preferences not successful");
        });
        
        return def.promise;
	};
	
	this.getUserPreferences = function(getPreferenceURL){
		console.log("getUserPreferences");
	    	var def = $q.defer();
	    	
	        $http.post(getPreferenceURL)
	        .success(function(data){  
	        console.log("data");      	
	        	def.resolve(data);         	
	        })
	        .error(function(data){ 
	        console.log("data");      	 	      
	       	 	def.reject("getUserPreference not successful");
	        });
	        
	        return def.promise; 
	    };
	
}]);
