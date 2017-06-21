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

'use strict';

/* App Module */

 
var app = angular.module('clds-app', ['ngRoute', 
                                          'ngResource',
                                          'angularjs-dropdown-multiselect',
                                          'angularjs-dropdown-multiselect-new',
                                          'hljs',
                                          'ui.bootstrap',
                                          'angular-loading-bar', 
                                          'ngAnimate',
                                          'dialogs.main',
                                          'ui.grid', 
                                          'ui.grid.resizeColumns',
                                          'ui.grid.paging',
                                          'ui.grid.selection',
                                          'ui.grid.cellNav',
                                          'ui.grid.pinning',
                                          'ngSanitize',
                                          'ngCookies',
                                          'ui.bootstrap.modal',
                                          'ui.grid.exporter',
                                          'angucomplete',
                                          'kendo.directives'
                                          ])
.config(['cfpLoadingBarProvider', function(cfpLoadingBarProvider) 
{
	console.log("///////////////cfpLoadingBarProvider");
    cfpLoadingBarProvider.includeBar = true;
    cfpLoadingBarProvider.includeSpinner = true;
  }])
.config(function ($httpProvider) {
	console.log("config");
	  $httpProvider.responseInterceptors.push('myHttpInterceptor');

	  var spinnerFunction = function spinnerFunction(data, headersGetter) 
	  {
	  	console.log("spinnerFunction");
		return data;
	  };

	  $httpProvider.defaults.transformRequest.push(spinnerFunction);
})
.config(['$routeProvider','$locationProvider', '$compileProvider','cfpLoadingBarProvider',function(
				$routeProvider, 
				$locationProvider,
				cfpLoadingBarProvider,
				$timeout,
				dialogs,
				$cookies) 
{
	console.log("$routeProvider','$locationProvider', '$compileProvider','cfpLoadingBarProvider'")
  $locationProvider.html5Mode(false);
  //alert("App.js");
     
	$routeProvider.
	when('/otherwise', {templateUrl: 'partials/please_wait.html', controller: QueryParamsHandlerCtrl }).
	//when('/dashboard_submit', { templateUrl: 'partials/portfolios/dashboard_submit.html', controller: CreateNewPrjCtrl }).
	when('/dashboard', { templateUrl: 'partials/portfolios/clds_modelling.html', controller: DashboardCtrl }).
	//when('/dashboard_upload', { templateUrl: 'partials/portfolios/dashboard_upload.html', controller: DashboardCtrl }).
	when('/activity_modelling', { templateUrl: 'partials/portfolios/clds_modelling.html', controller: DashboardCtrl }).
	when('/authenticate', { templateUrl: 'authenticate.html', controller: AuthenticateCtrl }).
	when('/invalidlogin', { templateUrl: 'partials/invalid_login.html', controller: PageUnderConstructionCtrl }).
	otherwise({redirectTo: '/otherwise'});
  
}]).controller('dialogCtrl',function($scope,$rootScope,$timeout,dialogs){
	
	//-- Variables --//
	console.log("dialogCtrl");
	$scope.lang = 'en-US';
	$scope.language = 'English';

	var _progress = 100;
	
	$scope.name = '';
	$scope.confirmed = 'No confirmation yet!';
	
	$scope.custom = {
		val: 'Initial Value'
	};
	
	//-- Listeners & Watchers --//

	$scope.$watch('lang',function(val,old){
		console.log("lang");
		switch(val){
			case 'en-US':
				$scope.language = 'English';
				break;
			case 'es':
				$scope.language = 'Spanish';
				break;
		}
	});

	//-- Methods --//
$rootScope.testCaseRequirements=[];
$rootScope.validTestRequirements=[];
/*$rootScope.testCaseValue=[];*/
	$scope.setLanguage = function(lang)
	{
		console.log("setLanguage");

		$scope.lang = lang;
		$translate.use(lang);
	};

	$rootScope.launch = function(which){
		console.log("launch");
		switch(which){
			case 'error':
				dialogs.error();
				break;
			case 'wait':
				//var dlg = dialogs.wait(undefined,undefined,_progress);
				//_fakeWaitProgress();
				break;
			case 'customwait':
				//var dlg = dialogs.wait('Custom Wait Header','Custom Wait Message',_progress);
				//_fakeWaitProgress();
				break;
			case 'notify':
				dialogs.notify();
				break;
			case 'confirm':
				var dlg = dialogs.confirm();
				dlg.result.then(function(btn){
					console.log("dlg.result");
					$scope.confirmed = 'You confirmed "Yes."';
				},function(btn){
					console.log("btn");
					$scope.confirmed = 'You confirmed "No."';
				});
				break;
			case 'custom':
				var dlg = dialogs.create('/dialogs/custom.html','customDialogCtrl',{},{size:'lg',keyboard: true,backdrop: 'static',windowClass: 'my-class'});
				dlg.result.then(function(name){
					console.log("dlg.result");
					$scope.name = name;
				},function(){
					console.log("custom");
					if(angular.equals($scope.name,''))
						$scope.name = 'You did not enter in your name!';
				});
				break;
			case 'custom2':
				var dlg = dialogs.create('/dialogs/custom2.html','customDialogCtrl2',$scope.custom,{size:'lg'});
				break;
			case 'custom3':
				var dlg = dialogs.notify('Message','All is not supported, Please select interface(s)/version(s) to fetch real time federated coverage report.');
				break;
			case 'custom4':
				var dlg = dialogs.confirm('Message','You are about to fetch real time federated coverage report.This may take sometime!!!.');
				dlg.result.then(function(btn){
					console.log("dlg.result");
					$scope.confirmed = 'You confirmed "Yes."';
				},function(btn){
					console.log("btn");
					$scope.confirmed = 'You confirmed "No."';
				});
				break;
			case 'custom5':
				var dlg = dialogs.notify('Success','Request has been successfully processed.');
				break;
			case 'custom6':
				var dlg = dialogs.notify('Message','Please type Testscenario Name');
				break;
		}
	}; // end launch
	
	var _fakeWaitProgress = function()
	{
		console.log("_fakeWaitProgress");
		$timeout(function()
		{
			console.log("timeout");
			if(_progress < 100)
			{
				_progress += 33;
				$rootScope.$broadcast('dialogs.wait.progress',{'progress' : _progress});
				_fakeWaitProgress();
			}
			else
			{
				$rootScope.$broadcast('dialogs.wait.complete');
				_progress = 0;
			}
		},1000);
	};
}).controller('MenuCtrl',['$scope', '$rootScope','$timeout','dialogs','$location','MenuService','Datafactory',
'userPreferencesService','cldsModelService','cldsTemplateService',function($scope,$rootScope,$timeout,dialogs,
	$location, MenuService,Datafactory,userPreferencesService,cldsModelService,cldsTemplateService)
{	
	console.log("MenuCtrl");
	$rootScope.screenName="Universal Test Modeler";
	$rootScope.testSet = null;
	$rootScope.isNew= false;
	var testingType="";
	$rootScope.contactUs= function()
	{
		console.log("contactUs");
		var link = "mailto:onap-discuss@lists.onap.org?subject=CLAMP&body=Please send us suggestions or feature enhancements or defect. If possible, please send us the steps to replicate any defect.";
		window.location.href = link;
	};
	
$scope.emptyMenuClick = function(value, name) {
    if ($rootScope.isNew && (name != "Save Template" && name != "Close Template" &&
            name != "Template Properties")) {
        saveConfirmationNotificationPopUp();
    } else if ($rootScope.isNewClosed && name != "Save CL" && name != "Close Model" &&
        name != "Properties CL") {
        saveConfirmationNotificationPopUp();
    } else {
        isSaveCheck(name);
    }

    function saveConfirmationNotificationPopUp() {
        $scope.saveConfirmationNotificationPopUp(function(data) {
            console.log("saveConfirmationNotificationPopUp");
            if (data) {
                if ($rootScope.isNewClosed) {
                    isSaveCheck("Save CL");
                } else {
                    isSaveCheck("Save Template");
                }
                $rootScope.isNewClosed = false;
                $rootScope.isNew = false;
            } else {
                return false;
            }
        });
    }

    function isSaveCheck(name) {
        console.log("isSaveCheck");
        if (name == "Wiki") {
            window.open(value);
        } else if (name == "Contact Us") {
            $rootScope.contactUs();
        } else if (name == "Revert Template Changes") {
            $scope.cldsRevertTemplate();
        } else if (name == "Revert Model Changes") {
            $scope.cldsRevertModel();
        } else if (name == "Create Template") {
            $rootScope.isNew = true;
            $scope.cldsCreateTemplate();
        } else if (name == "Open Template") {
            $scope.cldsOpenTemplate();
        } else if (name == "Save Template") {
            $scope.cldsTemplatePerformAction("SAVE");
            $rootScope.isNewClosed = false;
            $rootScope.isNew = false;
        } else if (name == "Template Properties") {
            $scope.cldsOpenTemplateProperties();
        } else if (name == "Close Model" || name == "Close Template") {
            $scope.cldsClose();
        } else if (name == "Refresh ASDC") {
            $scope.cldsRefreshASDC();
        } else if (name == "Create CL") {
            $rootScope.isNewClosed = true;
            $scope.cldsCreateModel();
        } else if (name == "Open CL") {
            $scope.cldsOpenModel();
        } else if (name == "Save CL") {
            $rootScope.isNewClosed = false;
            $rootScope.isNew = false;
            $scope.cldsPerformAction("SAVE");
        } else if (name == "Test") {
            $scope.cldsPerformAction("TEST");
        } else if (name == "Submit") {
            $scope.cldsConfirmPerformAction("SUBMIT");
        } else if (name == "Resubmit") {
            $scope.cldsConfirmPerformAction("RESUBMIT");
        } else if (name == "Update") {
            $scope.cldsConfirmPerformAction("UPDATE");
        } else if (name == "Delete") {
            $scope.cldsConfirmPerformAction("DELETE");
        } else if (name == "Stop") {
            $scope.cldsConfirmPerformAction("STOP");
        } else if (name == "Restart") {
            $scope.cldsConfirmPerformAction("RESTART");
        } else if (name == "Refresh Status") {
            $scope.cldsPerformAction("REFRESH");
        } else if (name == "Properties CL") {
            $scope.cldsOpenModelProperties();
        } else if (name === "Running Instances") {
            $scope.cldsShowRunningInstances();
        } else {
            $rootScope.screenName = name;
            $scope.updatebreadcrumb(value);
            $location.path(value);
        }
    }
};

	
	
	
	$rootScope.impAlerts= function()
	{
		console.log("impAlerts");
	};
	
	$scope.tabs = {		
					"Template": [
					     {
					    	 link: "/cldsCreateTemplate",
				             name: "Create Template"
					     }, {
						     link: "/cldsOpenTemplate",
						     name: "Open Template"
			             }, {
			                link: "/cldsSaveTemplate",
			                name: "Save Template"		           
					     },
					     {
					    	 link: "/cldsOpenTemplateProperties",
				             name: "Template Properties"
					     },
					     {
					    	 link: "/RevertChanges",
				             name: "Revert Template Changes"
					     },
					     {
					    	 link: "/Close",
				             name: "Close Template"
					     }
					 ],
			            
			         "Closed Loop": [
					     {
					    	 link: "/cldsCreateModel",
				             name: "Create CL"
					     }, {
						     link: "/cldsOpenModel",
						     name: "Open CL"
			             }, {
			                link: "/cldsSaveModel",
			                name: "Save CL"		           
					     },
					     {
					    	 link: "/cldsOpenModelProperties",
				             name: "Properties CL"
					     },
					     {
					    	 link: "/RevertChanges",
				             name: "Revert Model Changes"
					     },
					     {
					    	 link: "/Close",
				             name: "Close Model"
					     }
					 ],    
			         "Manage": [
						 {
						     link: "/cldsTestActivate",
						     name: "Test"
			            },
			            {
			                link: "/cldsSubmit",
			                name: "Submit"		           
					     },
			             {
			                link: "/cldsResubmit",
			                name: "Resubmit"		           
					     },
			             {
			                link: "/cldsUpdate",
			                name: "Update"		           
					     },
			             {
			                link: "/cldsStop",
			                name: "Stop"		           
					     },
			             {
			                link: "/cldsRestart",
			                name: "Restart"		           
					     },
			             {
			                link: "/cldsDelete",
			                name: "Delete"		           
					     }
					 ],   
					 "View": [
						{
						     link: "/refreshStatus",
						     name: "Refresh Status"
			            },
			            {
			            	link:"/refreshASDCProperties",
			            	name:"Refresh ASDC"
			            },
			            {
			            	link:"/viewRunningInstances",
			            	name:"Running Instances"
			            }
							 ],   
			         "Help": [
			            {
			                link: "http://wiki.onap.org",
			                name: "Wiki"
			            }, {
			                link: "/contact_us",
			                name: "Contact Us"		           
			            }
			            
			            
			         ]
        };

	
        if (!Object.keys) 
        {
            Object.keys = function(obj) 
            {
            	console.log("keys");
            	console.log("keys");
                var keys = [];

                for (var i in obj) {
                    if (obj.hasOwnProperty(i)) {
                        keys.push(i);
                    }
                }

                return keys;
            };
            $scope.keyList = Object.keys($scope.tabs);
        } else
        {
            $scope.keyList = Object.keys($scope.tabs);
        }
        
        $scope.updatebreadcrumb = function(path)
        {     
        	console.log("updatebreadcrumb");
        	var currentURL = $location.path();        	
        	if(path!=undefined)
        	{
        	 currentURL = path;
        	}        		
        	
        	if(currentURL=="/dashboard")
            {
            	$rootScope.screenName = "Universal Test Modeler";
            	$rootScope.parentMenu = "Home";
            	$rootScope.rightTabName="UTM Build Configuration";
            }
        	/*else if(currentURL=="/quicksearch")
            {
            	$rootScope.screenName = "Quick Search";
            	$rootScope.parentMenu = "Home";
            }*/
            else
            {
            	var found = false;
            	
    	        angular.forEach($scope.keyList, function(value, key) 
    	        {
    	        	console.log("foreachfunction");
    	        	if(!found)
    	        	{
	    	        	$rootScope.parentMenu = value;
	    	        	
	    	    	    angular.forEach($scope.tabs[value], function(value, key) 
	    		        {
	    		        	console.log("tebvalue");
	    	    	    	if(currentURL==value.link)
	    	    	    	{
	    	    	    		$rootScope.screenName=value.name;
	    	    	    		found=true;
	    	    	    	}	        	  
	    		        });
    	        	}
    	        });
            }
        };
        
        $scope.updatebreadcrumb();
        
        $scope.createNewProject = function(){  
            console.log("createNewProject");
        	if($rootScope.projectName != null){
        		var dlg = dialogs.confirm('Message','Do you want to over-write  the project ?');
        	
				dlg.result.then(function(btn){
					console.log("dlg.result");
					$scope.clearProject();
					var dlg1 = dialogs.create('partials/portfolios/create_new_project.html','CreateNewPrjCtrl',{},{size:'sm',keyboard: true,backdrop: false,windowClass: 'my-class'});
	    			dlg1.result.then(function(name){
	    				console.log("dlg.result");
	    				//$scope.name = name;
	    			},function(){
	    				console.log("emptyfunction");
	    				//if(angular.equals($scope.name,''))
	    					//$scope.name = 'You did not enter in your name!';
	    			});	
				},function(btn){
					console.log("btn");
					//$modalInstance.close("closed");
				});
        		
        	}else{
        		var dlg = dialogs.create('partials/portfolios/create_new_project.html','CreateNewPrjCtrl',{},{size:'lg',keyboard: true,backdrop: false,windowClass: 'my-class'});
    			dlg.result.then(function(name){
    				console.log("dlg.result");
    				//$scope.name = name;
    			},function(){
    				console.log("emptyfunction");
    				//if(angular.equals($scope.name,''))
    					//$scope.name = 'You did not enter in your name!';
    			});	
        		
        	}
        };
        
        $scope.clearProject= function(){ 
            console.log("clearProject");
        	$rootScope.projectName= null;
        	$rootScope.revision = -1;
        	//$rootScope.models.length=0;
        	$rootScope.utmModels=$rootScope.$new(true);
        	$rootScope.serviceInfo = $rootScope.$new(true);
        	$rootScope.serviceInfo= null;
        	$rootScope.serviceInputPartInfo = $rootScope.$new(true);
        	$rootScope.serviceOutputPartInfo=$rootScope.$new(true);
        	$rootScope.servicefaultPartInfo =$rootScope.$new(true);
        	$rootScope.isModel = false;
        	$("#paletteDiv").load('./modeler/dist/index.html');  
        	$rootScope.isPalette = false;      
        	$rootScope.isTestset = false;
        	$rootScope.isRequirementCoverage = false;
           	$rootScope.ispropertyExplorer = false;
         //  	$("#propertyDiv").load('./partials/portfolios/Property_Explorer.html');
        	$rootScope.modelName="";
        	//document.getElementById('propertyExplorer').classList.remove('visible');
        	document.getElementById("modeler_name").textContent="Activity Modeler";
        	//$( "#propertyExplorer" ).prev().css( "display", "block" );
        	$( "#activity_modeler" ).prev().css( "display", "block" );
        	$( 'div' ).find('.k-expand-next').click();
        
        	$rootScope.$apply();
        		
        };
        
        $scope.homePage=function(){
        	console.log("homePage");
        	$location.path('/dashboard');
        };
       $scope.propertyExplorerErrorMessage = function(msg)
       {
       	    console.log("propertyExplorerErrorMessage");
    	   var dlg = dialogs.notify('Error',msg);
       }
       
       //$scope.fromTstMultipleFlag=false;
        /*onclicking of review testset / generate testset */
        $scope.generateTestSet=function(testingType){
        	console.log("generateTestSet");
        	var errorMessage="";
        	var generateTestSetMDTURL = "/utm-service/test_generation/generateMDTTestSet";
        	
			var UTMProjectExplorer = {};
			UTMProjectExplorer.projectName = $rootScope.projectName;
			UTMProjectExplorer.utmModels = $rootScope.utmModels;
			
			UTMProjectExplorer.utmDepthValueMap = $rootScope.depthElementKeyMap;
			UTMProjectExplorer.utmCountValueMap = $rootScope.countElementKeyMap;

        	var utmMDTRequest = {};
        	utmMDTRequest.mainProcessName = selected_model;
			var utm_models = [];
			$rootScope.populateUTMModelArray(utm_models,$rootScope.utmModels);	        	
			utmMDTRequest.utmModels = utm_models;
			
			utmMDTRequest.testingType=testingType;
        	
			utmMDTRequest.utmProjectExplorer = UTMProjectExplorer;
			if ($rootScope.wsdlInfo != null) {
				utmMDTRequest.schemaLocation = $rootScope.wsdlInfo.schemaLocation;					
			}
			
			utmMDTRequest.dbToolRequests = Datafactory.getDbToolProjectLevelList();
			utmMDTRequest.runtimePythonScriptProjectLevelList = Datafactory.getRuntimePythonScriptProjectLevelList();
			utmMDTRequest.xmlValidatorList = Datafactory.getXmlValidatorList();
			utmMDTRequest.modelPreferenceInfo = Datafactory.getModelPreferenceInfo();
        	MenuService.generateMDTTestSet(utmMDTRequest, generateTestSetMDTURL)
    		.then(function(pars) {
    			list_model_test_sets[selected_model] = pars;
    			
    			//populate test sets of other models
    			for(var i = 0; i < utm_models.length; i++){
    				var model_test_set = {};
    				model_test_set.activityTestCases = [];
    				model_test_set.invalidModelException = pars.invalidModelException;
    				model_test_set.serviceName = pars.serviceName;
    				for(var y = 0; y < pars.activityTestCases.length; y++){
    					for(var z = 0; z < pars.activityTestCases[y].modelElements.length; z++){
    						if(pars.activityTestCases[y].modelElements[z].modelName == utm_models[i].modelName){
    							model_test_set.activityTestCases.push(pars.activityTestCases[y]);
    							break;
    						}
    					}
    				}
    				list_model_test_sets[utm_models[i].modelName] = model_test_set;
    			}
    			
    			list_model_test_sets[selected_model] = pars;

    			if(pars.invalidModelException.invalidModelElementExceptions.length>0){
    				for(var i=0;i<pars.invalidModelException.invalidModelElementExceptions.length;i++){
    					errorMessage = errorMessage +"\n"+"["+(i+1)+"]." + " "+pars.invalidModelException.invalidModelElementExceptions[i].message+"\n";
    					console.log("error Message:"+errorMessage);
    				}
    				
    				var dlg = dialogs.notify('Failure',errorMessage);
    				
    			}else{
    				
    				$rootScope.modeltestset = pars; 
    				
    				if(!$scope.fromTstMultipleFlag){
            			$rootScope.isPalette = false;
            			$rootScope.isTestset = true;
            			$rootScope.isRequirementCoverage = false;
            			document.getElementById("modeler_name").textContent="UTM Test Set";
            			//document.getElementById('propertyExplorer').classList.add('visible');
            			$('div').find('.k-collapse-next').click();
            			//$( "#propertyExplorer" ).prev().css( "display", "none" );
            			//$rootScope.$apply();
            			document.getElementById("Review/Validate Test Set").classList.remove('ThisLink');
    			
    				document.getElementById("Export to Excel").classList.remove('ThisLink');
    				/*document.getElementById("Export Test Set").classList.remove('ThisLink');*/
        			document.getElementById("Test Case / Requirement Coverage").classList.remove('ThisLink');
        			//$rootScope.$apply();
    			}
    		}
    			
    		},
    		function(data) {
    			console.log("data");

    		});
    		
        		
        };
        $scope.reviewTestSet=function(){

        	        console.log("reviewTestSet");
        			$rootScope.modeltestset = list_model_test_sets[selected_model];
        			
        			$rootScope.isPalette = false;
        			$rootScope.isTestset = true;
        			$rootScope.isRequirementCoverage = false;
        			document.getElementById("modeler_name").textContent="UTM Test Set";
        			//document.getElementById('propertyExplorer').classList.add('visible');
        			
        			//$( "#propertyExplorer" ).prev().css( "display", "none" );
        			$('div').find('.k-collapse-next').click();
        			console.log($rootScope.modeltestset);
        			//$rootScope.$apply();
        	
    		
        		
        };
        $scope.requirementCoverage=function(){
        	console.log("requirementCoverage");
        	$rootScope.testCaseRequirements=[];
        	$rootScope.validTestRequirementArray=[];
        	$rootScope.validTestRequirements={};        
        	$rootScope.modeltestset = list_model_test_sets[selected_model];        	
        	var allPathDetails=[]; 
        	$scope.currentSelectedModel = {};
        	$scope.getPathDetails($rootScope.utmModels,selected_model);
        	$scope.populatePathDetails(allPathDetails,$scope.currentSelectedModel);
        	$rootScope.pathDetailsList = list_model_path_details[selected_model];
        	/*for(var p=0;p<100;p++){
        			$rootScope.testCaseRequirements.push("Requirement"+p);	
        		}
        	for(var p=0;p<100;p++){
    			$rootScope.testCaseValue.push("TestCase"+p);    			
    		}*/
        	for(var x=0;x<allPathDetails.length;x++){        		
        		var tempPathDetails = allPathDetails[x];   
        		if(tempPathDetails != null){
        			for (var i = 0; i < tempPathDetails.length; i++) {
    					var pathDetails = tempPathDetails[i];				
    					if(pathDetails.requirement !=='' && pathDetails.requirement !== null ){					
    						$rootScope.testCaseRequirements.push(pathDetails.requirement);
    					}
    	        
    					/*for (var j = 0; j < pathDetails.decisionIdentifiers.length; j++) {
    					  if(pathDetails.decisionIdentifiers[j].requirement !== '' && pathDetails.decisionIdentifiers[j].requirement !== null){						
    						$rootScope.testCaseRequirements.push(pathDetails.decisionIdentifiers[j].requirement);
    					  }
    					}*/
            	    }
        		}
        		
        }
        	/*console.log("path details "+JSON.stringify($rootScope.pathDetailsList));
        	console.log("modeltestset "+$rootScope.modeltestset);*/
        	for(var p=0;p<$rootScope.modeltestset.activityTestCases.length;p++)
 			{
								var activityTestCases = $rootScope.modeltestset.activityTestCases[p];
								if (activityTestCases.mappedRequirements !=null) {
								for (var i = 0; i < activityTestCases.mappedRequirements.length; i++) {
									//$rootScope.testCaseRequirements
									//.push(activityTestCases.mappedRequirements[i]);
									var testCaseNames = $rootScope.validTestRequirements[activityTestCases.mappedRequirements[i]];
									if(testCaseNames == null){
										testCaseNames = [];
									}
									if(activityTestCases.version !=null)
										var testCase= activityTestCases.testCaseName + "_" +activityTestCases.version;
									else
										var testCase= activityTestCases.testCaseName;
									testCaseNames.push(testCase);
									$rootScope.validTestRequirements[activityTestCases.mappedRequirements[i]]=testCaseNames;
								}
							}
 			}
        	
        	
        	
        	
        	
    		
        			$rootScope.isPalette = false;
        			$rootScope.isTestset = false;
        			$rootScope.isRequirementCoverage = true;
        			document.getElementById("modeler_name").textContent="Test Case / Requirement Coverage";
        			//document.getElementById('propertyExplorer').classList.add('visible');
        			console.log("modeltestset"+JSON.stringify($rootScope.modeltestset));
        			//$( "#propertyExplorer" ).prev().css( "display", "none" );
        			$('div').find('.k-collapse-next').click();
        			//$rootScope.$apply();
        	
    		
        		
        };
        
        
        
        
        $scope.activityModelling = function(){
        	console.log(".activityModelling");
        	//window.open("./bpmn-js-examples-master/modeler/dist/index.html", "_self");
       // $location.path('/activity_modelling');
        };
        /*$scope.openProject = function(){
        	$location.path('/dashboard_upload');
        };*/
        $rootScope.cldsOpenTemplateProperties=function(){
        	console.log("cldsOpenTemplateProperties");
        	var dlg = dialogs.create('partials/portfolios/global_template_properties.html','CldsOpenTemplateCtrl',{},{size:'lg',keyboard: true,backdrop: 'static',windowClass: 'my-class'});
        	
        	dlg.result.then(function(name){
        		console.log("dlg.result");
        		//$scope.modelName =modelName;
        		//$("#" + selected_model).addClass("selectedcolor");
				//alert ("model name:"+$scope.modelName);
			},function(){
				console.log("....emptyfunction");
				//if(angular.equals($scope.name,''))
					//$scope.name = 'You did not enter in your name!';
			});	
        }
        
        $scope.cldsShowRunningInstances=function(){
        	console.log("cldsShowRunningInstances");
        	var localInstances;
        	var modelName = selected_model;
        	var dlg;
        	cldsModelService.getRunningInstances(modelName).then(function(pars) {
        		console.log("getRunningInstances");
       			localInstances = pars;
       			angular.forEach(localInstances.cldsModelInstanceList , function(element){
       				console.log("cldsModelInstanceList");
       				element.status = "Stopped";
       				if ( localInstances.status == "ACTIVE" ) {
       					element.status = "Running";
       				}
					element.view = "";
				});
				runningInstances = localInstances.cldsModelInstanceList;
				dlg = dialogs.create('partials/portfolios/running_instances.html','CldsOpenModelCtrl',{closable:true,draggable:true},{size:'lg',keyboard: true,backdrop: 'static',windowClass: 'my-class'});
				dlg.result.then(function(name){
					console.log("dlg.ressult");
    			//$scope.name = name;
	    		},function(){
	    			console.log("...emptyfunction");
	    			//if(angular.equals($scope.name,''))
	    				//$scope.name = 'You did not enter in your name!';
	    		});					
       		},
       		function(data) {
       			console.log("data");
       			//alert("setModel failed:  " + data);
       		});       	
        		
        	
        };
        
        $scope.cldsClose = function(){
        	console.log("cldsClose");
        	var dlg = dialogs.create('partials/portfolios/confirmation_window.html','CldsOpenTemplateCtrl',{closable:true,draggable:true},{size:'lg',keyboard: true,backdrop: 'static',windowClass: 'my-class'});
        		
        	dlg.result.then(function(name){
        		console.log("dlg.result");
    			//$scope.name = name;
    		},function(){
    			console.log("...emptyfunction");
    			//if(angular.equals($scope.name,''))
    				//$scope.name = 'You did not enter in your name!';
    		});	
        };
        $scope.cldsOpenTemplate = function(){
        	console.log("cldsOpenTemplate");
        	var dlg = dialogs.create('partials/portfolios/clds_open_template.html','CldsOpenTemplateCtrl',{closable:true,draggable:true},{size:'lg',keyboard: true,backdrop: 'static',windowClass: 'my-class'});
        		
        	dlg.result.then(function(name){
        		console.log("dlg.result");
    			//$scope.name = name;
    		},function(){
    			console.log("emptyfunction");
    			//if(angular.equals($scope.name,''))
    				//$scope.name = 'You did not enter in your name!';
    		});	
        };
        $scope.saveConfirmationNotificationPopUp = function(callBack) {
        	console.log("saveConfirmationNotificationPopUp");
		    var dlg = dialogs.create('partials/portfolios/save_confirmation.html', 'saveConfirmationModalPopUpCtrl', { closable: true, draggable: true }, { size: 'lg', keyboard: true, backdrop: 'static', windowClass: 'my-class' });

		    dlg.result.then(function(name) {
		        console.log("OK");
		        console.log("MINA TEST OK BUTTON: " + callBack);
		        callBack("OK");
		    }, function() {
		        console.log("CANCEL");
		        callBack(null);
		    });

		};
        $scope.cldsCreateTemplate=function(){
        	console.log("cldsCreateTemplate");
        	var dlg = dialogs.create('partials/portfolios/clds_create_template.html','CldsOpenTemplateCtrl',{closable:true,draggable:true},{size:'lg',keyboard: true,backdrop: 'static',windowClass: 'my-class'});
    		
        	dlg.result.then(function(name){
        		console.log("dlg.result");
    			//$scope.name = name;
    		},function(){
    			console.log("...emptyfunction");
    			//if(angular.equals($scope.name,''))
    				//$scope.name = 'You did not enter in your name!';
    		});	
  
        };
        $scope.cldsRefreshASDC=function(){
        	console.log("cldsRefreshASDC");
        	var dlg = dialogs.create('partials/portfolios/refresh_asdc.html','CldsOpenModelCtrl',{closable:true,draggable:true},{size:'lg',keyboard: true,backdrop: 'static',windowClass: 'my-class'});
        	dlg.result.then(function(name){
        		console.log("dlg.result");
    			//$scope.name = name;
    		},function(){
    			console.log("...emtptyfunction");
    			//if(angular.equals($scope.name,''))
    				//$scope.name = 'You did not enter in your name!';
    		});	
        }
        $scope.cldsRevertModel=function(){
        	console.log("cldsRevertModel");
        	var dlg = dialogs.create('partials/portfolios/ConfirmRevertChanges.html','CldsOpenModelCtrl',{closable:true,draggable:true},{size:'lg',keyboard: true,backdrop: 'static',windowClass: 'my-class'});
    		
        	dlg.result.then(function(name){
        		console.log("dlg.result");
    			//$scope.name = name;
    		},function(){
    			console.log("...emptyfunction");
    			//if(angular.equals($scope.name,''))
    				//$scope.name = 'You did not enter in your name!';
    		});	
  
        };
        
        $scope.cldsRevertTemplate=function(){
        	console.log("cldsRevertTemplate");
        	var dlg = dialogs.create('partials/portfolios/ConfirmRevertChanges.html','CldsOpenTemplateCtrl',{closable:true,draggable:true},{size:'lg',keyboard: true,backdrop: 'static',windowClass: 'my-class'});
    		
        	dlg.result.then(function(name){
        		console.log("dlg.result");
    			//$scope.name = name;
    		},function(){
    			console.log("..emptyfunction");
    			//if(angular.equals($scope.name,''))
    				//$scope.name = 'You did not enter in your name!';
    		});	
  
        };
        $scope.cldsTemplatePerformAction = function(uiAction){
        	console.log("cldsTemplatePerformAction");
			var modelName = selected_model;
			var controlNamePrefix = "ClosedLoop-";
			var bpmnText = modelXML;
			// serialize model properties
			var propText =  JSON.stringify(elementMap);	

			console.log("Generateing svg image");
 			var svgXml = "";
			console.log(abootDiagram.saveSVG({format:true},function(err,xml){

				if(err)
				console.log("error")
				else
				console.log(xml)
				svgXml = xml;
				}));
			console.log("cldsTemplatePerformAction: " + uiAction + " modelName=" + modelName);      
			console.log("cldsTemplatePerformAction: " + uiAction + " controlNamePrefix=" + controlNamePrefix);      
			console.log("cldsTemplatePerformAction: " + uiAction + " bpmnText=" + bpmnText);      
			console.log("cldsTemplatePerformAction: " + uiAction + " propText=" + propText);      
			cldsTemplateService.processAction( uiAction, modelName, controlNamePrefix, bpmnText, propText,svgXml).then(function(pars) {
				console.log("processAction");
       			console.log("cldsTemplatePerformAction: pars=" + pars);
       			cldsTemplateService.processActionResponse(modelName, pars);
       		},
       		function(data) {
       			console.log(".....emptyfunction");
       			//alert("setModel failed:  " + data);
       		});
        };  
        
        
        
        $rootScope.cldsOpenModelProperties=function(){
        	console.log("cldsOpenModelProperties");
        	var dlg = dialogs.create('partials/portfolios/global_properties.html','GlobalPropertiesCtrl',{},{size:'lg',keyboard: true,backdrop: 'static',windowClass: 'my-class'});
        	
        	dlg.result.then(function(name){
        		console.log("dlg.result");
        		//$scope.modelName =modelName;
        		//$("#" + selected_model).addClass("selectedcolor");
				//alert ("model name:"+$scope.modelName);
			},function(){
				console.log("...emptyfunction");
				//if(angular.equals($scope.name,''))
					//$scope.name = 'You did not enter in your name!';
			});	
        }
        $scope.cldsOpenModel = function(){
        	console.log("cldsOpenModel");
        	var dlg = dialogs.create('partials/portfolios/clds_open_model.html','CldsOpenModelCtrl',{closable:true,draggable:true},{size:'lg',keyboard: true,backdrop: 'static',windowClass: 'my-class'});
        		
        	dlg.result.then(function(name){
        		console.log("dlg.result");
    			//$scope.name = name;
    		},function(){
    			console.log("...emptyfunction");
    			//if(angular.equals($scope.name,''))
    				//$scope.name = 'You did not enter in your name!';
    		});	
        };
        $scope.cldsCreateModel=function(){
        	console.log("cldsCreateModel");
        	var dlg = dialogs.create('partials/portfolios/clds_create_model_off_Template.html','CldsOpenModelCtrl',{closable:true,draggable:true},{size:'lg',keyboard: true,backdrop: 'static',windowClass: 'my-class'});
    		
        	dlg.result.then(function(name){
        		console.log("dlg.result");
    			//$scope.name = name;
    		},function(){
    			console.log("...emptyfunction");
    			//if(angular.equals($scope.name,''))
    				//$scope.name = 'You did not enter in your name!';
    		});	
  
        };
        $scope.cldsPerformAction = function(uiAction){
        	console.log("cldsPerformAction");
			var modelName = selected_model;
			var controlNamePrefix = "ClosedLoop-";
			var bpmnText = modelXML;
			// serialize model properties
			var propText =  JSON.stringify(elementMap);	
			var templateName=selected_template
			console.log("Generateing svg image");
 			var svgXml = "";
			console.log(abootDiagram.saveSVG({format:true},function(err,xml){
				if(err)
				console.log("error")
				else
				console.log(xml)
				svgXml = xml;
				}));
			console.log("cldsPerformAction: " + uiAction + " modelName=" + modelName);      
			console.log("cldsPerformAction: " + uiAction + " controlNamePrefix=" + controlNamePrefix);      
			console.log("cldsPerformAction: " + uiAction + " bpmnText=" + bpmnText);      
			console.log("cldsPerformAction: " + uiAction + " propText=" + propText);      
       		cldsModelService.processAction( uiAction, modelName, controlNamePrefix, bpmnText, propText,svgXml,templateName).then(function(pars) {
       			console.log("cldsPerformAction: pars=" + pars);
       			cldsModelService.processRefresh(pars);
       		},
       		function(data) {
       			console.log("data");
       			//alert("setModel failed:  " + data);
       		});
        };   
        
       $scope.cldsConfirmPerformAction = function(uiAction){

       console.log("cldsConfirmPerformAction");
        	var dlg = dialogs.confirm('Message', 'Do you want to ' + uiAction.toLowerCase() + ' the closed loop?');
        	dlg.result.then(function (btn) {
        		console.log("dlg.result");
        		$scope.cldsPerformAction(uiAction);
        	}, function (btn) {
        		console.log("btn");
                //$modalInstance.close("closed");
            });
        }; 
      
        $scope.CollectorsWindow = function (collectorsWin) {
        	console.log("CollectorsWindow");

            if (isTemplate){
            	var dlg = dialogs.create('partials/portfolios/Template_model.html','ImportSchemaCtrl',collectorsWin,{closable:true,draggable:true},{size:'lg',keyboard: true,backdrop: 'static',windowClass: 'my-class'});
                dlg.result.then(function(name){
                	console.log("dlg.result");

                },function(){
                	console.log("...emptyfunction");
                    //if(angular.equals($scope.name,''))
                    //$scope.name = 'You did not enter in your name!';
                });
            }
            else{
                var dlg = dialogs.create('partials/portfolios/Collector_properties.html','ImportSchemaCtrl',{closable:true,draggable:true},{size:'lg',keyboard: true,backdrop: 'static',windowClass: 'my-class'});
                dlg.result.then(function(name){
                	console.log("dlg.result");

                },function(){
                	console.log("...emptyfunction");
                    //if(angular.equals($scope.name,''))
                    //$scope.name = 'You did not enter in your name!';
                });

            }

        };
        $scope.StringMatchWindow = function (stringMatch) {
        	console.log("StringMatchWindow");
        	if (isTemplate){
            	var dlg = dialogs.create('partials/portfolios/Template_model.html','ImportSchemaCtrl',stringMatch,{closable:true,draggable:true},{size:'lg',keyboard: true,backdrop: 'static',windowClass: 'my-class'});
                dlg.result.then(function(name){
                	console.log("dlg.result");

                },function(){
                	console.log("...emptyfunction");
                    //if(angular.equals($scope.name,''))
                    //$scope.name = 'You did not enter in your name!';
                });
            }
            else{

                var dlg = dialogs.create('partials/portfolios/stringMatch_properties.html','ImportSchemaCtrl',{closable:true,draggable:true},{size:'lg',keyboard: true,backdrop: 'static',windowClass: 'my-class'});

                dlg.result.then(function(name){
                	console.log("dlg.result");
                    //$scope.name = name;
                },function(){
                	console.log("...emptyfunction");
                    //if(angular.equals($scope.name,''))
                    //$scope.name = 'You did not enter in your name!';
                });

            }
        };

        $scope.TCAWindow = function (tca) {
        	if (isTemplate){
            	var dlg = dialogs.create('partials/portfolios/Template_model.html','ImportSchemaCtrl',tca,{closable:true,draggable:true},{size:'lg',keyboard: true,backdrop: 'static',windowClass: 'my-class'});
                dlg.result.then(function(name){
                },function(){
                    //if(angular.equals($scope.name,''))
                    //$scope.name = 'You did not enter in your name!';
                });
            }
            else{
                var dlg = dialogs.create('partials/portfolios/tca_properties.html','ImportSchemaCtrl',{closable:true,draggable:true},{size:'lg',keyboard: true,backdrop: 'static',windowClass: 'my-class'});

                dlg.result.then(function(name){
                    //$scope.name = name;
                },function(){
                    //if(angular.equals($scope.name,''))
                    //$scope.name = 'You did not enter in your name!';
                });
            }
        };
        
        $scope.PolicyWindow=function (policy) {
        	console.log("PolicyWindow");
        	if (isTemplate){
            	var dlg = dialogs.create('partials/portfolios/Template_model.html','ImportSchemaCtrl',policy,{closable:true,draggable:true},{size:'lg',keyboard: true,backdrop: 'static',windowClass: 'my-class'});
                dlg.result.then(function(name){
                	console.log("dlg.result");

                },function(){
                	console.log("...emptyfunction");
                    //if(angular.equals($scope.name,''))
                    //$scope.name = 'You did not enter in your name!';
                });
            }
            else {
                var dlg = dialogs.create('partials/portfolios/PolicyWindow_properties.html', 'ImportSchemaCtrl', {
                    closable: true,
                    draggable: true
                }, {size: 'lg', keyboard: true, backdrop: 'static', windowClass: 'my-class'});

                dlg.result.then(function (name) {
                	console.log("dlg.result");
                    //$scope.name = name;
                }, function () {
                	console.log("...emptyfunction");
                    //if(angular.equals($scope.name,''))
                    //$scope.name = 'You did not enter in your name!';
                });

            }
        };
       
        
      
        
        $scope.populatePathDetails= function(allPathDetails,utmModels){
        console.log("populatePathDetails");      	
        	if (utmModels != null && utmModels.name != null) {
    			var pathDetails = {};    			
    			pathDetails = list_model_path_details[utmModels.name];
    			allPathDetails.push(pathDetails);
    			if(utmModels.subModels != null && utmModels.subModels.length>0){				
    				for(var i=0 ; i<utmModels.subModels.length;i++) {
    					var subModel = {};
    					subModel = utmModels.subModels[i];
    					$scope.populatePathDetails(allPathDetails,subModel);
    				 }
    			}
    			
    		}
    	};    	
    	
    	$scope.getPathDetails= function(utmModels,selectedModelName) { 
    	   console.log("getPathDetails");   		
    		if (utmModels != null && utmModels.name != null && utmModels.name===selectedModelName){
    			$scope.currentSelectedModel = utmModels;
    		}else if(utmModels.subModels != null && utmModels.subModels.length>0){				
    				for(var i=0 ; i<utmModels.subModels.length;i++) {
    					var subModel = {};
    					subModel = utmModels.subModels[i];
    					$scope.getPathDetails(subModel,selectedModelName);
    				 }
    			}
    				
    	};
        
        
        
}]);

app.service('MenuService', ['$http', '$q', function ($http, $q) {
	console.log("MenuService");
    this.generateMDTTestSet = function(utmMDTRequest, generateTestSetMDTURL){

    	console.log("generateMDTTestSet");
    	//alert("In generateMDTTestSet :: " + JSON.stringify(utmMDTRequest));
    	var def = $q.defer();
    	var sets = [];
    	
        $http.post(generateTestSetMDTURL, utmMDTRequest)
        .success(function(data){ 
        console.log("success");       	
        	sets = data;
        	def.resolve(data);         	
        	
        })
        .error(function(data){
        console.log("error");       	 	      
       	 	def.reject("GenerateMDTTestSet not successful");
        });
        
        return def.promise;
    };
}]);

app.directive('focus',
		function($timeout) {
			console.log("focus");
	return {
		scope : {
			trigger : '@focus'
		},
		link : function(scope, element) {
			scope.$watch('trigger', function(value) {
				console.log("trigger");
				if (value === "true") {
					$timeout(function() {
						console.log("timeout");
						element[0].focus();
					});
				}
			});
		}
	};
}
); 
app.directive('draggable', function($document) {
	console.log("draggable");
  return function(scope, element, attr) {
  	console.log("return");
    var startX = 0, startY = 0, x = 0, y = 0;
    element.css({
     position: 'relative',
     
     backgroundColor: 'white',
     cursor: 'move',
     display: 'block',
     
    });
    element.on('mousedown', function(event) {
    	console.log("mousedown");
      // Prevent default dragging of selected content
      //event.preventDefault();
      startX = event.screenX - x;
      startY = event.screenY - y;
      $document.on('mousemove', mousemove);
      $document.on('mouseup', mouseup);
    });

    function mousemove(event) {
    	console.log("mousemove");
      y = event.screenY - startY;
      x = event.screenX - startX;
      element.css({
        top: y + 'px',
        left:  x + 'px'
      });
    }

    function mouseup() {
    	console.log("mouseup");
      $document.off('mousemove', mousemove);
      $document.off('mouseup', mouseup);
    }
  };
});

app.factory('myHttpInterceptor', function ($q, $window) 
{
	console.log("myHttpInterceptor");
	  return function (promise) 
	  {
	  	console.log("promise");
	    return promise.then(function (response) 
	    {
	    	console.log("response");
	      return response;
	    }, 
	    function (response) 
	    {	
	    	console.log("response");
	      return $q.reject(response);
	    });
	  };
});



app.run(['$route', function($route)  {
	console.log("route");
	  $route.reload();
}]);
function TestCtrl($scope) {
	console.log("TestCtrl");
    $scope.msg = "Hello from a controller method.";
    $scope.returnHello = function() {
    	console.log("returnHello");
        return $scope.msg ; 
    }
}
function importshema()
{
	console.log("importshema");

	angular.element(document.getElementById('navbar')).scope().importSchema();
	
}

function CollectorsWindow(collectorsWin) {
	console.log("CollectorsWindow");

    angular.element(document.getElementById('navbar')).scope().CollectorsWindow(collectorsWin);

}

function F5Window() {
	console.log("F5Window");

    angular.element(document.getElementById('navbar')).scope().F5Window();

}

function StringMatchWindow(stringMatch) {
	console.log("StringMatchWindow");

    angular.element(document.getElementById('navbar')).scope().StringMatchWindow(stringMatch);

}
function TCAWindow(tca) {

    angular.element(document.getElementById('navbar')).scope().TCAWindow(tca);

}
function GOCWindow() {
	console.log("GOCWindow");

    angular.element(document.getElementById('navbar')).scope().GOCWindow();

}
function PolicyWindow(PolicyWin) {
	console.log("PolicyWin");
    angular.element(document.getElementById('navbar')).scope().PolicyWindow(PolicyWin);

}



function pathDetails(bpmnElementID,bpmnElementName,pathIdentifiers)
{
	console.log("pathDetails");

	angular.element(document.getElementById('navbar')).scope().pathDetails(bpmnElementID,bpmnElementName,pathIdentifiers);
	
}
function setdefaultvalue()
{
	console.log("setDefaultValue");

	angular.element(document.getElementById('navbar')).scope().setDefaultValue();
	
}
function upgradeSchemaVersion()
{
	console.log("upgradeSchemaVersion");

	angular.element(document.getElementById('navbar')).scope().upgradeSchemaVersion();
	
}
function saveProject()
{
	console.log("saveProject");

	angular.element(document.getElementById('navbar')).scope().saveProject();
	
}
function modifySchema()
{
	console.log("modifySchema");

	angular.element(document.getElementById('navbar')).scope().modifySchema();
	
}

function definePID()
{
	console.log("definePID");

	angular.element(document.getElementById('navbar')).scope().definePID();
	
}
function defineServiceAcronym()
{
	console.log("defineServiceAcronym");

	angular.element(document.getElementById('navbar')).scope().defineServiceAcronym();
	
}
function errorProperty(msg)
{
	console.log("errorProperty");
	angular.element(document.getElementById('navbar')).scope().propertyExplorerErrorMessage(msg);
}
function invisiblepropertyExplorer()
{
	console.log("invisiblepropertyExplorer");
	
	angular.element(document.getElementById('navbar')).scope().invisibleproperty();
}
function updateDecisionLabel(originalLabel, newLabel)
{
	console.log("updateDecisionLabel");
	angular.element(document.getElementById('navbar')).scope().updateDecisionLabels(originalLabel, newLabel);
}
