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

function AutosaveProject($scope,$rootScope,$resource, $http, $timeout, $location, $interval, $q, Datafactory) 
{	
    console.log("//////////Autosaveproject");	
	$scope.saveProjectToUrl = function(UTMMDTProject,svnUploadURL){
	    console.log("saveProjectURL");   
    	console.log('Autosaveproject Enter == ' + new Date().getTime());
    	console.log(new Date());
    	var def = $q.defer();
    	
        $http.post(svnUploadURL, UTMMDTProject)
        .success(function(data){ 
        console.log("success");       	
        	def.resolve(data);
        })
        .error(function(data){ 
        console.log("error");      	 	      
       	 	def.reject("Autosave unsuccessful");
        });
        
        return def.promise;        
       
    };
	
	$scope.autosave = function(){
		console.log("autosave");
		
		var saveProjectURL = "/utm-service/project_administration/autoSaveProject";
       	
		var UTMProjectExplorer = {};
		UTMProjectExplorer.projectName = $rootScope.projectName;
		UTMProjectExplorer.utmModels = $rootScope.utmModels;
		UTMProjectExplorer.revision = autoSaveRevision;
		UTMProjectExplorer.utmDepthValueMap = $rootScope.depthElementKeyMap;
		UTMProjectExplorer.utmCountValueMap = $rootScope.countElementKeyMap;

		var utm_models = [];
		$rootScope.populateUTMModelArray(utm_models,$rootScope.utmModels);	        	
										
		var UTMMDTProject = {};
		UTMMDTProject.utmProjectExplorer = UTMProjectExplorer;
		UTMMDTProject.utmModels = utm_models;		
		UTMMDTProject.almqcdata = $rootScope.almqcData;		
		UTMMDTProject.wsdlInfo = null;
		UTMMDTProject.schemaLocation = null;
		
		if ($rootScope.wsdlInfo != null) {
			UTMMDTProject.schemaLocation = $rootScope.wsdlInfo.schemaLocation;
			UTMMDTProject.oldSchemaLocation = $rootScope.wsdlInfo.oldSchemaLocation;
			UTMMDTProject.schemaUpgradedFlag = $rootScope.wsdlInfo.schemaUpgradedFlag;
		}

		UTMMDTProject.revision=$rootScope.revision;
		console.log(UTMMDTProject.revision);
		
		UTMMDTProject.checkOutPath=$rootScope.checkOutPath;
		UTMMDTProject.oldNewModelNameMap = old_new_model_name;		
		UTMMDTProject.automationDetails={};
		
		UTMMDTProject.automationDetails.projectPreferenceInfo =  Datafactory.getProjectPreferenceInfo();
		UTMMDTProject.automationDetails.environmentData =  $rootScope.environmentData;
		UTMMDTProject.automationDetails.pythonScriptList =  Datafactory.getCommonPythonScriptList();
		UTMMDTProject.automationDetails.dbToolRequestList =  Datafactory.getDbToolProjectLevelList();					
		UTMMDTProject.automationDetails.runtimePythonScriptList =  Datafactory.getRuntimePythonScriptProjectLevelList();
		UTMMDTProject.automationDetails.xmlValidatorList =  Datafactory.getXmlValidatorList();
		UTMMDTProject.automationDetails.fileWriterList =  Datafactory.getWriteFileDataList();
		UTMMDTProject.automationDetails.fileStreamWriterList =  Datafactory.getFileStreamWriterList();
		
		
		
		if($rootScope.openedProject != null){
			console.log('opened project...')			
			var existingData = JSON.stringify($rootScope.openedProject);
			var currentgData = JSON.stringify(UTMMDTProject);			
			if(angular.equals(existingData, currentgData)){
				//do nothing
				console.log('No changes found.');
			}else{
				console.log('Changes found.');
				$scope.saveProjectToUrl(UTMMDTProject,saveProjectURL)
				.then(function(pars) {	
				console.log("");				
				},
				function(data) {
					
				});
			}
			
		}else{
			console.log('autosaving project...')
			$scope.saveProjectToUrl(UTMMDTProject,saveProjectURL)
			.then(function(pars) {
				
			},
			function(data) {
				
			});
		}		
		
	}
	
	$scope.autosave();
}
