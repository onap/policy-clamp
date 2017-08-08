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


function DashboardCtrl($scope,$rootScope,$resource, $http, $timeout, $location, $interval, $q, Datafactory) 
{
	console.log("//////////////DashboardCtrl");
	$scope.autosaveInterval = 1800000;
	$scope.testsetgendashboard = false;
	$rootScope.isModel = false;
	$rootScope.isPalette = false;
	$rootScope.isTestSet = false;  
	$rootScope.isRequirementCoverage = false;
	$rootScope.ispropertyExplorer = false;
	$rootScope.parameters;
	$scope.orientation ="horizontal";
	$rootScope.ispropertyExplorer = false;
	$rootScope.isActive =true;
	$rootScope.models=[];
	$scope.selectedParent={};
	$rootScope.utmModels={};
	$rootScope.selectedModelName;
	$rootScope.dialogName="";
	

	$interval( function(){
		console.log("interval"); 
		/*AutosaveProject($scope,$rootScope,$resource, $http, $timeout, $location, $interval,
	 	$q, Datafactory);*/
		}, $scope.autosaveInterval);
	
	
	$rootScope.onSelectActivityModel =function(obj)
    {
    	console.log("onSelectActivityModel");
    	
		$rootScope.isPalette = true;
		$rootScope.isTestSet = false;  
		$rootScope.isRequirementCoverage = false;
		$rootScope.ispropertyExplorer = false;
    	//document.getElementById('propertyExplorer').classList.remove('visible');
    	
    	//$( "#propertyExplorer" ).prev().css( "display" ,"block");
		$( "#activity_modeler" ).prev().css( "display", "block" );
		$( 'div' ).find('.k-expand-next').click();
	   
		if(obj == undefined){
			document.getElementById("modeler_name").textContent="Activity Modeler" ;
		}else{
			selected_model=obj;
	        document.getElementById("modeler_name").textContent="Activity Modeler"+"  - "+ selected_model ;			
		}
		
		$rootScope.modelName = selected_model;
        
        $rootScope.modeltestset = list_model_test_sets[selected_model];
        if(list_model_schema_extensions[selected_model] == null){
        	if(list_model_schema_extensions[$rootScope.utmModels.name] != null) {
        		list_model_schema_extensions[selected_model] = jQuery.extend(true, {}, list_model_schema_extensions[$rootScope.utmModels.name]);
        	} else { 
        		list_model_schema_extensions[selected_model] = {};
        	}
        }

		$rootScope.initProjectExplorer();
        
        visibility_model();
        changecolor(selected_model);
        
       
    };
    $scope.selectActivityTestSet =function()
    {
    	console.log("selectActivityTestSet");
    	$rootScope.isPalette = false;
    	$rootScope.isRequirementCoverage = false;
		$rootScope.isTestset = true;
		document.getElementById("modeler_name").textContent="UTM Test Set";
		//document.getElementById('propertyExplorer').classList.add('visible');
		//$( "#propertyExplorer" ).prev().css( "display" ,"none");
		$( 'div' ).find('.k-collapse-next').click();
		$rootScope.modeltestset = list_model_test_sets[selected_model];
		$rootScope.$apply();
		
    };
	$scope.showPalette= function(){
		console.log("showPalette");
		//alert("showPalette()");
		$rootScope.isModel = true;
	//	$rootScope.isPalette = true;
		
	};
	
	//$scope.initialShow=false;
			
	if("/testsetgendashboard"==$location.url())
	{
		$scope.testsetgendashboard = true;
		
	 	
		$rootScope.total_users = 0;
		$scope.showUserView = true;
		$scope.showTestExecution = true;
		//$rootScope.total_accounts = 606;
		$scope.showAccountView = true;

		$rootScope.total_creation_times = 0;
		$rootScope.success_rate_percent = 0;//(((data.data[0].value*1.0)/(data.data[0].value+data.data[1].value))*100).toFixed();
		$rootScope.total_accounts = 0;
		$scope.showCreation_timeView = true;
		$scope.showSuccess_ratePercent = true;
		
		
		//$scope.generalMessages= "This section will show general messages/alerts.";
		$scope.gridHeaderMessages= "TestData Self-Service: Select TestCase and Click on Run button!!!";
		
		//Filters' JS

		    $scope.dt = new Date();
		  $scope.clear = function () {
		  	console.log("clear");
		    $scope.dt = null;
		  };

		  // Disable weekend selection
		  $scope.disabled = function(date, mode) {
		  	console.log("disabled");
		    return ( mode === 'day' && ( date.getDay() === 0 || date.getDay() === 6 ) );
		  };

		  $scope.toggleMin = function() {
		  	console.log("toggleMin");
		    $scope.minDate = $scope.minDate ? null : new Date();
		  };
		  $scope.toggleMin();

		  $scope.open2 = function($event) {
		  	console.log("open2");
			  $event.preventDefault();
		    $event.stopPropagation();
		    console.log(' herro: is the value of opened');
		    $scope.opened = true;
		    console.log($scope.opened + ' is the value of opened');
		  };

		  $scope.dateOptions = {
		    formatYear: 'yy',
		    startingDay: 1
		  };

		  $scope.formats = ['dd-MMMM-yyyy', 'yyyy/MM/dd', 'dd.MM.yyyy', 'shortDate'];
		  $scope.format = $scope.formats[0];
		  
		  $scope.disabled = function(date, mode) 
		  {
		  	console.log("disabled");
	        return ( mode === 'day' && ( date.getDay() === -1 || date.getDay() === 7 ) );
	      };

	       $scope.maxDate = new Date();


	         $scope.open = function($event,opened) {
	         	console.log("open");
	              $event.preventDefault();
	              $event.stopPropagation();

	              $scope[opened] = true;
	            };


	       $scope.dateOptions = {
	       'year-format': "'yy'",
	       'starting-day': 1
	       };
	       
	       $scope.date = "29-December-2014";
	       $scope.date2 = new Date();
	       
	       $scope.printDate = function()
	       {
	    	   console.log("printDate");
	       };
				
	};
	
	
	
	
	
	$scope.returnObjectArray = function(arrayObj)
	{
		console.log("returnObjectArray");
		var newArrayObj = [];
		
		angular.forEach(arrayObj, function(value, key) 
		{
			console.log("arrayObj");
			newArrayObj.push({"data":value});
		});
		
		return newArrayObj;
	};
	
	 
	$scope.returnTestScenarioTstObjectArray = function(arrayObj)
	 {
	  console.log("returnTestScenarioTstObjectArray");
	  var newArrayObj = [];
	  
	  angular.forEach(arrayObj, function(value, key) 
	  {
	  	console.log("arrayObj");
		  if( value.testScenarioInstances !=undefined && value.testScenarioInstances.length !=0){
			  var tempLabel = value.testScenarioInstances[0].label.split(":");
			  	newArrayObj.push({"data":tempLabel[0]});
		  }
			
	  });
	  
	  return newArrayObj;
	 };

	
	

	$scope.returnOverRiddenObjectArray = function(arrayObj)
	{
		console.log("returnOverRiddenObjectArray");
		var newArrayObj = [];
		
		angular.forEach(arrayObj, function(value, key) 
		{
			console.log("arrayObj");
			newArrayObj.push({"data":value.name});
		});
		
		return newArrayObj;
	};
	
	
	
	
	$rootScope.filterRouter = 'partials/DashboardFilters.html';
	$scope.isActivePROD = true;
	$scope.isActiveQC = false;
	$rootScope.reload = function(env)
	{
			console.log("reload");
	};

	$scope.showTDSSView = true;
	
	$scope.ReLoadDashboardFromViewResetComboBox = function(type, amount)//('Users','All') ('Accounts','All') ('Creation_times','All')
	{
		console.log("ReLoadDashboardFromViewResetComboBox");
	};
	
	
	
	//////////////////////////////////
	
	$scope.total_tdr_team_selected_model = [];
	$scope.total_tdr_users_selected_model = [];
	$scope.total_tdr_requests_selected_model = [];
	$scope.total_tdr_entities_selected_model = [];
	
	$scope.reloadTDRDashboard = function(name)
	{
		console.log("reloadTDRDashboard");
		var url = "/testdata-service/test-data-request/dashboard/counts.json?";
		 		
		$http.get(url+params).success(function(data)
		{
			console.log("success");
			$scope.total_tdr_team_count = data.result.dashboardCountModel.teams;
			$scope.total_tdr_users_count = data.result.dashboardCountModel.users;
			$scope.total_tdr_requests_count = data.result.dashboardCountModel.requests;
			$scope.total_tdr_entities_count = data.result.dashboardCountModel.entities;
			
			
			if("team"!=name)
			{
				$scope.total_tdr_team_array = $scope.returnObjectArray(data.result.tdrDashboardModel.teams);	
				$scope.TDRTeamGridId.api().clear().draw();
				$scope.TDRTeamGridId.fnAddData($scope.total_tdr_team_array);
			}
			
			
			if("users"!=name)
			{
				$scope.total_tdr_users_array = $scope.returnObjectArray(data.result.tdrDashboardModel.users);
				$scope.TDRUserGridId.api().clear().draw();
				$scope.TDRUserGridId.fnAddData($scope.total_tdr_users_array);
			}
				
			if("status"!=name)
			{
				$scope.total_tdr_entities_array = $scope.returnObjectArray(data.result.tdrDashboardModel.entities);
				$scope.TDREntitiesGridId.api().clear().draw();
				$scope.TDREntitiesGridId.fnAddData($scope.total_tdr_entities_array);
			}
			
			if("request"!=name)
			{
				$scope.total_tdr_requests_array = $scope.returnObjectArray(data.result.tdrDashboardModel.requests);
				$scope.TDRNumberGridId.api().clear().draw();
				$scope.TDRNumberGridId.fnAddData($scope.total_tdr_requests_array);
			}			
		});
		
	};
	
	
	$scope.loadTDRDashboard = function()
	{
		console.log("loadTDRDashboard");
		$rootScope.launch('wait'); 
		
		/* api jobs */
		var apiJobUrl = "/utm-service/em/jobs?timezoneOffset=420";
		 
		$http.get(apiJobUrl).success(function(data)
		{
			console.log("success");
			$scope.total_test_scenario_count = data.length;
			

			$scope.TDRNumberGridId = $('#TDRNumberGridId').dataTable( {
				"serverSide": false,
				"aoColumns": [

		                      {   "sTitle": "","mDataProp": null, "sWidth": "20px", "bSortable": false},
		                      {   "sTitle":"Total TestScenarios","mDataProp": "data","bSortable": true}					                     
		          ],
				//"columns": [{"data":   "data"}],
		          "order": [[ 1, "asc" ]],
				"bPaginate": false,
				"bFilter": false, 
				"bInfo": false,
			    "bAutoWidth": false,
			    "bScrollCollapse": false,
			   "bLengthChange":false,
			   "bJQueryUI": true,
			   "search": {"caseInsensitive": true},
			   "scrollY": "200px",
		       "scrollX": "100%",
		       "sScrollXInner": "100%",
			   "fnCreatedRow": function( nRow, aData, iDataIndex ) 
			   {
			   	console.log("fnCreatedRow");
			    	$(nRow).children("td").css("overflow", "hidden");
			    	$(nRow).children("td").css("white-space", "nowrap");
			    	$(nRow).children("td").css("text-overflow", "ellipsis");
			    	
			    	var found = false;		
		     		
     				angular.forEach($scope.total_tdr_requests_selected_model, function (value) 
	                {
	                	console.log("total_tdr_requests_selected_model");
						if(aData.data==value.id)
						{
							found=true;
						}
	                });
		     		
     				if(found)
     				{
     					$('td:eq(0)', nRow).html( '<span class="tdr_checkbox tdr_checkbox_glyphicon_glyphicon_ok glyphicon glyphicon-ok" id="'+aData.data+'"></span>');
     				}
     				else
     				{
     					$('td:eq(0)', nRow).html( '<span class="tdr_checkbox tdr_checkbox_glyphicon_glyphicon_unchecked glyphicon glyphicon-unchecked" id="'+aData.data+'"></span>');
     				}
     				
			    	
			    }
		    } );
			
			$scope.total_tdr_requests_array = $scope.returnOverRiddenObjectArray(data);
			
			if($scope.total_tdr_requests_array.length > 0)
			{
				$scope.TDRNumberGridId.fnAddData($scope.total_tdr_requests_array);	
			}
			
			
			
						
			
			$('#TDRNumberGridId tbody').on( 'click', 'td', function () 
			{
				console.log("click");
				var position = $scope.TDRNumberGridId.fnGetPosition(this); // getting the clicked row position			    	
		    	
		    	 if(position[1]==1)
		    	 {
		    		 
				 	var valueX = this.innerHTML;
		    		
		    		$('.tdr_checkbox').each(function(i, obj) 
		    		{	 
		    			console.log("tdr_checkbox");
		    			var uncheck = $(obj).hasClass( "glyphicon-unchecked");
		    			
		    			if(valueX==obj.id && uncheck)
				    	{
		    				$(obj).attr('class','tdr_checkbox tdr_checkbox_glyphicon_glyphicon_ok glyphicon glyphicon-ok');
				    	}		    		    	
				    	else if(valueX==obj.id && !uncheck)
				    	{
				    		$(obj).attr('class','tdr_checkbox tdr_checkbox_glyphicon_glyphicon_unchecked glyphicon glyphicon-unchecked');
				    	}		    		    	  			    			
		    		});
		    		
		    		
		    		 if($('.tdr_checkbox_glyphicon_glyphicon_ok').length <= 0)
				     {
		    			 $scope.total_tdr_requests_selected_model = [];			    		 
			    		 $scope.total_tdr_requests_selected_model.push({'id':'All'});			    		 
		    		 }
		    		 else
					 {
		    			 $scope.total_tdr_requests_selected_model = [];
		    			 
			 			$('.tdr_checkbox_glyphicon_glyphicon_ok').each(function(i, obj) 
				    	{	
				    		console.log("tdr_checkbox_glyphicon_glyphicon_ok");
			 				$scope.total_tdr_requests_selected_model.push({'id':obj.id});
				    	});
			 			
					 }
		    	 }		
		    	 else
		    	 {
		    		var valueX = this.innerHTML.substring(this.innerHTML.indexOf("id=\"")+4,this.innerHTML.length-9);
		    		
		    		$('.tdr_checkbox').each(function(i, obj) 
					{	 
						console.log("tdr_checkbox");
		    			var uncheck = $(obj).hasClass( "glyphicon-unchecked");
		    			
		    			if(valueX==obj.id && uncheck)
				    	{
		    				$(obj).attr('class','tdr_checkbox tdr_checkbox_glyphicon_glyphicon_ok glyphicon glyphicon-ok');
				    	}		    		    	
				    	else if(valueX==obj.id && !uncheck)
				    	{
				    		$(obj).attr('class','tdr_checkbox tdr_checkbox_glyphicon_glyphicon_unchecked glyphicon glyphicon-unchecked');
				    	}
				    	  			    			
					 });
		    		
		    		
		    		 if($('.tdr_checkbox_glyphicon_glyphicon_ok').length <= 0)
				     {
		    			 $scope.total_tdr_requests_selected_model = [];			    		 
			    		 $scope.total_tdr_requests_selected_model.push({'id':'All'});	
		    		 }
		    		 else
					 { 
		    			 $scope.total_tdr_requests_selected_model = [];
			 			$('.tdr_checkbox_glyphicon_glyphicon_ok').each(function(i, obj) 
				    	{	 
				    		console.log("tdr_checkbox_glyphicon_glyphicon_ok");
			 				$scope.total_tdr_requests_selected_model.push({'id':obj.id});	
				    	});
					 }
		    	 }

		    	 $scope.reloadTDRDashboard('request');
			});
			//$scope.initialShow=true;
			$rootScope.$broadcast('dialogs.wait.complete');
			
		
		});
		
	};
		

	
	$scope.getCommaSeparatedString = function(json) 
	{ 
		console.log("getCommaSeparatedString");
		if(json==undefined || json==null)
		{
			return "All";
		}
		
		var result = "";
		var found =false;
		
		for (var dString in json) 
		{ 
			result += json[dString].id + ",";
			found=true;
		}
				
		var res = result.match(/All,/g);
		
		if(res!=null && result.split(",").length > 1)
		{
			result = result.replace("All,", "");
		}
				
		if(!found || result=="")
		{
			return "All";
		}
		
		return result.replace(/,(\s+)?$/, '');
	};
	
	
	/*if("/dashboard"==$location.url())
	{
		$scope.loadTDRDashboard();
	}*/
	
	
	
	$scope.reloadTDRDashboardFromReset = function(name)
	{
		console.log("reloadTDRDashboardFromReset");
		var url = "/testdata-service/test-data-request/dashboard/counts.json?";
		var params = "teams="+$scope.getCommaSeparatedString($scope.total_tdr_team_selected_model);
		params = params + "&users="+$scope.getCommaSeparatedString($scope.total_tdr_users_selected_model); 
		params = params + "&requests="+$scope.getCommaSeparatedString($scope.total_tdr_requests_selected_model);
		params = params + "&status="+$scope.getCommaSeparatedString($scope.total_tdr_entities_selected_model);
				
		$http.get(url+params).success(function(data)
		{
			console.log("success");
			$scope.total_tdr_team_count = data.result.dashboardCountModel.teams;
			$scope.total_tdr_users_count = data.result.dashboardCountModel.users;
			$scope.total_tdr_requests_count = data.result.dashboardCountModel.requests;
			$scope.total_tdr_entities_count = data.result.dashboardCountModel.entities;
			
			$scope.total_tdr_team_array = $scope.returnObjectArray(data.result.tdrDashboardModel.teams);	
			$scope.TDRTeamGridId.api().clear().draw();
			$scope.TDRTeamGridId.fnAddData($scope.total_tdr_team_array);
			
			$scope.total_tdr_users_array = $scope.returnObjectArray(data.result.tdrDashboardModel.users);
			$scope.TDRUserGridId.api().clear().draw();
			$scope.TDRUserGridId.fnAddData($scope.total_tdr_users_array);
			
			$scope.total_tdr_entities_array = $scope.returnObjectArray(data.result.tdrDashboardModel.entities);			
			$scope.TDREntitiesGridId.api().clear().draw();
			$scope.TDREntitiesGridId.fnAddData($scope.total_tdr_entities_array);
			
			$scope.total_tdr_requests_array = $scope.returnObjectArray(data.result.tdrDashboardModel.requests);
			$scope.TDRNumberGridId.api().clear().draw();
			$scope.TDRNumberGridId.fnAddData($scope.total_tdr_requests_array);						
		});
		
	};
	
	
	$scope.ReLoadTDRDashboard = function(name)
	{
		console.log("ReLoadTDRDashboard");
		if("team"==name)
		{
			$scope.total_tdr_team_selected_model = [];
		}		
		
		if("users"==name)
		{
			$scope.total_tdr_users_selected_model = [];
		}
			
		if("status"==name)
		{
			$scope.total_tdr_entities_selected_model = [];
		}
		
		if("request"==name)
		{
			$scope.total_tdr_requests_selected_model = [];
		}	
		
		$scope.reloadTDRDashboardFromReset(name);
		
	};
	
}
function changecolor(selected_model)
{
	console.log("changecolor");
	
	var i = 0;
	//var modelNames =[];
	
    $(".models").each(function(i){
    	console.log("each");
    var model_value = $(this).text().trim();
    //modelName.push(model_value);
    if(model_value == selected_model || model_value == "")
    {
    	 $(this).addClass("selectedcolor");
    }
    else
    {
        $(this).removeClass("selectedcolor");
        
    }
        
       i++; 
    });
}


