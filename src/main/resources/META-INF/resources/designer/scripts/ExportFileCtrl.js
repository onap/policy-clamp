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

app.controller('exportCtrl', ['$scope', '$rootScope','exportService','dialogs', function($scope,$rootScope,exportService,dialogs){
    console.log("exportCtrl");
	
	
$scope.exportfile = function(format) {
    console.log("//////////exportfile");

    	console.log("exporting data inside exportfile function...."); 
		
		var formatValue=format;
		$rootScope.exportFormat = format;		
		var testsetValue=$rootScope.testset;     
        
        var exporturl = "/utm-service/testset_export/exportTestSet";
                
        exportService.exportToUrl(testsetValue, formatValue, exporturl)
        .then(function(results) {
            console.log("results");
        	
        	 var sets=results.data;
        	 console.log("Sets value"+sets);            
             var headerValue=results.headers;
             var fileName=headerValue.filename;
             console.log("Filename Server value"+fileName);
        		
            
	  			var hiddenElement = document.createElement('a');

	  			if (angular.equals($rootScope.exportFormat,"Excel")) {
	  				
	  				var blob = new Blob([sets], {type: "application/vnd.ms-excel"});
	  	            var objectUrl = URL.createObjectURL(blob);
    	  			//alert("EXCEL Format");
        	  		hiddenElement.href = objectUrl;
        	  		hiddenElement.download = fileName;
        	  	} else if (angular.equals($rootScope.exportFormat,"NIST")) {
    	  			//alert("NIST Format");
            	  	hiddenElement.href = 'data:attachment/nist,' + encodeURI(sets);
            	  	hiddenElement.download = fileName;
            	} else {
       	  			//alert("CSV Format");
           	  		hiddenElement.href = 'data:attachment/csv,' + encodeURI(sets);
           	  		hiddenElement.download = fileName;
            	}		
        	    
        	    hiddenElement.target = '_blank';
        	    hiddenElement.click();   	
        },
        function(data) {
            console.log("data");
        	//alert("File upload failed and parameters not returned");
        });
    };
    
   /* $rootScope.exportUTMTestSet = function() {
        console.log("exportUTMTestSet");
    	
    	
    	var dlg = dialogs.create('partials/portfolios/export_almqc_data.html','exportALMQCCtrl',{},{size:'xlg',keyboard: true,backdrop: true,windowClass: 'my-class'});
		dlg.result.then(function(name){
            console.log("dlg.result");
			//$scope.name = name;
		},function(){
			//if(angular.equals($scope.name,''))
				//$scope.name = 'You did not enter in your name!';
		});*/
 	

    	/*console.log("exporting data inside exportUTMTestSet function...."); 
		
				
		var testsetValue=$rootScope.modeltestset;     
        
        var exporturl = "/utm-service/testset_export/exportMDTTestSet";
                
        exportService.exportToUTMTestSet(testsetValue, exporturl)
        .then(function(results) {
        	
        	 var sets=results.data;
        	 console.log("Sets value"+sets);            
             var headerValue=results.headers;
             var fileName="UTMTestSet";
             //console.log("Filename Server value"+fileName);
	  		 var hiddenElement = document.createElement('a');	
	  		 var blob = new Blob([sets], {type: "application/vnd.ms-excel"});
	  	     var objectUrl = URL.createObjectURL(blob);
    	  	//alert("EXCEL Format");
        	 hiddenElement.href = objectUrl;
        	 hiddenElement.download = fileName; 
        	 hiddenElement.target = '_blank';
        	 hiddenElement.click();   	
        },
        function(data) {
        	//alert("File upload failed and parameters not returned");
        });
    };*/
    
}]);
