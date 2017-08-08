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

app.controller('ImportSchemaCtrl', ['$scope', '$rootScope','$modalInstance','data','svnservice', 'fileUpload','dialogs', function($scope,$rootScope,$modalInstance,data,svnservice,fileUpload,dialogs){
	console.log("//////ImportSchemaCtrl");
    $rootScope.serviceInfo;
	$rootScope.serviceInput;
	$rootScope.serviceOutput;
	$rootScope.serviceFault;
	$rootScope.serviceInputPartInfo;
	$rootScope.schemElemant1;
	
	$rootScope.updateServiceInfo;
	$rootScope.updateServiceInput;
	$rootScope.updateServiceOutput;
	$rootScope.updateServiceFault;
	$rootScope.updateServiceInputPartInfo;
	$rootScope.updateSchemElemant1;
    

// Below code is added to get the policyNames
	for ( var polElement in elementMap) {
		if (polElement.indexOf('Policy_') === 0) {
			var obj = elementMap[polElement];
			if (!($.isEmptyObject(obj))) {
				allPolicies = jQuery.extend({}, obj);
				$scope.policyNames = [];
				for ( var policy in allPolicies) {
					$scope.policyNames.push(policy);
				}
			}
			break;
		}
	}
     
    setTimeout(function(){
    console.log("setTimeout");
    setMultiSelect(); }, 100);
	
	$scope.init = function() {
        console.log("init");
        $scope.schemaLocation = 'svn://svnrepo:3690';
		$scope.upgrade_schemaLocation = 'svn://svnrepo:3690';
		$scope.userID = 'user_id';
		$scope.password = 'password';
	};
	
	$scope.init();
	 

	$scope.close = function(){
    console.log("close");		
		$modalInstance.close("closed");
	};
	$rootScope.file_path;
	
	
	$scope.importSchema= function(){
    console.log("importSchema");		
		isImportSchema = true;
		var file=$rootScope.file_path; 
		//alert("file:"+schemaFile);
        //console.log('file is ' + JSON.stringify(file)); 
        var userID = document.getElementById("userID").value;
        var password = document.getElementById("password").value;        
        var svnURL = document.getElementById("schemaLocation").value;
        var schemaLocation = document.getElementById("schemaLocation").value;
        
        if( schemaLocation &&  userID && password && document.getElementById("schemaLocation").disabled== false)
        {
        	$scope.schemaLocation=schemaLocation;
        	$scope.userID=userID;
        	$scope.password=password;

        	document.getElementById("fileUpload").disabled = true;	
            
        	var svnUploadURL = "/utm-service/schema_upload/svnUploadWSDL";
          
        	svnservice.SVNToUrl(schemaLocation, userID, password,svnURL,svnUploadURL)
        		.then(function(pars) {
                    console.log("pars");
        			document.getElementById('Upgrade Schema Version').classList.remove('ThisLink');
        			document.getElementById('Set Default Values').classList.remove('ThisLink');
        			$rootScope.wsdlInfo = angular.fromJson(pars);
        			$rootScope.serviceInfo =  $rootScope.wsdlInfo.serviceInfo;
        			serviceName = $rootScope.serviceInfo.service.name;
        			$rootScope.schemaLocation=$rootScope.wsdlInfo.schemaLocation;
        			$rootScope.serviceInput = $rootScope.serviceInfo.bindingInfo.portTypeInfo.operationInfo[0].inputMessage;
        			$rootScope.serviceInputPartInfo = $rootScope.serviceInfo.bindingInfo.portTypeInfo.operationInfo[0].inputMessage.partInfo;
        			
        			$rootScope.serviceOutput = $rootScope.serviceInfo.bindingInfo.portTypeInfo.operationInfo[0].outputMessage;
                	$rootScope.serviceOutputPartInfo = $rootScope.serviceInfo.bindingInfo.portTypeInfo.operationInfo[0].outputMessage.partInfo;
                	
                	$rootScope.servicefault = $rootScope.serviceInfo.bindingInfo.portTypeInfo.operationInfo[0].faultMessage;
                	$rootScope.servicefaultPartInfo = $rootScope.serviceInfo.bindingInfo.portTypeInfo.operationInfo[0].faultMessage.partInfo;
        			
        			//alert("serviceInputPartInfo :: " + JSON.stringify($rootScope.serviceInputPartInfo));
                	
                	$rootScope.inputSchemaServiceInputPartInfo=[];
                	$rootScope.inputSchemaServiceOutputPartInfo=[];
                	$rootScope.inputSchemaServicefaultPartInfo=[];
                	
                	
                	 
                     angular.copy($rootScope.serviceInputPartInfo, $rootScope.inputSchemaServiceInputPartInfo);
                     
                     angular.copy($rootScope.serviceOutputPartInfo, $rootScope.inputSchemaServiceOutputPartInfo);
                     
                     angular.copy($rootScope.servicefaultPartInfo, $rootScope.inputSchemaServicefaultPartInfo);
                     
        			$rootScope.isModel = true;
        		},
        		function(data) {
                    console.log("data");
        			//alert("File upload failed and parameters not returned");
        		});
        } else  {
        	var uploadUrl = "/utm-service/schema_upload/uploadWSDL";
            
            fileUpload.uploadFileToUrl(file, uploadUrl)
            .then(function(pars) {
                console.log("pars");
            	document.getElementById('Upgrade Schema Version').classList.remove('ThisLink');
            	document.getElementById('Set Default Values').classList.remove('ThisLink');
            	//document.getElementById('Define/Modify Schema').classList.remove('ThisLink');
              	$rootScope.wsdlInfo = angular.fromJson(pars);
            	$rootScope.serviceInfo =  $rootScope.wsdlInfo.serviceInfo;
            	serviceName = $rootScope.serviceInfo.service.name;
            	
            	$rootScope.serviceInput = $rootScope.serviceInfo.bindingInfo.portTypeInfo.operationInfo[0].inputMessage;
            	$rootScope.serviceInputPartInfo = $rootScope.serviceInfo.bindingInfo.portTypeInfo.operationInfo[0].inputMessage.partInfo;
            	
            	//alert("Input Part Info :: " + JSON.stringify($rootScope.serviceInputPartInfo));
            	//alert("Input Part 1 Info :: " + JSON.stringify($rootScope.serviceInputPartInfo[1]));
            	
            	//alert("Input Element :: " + JSON.stringify($rootScope.serviceInputPartInfo[1].schemaElements[1].elements[0]));
            	
            	$rootScope.serviceOutput = $rootScope.serviceInfo.bindingInfo.portTypeInfo.operationInfo[0].outputMessage;
            	$rootScope.serviceOutputPartInfo = $rootScope.serviceInfo.bindingInfo.portTypeInfo.operationInfo[0].outputMessage.partInfo;
            	
            	$rootScope.servicefault = $rootScope.serviceInfo.bindingInfo.portTypeInfo.operationInfo[0].faultMessage;
            	$rootScope.servicefaultPartInfo = $rootScope.serviceInfo.bindingInfo.portTypeInfo.operationInfo[0].faultMessage.partInfo;
            	
            	//alert("servicefaultPartInfo :: " + JSON.stringify($rootScope.servicefaultPartInfo));
            	
            	$rootScope.inputSchemaServiceInputPartInfo=[];
            	$rootScope.inputSchemaServiceOutputPartInfo=[];
            	$rootScope.inputSchemaServicefaultPartInfo=[];
            	
            	
            	 
                 angular.copy($rootScope.serviceInputPartInfo, $rootScope.inputSchemaServiceInputPartInfo);
                 
                 angular.copy($rootScope.serviceOutputPartInfo, $rootScope.inputSchemaServiceOutputPartInfo);
                 
                 angular.copy($rootScope.servicefaultPartInfo, $rootScope.inputSchemaServicefaultPartInfo);
            
            	$rootScope.isModel = true;            	
	        },
            function(data) {
                console.log("data");
            	
            });
        }
        
       
        
        
        
		$modalInstance.close("closed");
	};
	
	 $scope.setFile = function(element) {
        console.log("setFile");
		 
         $scope.$apply(function($scope) {
            console.log("apply");
             $scope.theFile = element.files[0];
             $rootScope.fileName =$scope.theFile.name;
            var file =element.files[0]; 
            $rootScope.file_path = file;
        
     		//$modalInstance.close("closed");
            
             angular.element(document.getElementById('fileUpload')).val(null);
             	
         });
     };
     
     $scope.setUpgradeFile = function(element) {
		 console.log("setUpgradeFile");
         $scope.$apply(function($scope) {
            console.log("apply");
             $scope.theUpgradeFile = element.files[0];
             $rootScope.upgradeFileName =$scope.theUpgradeFile.name;
            //alert("fname1"+$rootScope.upgradeFileName);
            var file =element.files[0]; 
            $rootScope.file_path = file;
        
     		//$modalInstance.close("closed");
            
             angular.element(document.getElementById('fileUpload')).val(null);
             	
         });
     };
     
     
     
     $scope.reset = function(){
        console.log("reset");
    	 document.getElementById("fileUpload").disabled = false;
    	 document.getElementById("schemaLocation").disabled = false;
    	 document.getElementById("userID").disabled = false;
    	 document.getElementById("password").disabled = false;
    	 
    	 document.getElementById("schemaLocation").value='';
         
         document.getElementById("userID").value='';
         document.getElementById("password").value='';
         $scope.theFile = null;
         angular.element(document.getElementById('fileUpload')).val(null);
       
    	 
     };
     
     $scope.upgradeSchema = function(){
        console.log("upgradeSchema");
    	 //alert("inside upgrade schema");
 		 var file=$rootScope.file_path; 
 		//alert("file:"+schemaFile);
         //console.log('file is ' + JSON.stringify(file)); 
         var userID = document.getElementById("userID").value;
         var password = document.getElementById("password").value;
         var schemaLocation = document.getElementById("upgradeSchemaLocation").value;
         var svnURL = document.getElementById("upgradeSchemaLocation").value;
         console.log("after");
         $rootScope.Currentmappedvalues = [];
         if( schemaLocation &&  userID && password && document.getElementById("upgradeSchemaLocation").disabled== false)
         {
         	$scope.schemaLocation=schemaLocation;
         	$scope.userID=userID;
         	$scope.password=password;
         	
         	document.getElementById("fileUpload").disabled = true;	
             
         	var svnUploadURL = "/utm-service/schema_upload/svnUploadWSDL";
           
         	svnservice.SVNToUrl(schemaLocation, userID, password,svnURL,svnUploadURL)
         		.then(function(pars) {
                    console.log("pars");
         			$rootScope.updateWsdlInfo = angular.fromJson(pars);
         			$rootScope.updateServiceInfo =  $rootScope.updateWsdlInfo.serviceInfo;
         			$rootScope.schemaLocation=$rootScope.updateWsdlInfo.schemaLocation;
         			$rootScope.updateServiceInput = $rootScope.updateServiceInfo.bindingInfo.portTypeInfo.operationInfo[0].inputMessage;
         			$rootScope.updateServiceInputPartInfo = $rootScope.updateServiceInfo.bindingInfo.portTypeInfo.operationInfo[0].inputMessage.partInfo;
         			
         			$rootScope.updateServiceOutput = $rootScope.updateServiceInfo.bindingInfo.portTypeInfo.operationInfo[0].outputMessage;
                 	$rootScope.updateServiceOutputPartInfo = $rootScope.updateServiceInfo.bindingInfo.portTypeInfo.operationInfo[0].outputMessage.partInfo;
                 	
                 	$rootScope.updateServicefault = $rootScope.updateServiceInfo.bindingInfo.portTypeInfo.operationInfo[0].faultMessage;
                 	$rootScope.updateServicefaultPartInfo = $rootScope.updateServiceInfo.bindingInfo.portTypeInfo.operationInfo[0].faultMessage.partInfo;
         			
         			//alert("serviceInputPartInfo :: " + JSON.stringify($rootScope.serviceInputPartInfo));
         
         			//$rootScope.isModel = true;
         		},
         		function(data) {
                    console.log("data");
         			//alert("File upload failed and parameters not returned");
         		});
         } else  {
         	var uploadUrl = "/utm-service/schema_upload/uploadWSDL";
             
             fileUpload.uploadFileToUrl(file, uploadUrl)
             .then(function(pars) {
                console.log("pars");
             	
               	$rootScope.updateWsdlInfo = angular.fromJson(pars);
               //	alert("wsdlinfo:"+$rootScope.updateWsdlInfo);
             	$rootScope.updateServiceInfo =  $rootScope.updateWsdlInfo.serviceInfo;
             	
             	$rootScope.updateServiceInput = $rootScope.updateServiceInfo.bindingInfo.portTypeInfo.operationInfo[0].inputMessage;
             	$rootScope.updateServiceInputPartInfo = $rootScope.updateServiceInfo.bindingInfo.portTypeInfo.operationInfo[0].inputMessage.partInfo;
             	
             	$rootScope.updateServiceOutput = $rootScope.updateServiceInfo.bindingInfo.portTypeInfo.operationInfo[0].outputMessage;
             	$rootScope.updateServiceOutputPartInfo = $rootScope.updateServiceInfo.bindingInfo.portTypeInfo.operationInfo[0].outputMessage.partInfo;
             	
             	$rootScope.updateServicefault = $rootScope.updateServiceInfo.bindingInfo.portTypeInfo.operationInfo[0].faultMessage;
             	$rootScope.updateServicefaultPartInfo = $rootScope.updateServiceInfo.bindingInfo.portTypeInfo.operationInfo[0].faultMessage.partInfo;
             	
             	//alert("servicefaultPartInfo :: " + JSON.stringify($rootScope.servicefaultPartInfo));
             	
             
             	//$rootScope.isModel = true;            	
 	        },
             function(data) {
                console.log("data");
             	
             });
         }
        
 		$modalInstance.close("closed");
 		
 		var dlg = dialogs.create('partials/portfolios/upgrade_schema_dtls.html','UpgradeSchemaCtrl',{},{size:'xlg',keyboard: true,backdrop: true,windowClass: 'my-class'});
		dlg.result.then(function(name){
            console.log("dlg.result");
			//$scope.name = name;
		},function(){
			//if(angular.equals($scope.name,''))
				//$scope.name = 'You did not enter in your name!';
		});
 	
    	 
     };
}]);