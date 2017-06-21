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

app.controller('fileUploadCtrl', ['$scope', '$rootScope','fileUpload', function($scope, $rootScope,fileUpload){
    console.log("///////////fileUploadCtrl");
	$rootScope.isAllOption = false;
	
    $scope.uploadSchemaFile = function(element){
        console.log("uploadSchemaFile");
    	  $scope.$apply(function($scope) {
            console.log("apply");
    	$rootScope.isStatic = true;    	
    	$rootScope.isAllOption = true;    	
    	$scope.requiredval= true;    	
    	$rootScope.rightTabName ="UTM Build Configuration";
    	$rootScope.testSet = null;
    	$scope.parameters = null;    	
    	$scope.constraints =null;
    	$scope.relations =null;
    	if($rootScope.isStatic == true){
			document.getElementById('buidConfigBtn').style.visibility  = "hidden";
		}
    	
        var file = element.files[0];/*$scope.requestSchemaFile;*/
        console.log('file is ' + JSON.stringify(file));        
        $rootScope.file_type="Schema";
        
        var uploadUrl = "/utm-service/schema_upload/uploadSchema";
        
    	//alert("uploadFile file::"+file + " :: " + uploadUrl);
        
        fileUpload.uploadFileToUrl(file, uploadUrl)
        .then(function(pars) {
            console.log("uploadFileToUrl");
        	
        	$rootScope.SUT =  pars;
        	console.log("file name :"+pars.name);
        	$scope.fileName = pars.name;
        	$scope.parameters = pars.parameters;
        	
        	var param = pars.parameters;
        	
        	var paramarray = pars.parameters;
        	var parArray1=[];
        	for(var i=0;i<paramarray.length;i++){
        		parArray1.push(paramarray[i].required);
        	}
        	
        	$scope.parArray=parArray1;
        	$scope.constraints =pars.constraints;
        	$scope.relations =pars.relations;
        	var  con  = $scope.constraints;
        	$scope.required=pars.required;
		$scope.required='Required Only';
        },
        function(data) {
            console.log("data");
        	//alert("File upload failed and parameters not returned");
        });
        
        
        angular.forEach(
        	    angular.element("input[type='file']"),
        	    function(inputElem) {
                    console.log("inputElem");
        	      angular.element(inputElem).val(null);
        	    });
        
    	  });
    };
    
//-----For Required Radio button functionality
    
 

    $scope.requiredonly= function(){
        console.log("requiredonly");
    	//var tempArray = $rootScope.SUT;
    	//var tempParam = tempArray.parameters;
    	//alert("testParam.length:"+tempParam.length);
    	var parameter=$scope.parArray;

    	
    	var param=$scope.parameters; 
    	
      	var i=0;

    	$('.req').each(function(){
            console.log(".req");
    			    var newID='requiredval'+i;
    			    //jQuery(this).prev("req").attr("requiredval", "newID");
    			    //$(this).attr("requiredval","newID");
    			    //var newval=$(this).val(newID);
    			    var newval=$(this).attr('id',newID);
    			    console.log("Angular id: "+newval);
    			    if(i<param.length){
    			    	document.getElementById(newID).disabled=false;
    			    if (parameter[i]){
    			    	param[i].required=parameter[i];
    					//document.getElementById(newID).disabled=true;
    					document.getElementById(newID).checked=true;
    					}
    					else{
    					param[i].required=parameter[i];	
    					//document.getElementById(newID).disabled=false;
    					document.getElementById(newID).checked=false;
    				}
    			    }
    			    i++;
    			});
    };
    

    $scope.allrequired= function(){
        console.log("allrequired");
      	var param=$scope.parameters;
      	var i=0;
      	$('.req').each(function(){
            console.log("req");
      		var newID='requiredval'+i;
			    //jQuery(this).prev("req").attr("requiredval", "newID");
			    //$(this).attr("requiredval","newID");
			    //var newval=$(this).val(newID);
			    var newval=$(this).attr('id',newID);
			    console.log("Angular id: "+newval);
			    if(i<param.length){
			    	param[i].required=true;
			    	document.getElementById(newID).checked=true;
			    	document.getElementById(newID).disabled=true;
		    }
		    i++;
		});

      };

    
$scope.uploadSUTFile = function(element){
console.log("uploadSUTFile");	
	$scope.$apply(function($scope) {
        console.log("apply");
	     $rootScope.isAllOption = false;
	     $rootScope.isStatic = true;	    
	     $scope.requiredval= false;	     
	     $rootScope.rightTabName ="UTM Build Configuration";
		 $rootScope.testSet = null;
		 
		if($rootScope.isStatic == true){
			document.getElementById('buidConfigBtn').style.visibility  = "hidden";
		}
        var file = element.files[0];/*$scope.requestFile;*/
        
        console.log('file is ' + JSON.stringify(file));
        
        $rootScope.file_type="SUT";
        
        var uploadUrl = "/utm-service/sut_upload/uploadSUT";
        
        fileUpload.uploadFileToUrl(file, uploadUrl)
        .then(function(pars) {
            console.log("uploadFileToUrl");
        	$rootScope.SUT =  pars;
        	console.log("file name :"+pars.name);
        	$scope.fileName = pars.name;
        	$scope.parameters = pars.parameters;
        	
        	$scope.constraints =pars.constraints;
        	$scope.relations =pars.relations;
        	var  con  = $scope.constraints;
        },
        function(data) {
            console.log("data");
        	//alert("File upload failed and parameters not returned");
        });
        angular.forEach(
        	    angular.element("input[type='file']"),
        	    function(inputElem) {
                    console.log("inputElem");
        	      angular.element(inputElem).val(null);
        	    });
        
    	  
        
	});
    };
    
    $scope.buildConfig = function(){
    console.log("buildConfig");    	
    	$rootScope.isStatic = true;    	
    	$rootScope.rightTabName ="UTM Build Configuration";
    	document.getElementById('buidConfigBtn').style.visibility  = "hidden";
    	$rootScope.testset = null;
    		
    	
    };
    
  /*  $scope.close = function(){
    	$modalInstance.close('closed');
    };
    
    $scope.importSchema= function(){	
    	 var file = $scope.requestFile;
         console.log('file is ' + JSON.stringify(file));
         var uploadUrl = "/utm-service/schema_upload/uploadSchema";
         fileUpload.uploadFileToUrl(file, uploadUrl)
         .then(function(pars) {         	
         	$rootScope.SUT =  pars;
         	console.log("file name :"+pars.name);
         	$scope.fileName = pars.name;
         	$scope.parameters = pars.parameters;         	
         	var param = pars.parameters;
         },
         function(data) {
         	//alert("File upload failed and parameters not returned");
         });

		
    };	*/
    


    
    
}]);

function clearFileInput(id) 
{ 
    console.log("clearFileInput");
    var oldInput = document.getElementById(id); 

    var newInput = document.createElement("input"); 

    newInput.type = "file"; 
    newInput.id = oldInput.id; 
    newInput.name = oldInput.name; 
    newInput.className = oldInput.className; 
    newInput.style.cssText = oldInput.style.cssText; 
    // TODO: copy any other relevant attributes 

    oldInput.parentNode.replaceChild(newInput, oldInput); 
}

