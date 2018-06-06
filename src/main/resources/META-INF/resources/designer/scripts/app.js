/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2017-2018 AT&T Intellectual Property. All rights
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
 * 
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
    'kendo.directives',
  ])
  .config(['cfpLoadingBarProvider', function(cfpLoadingBarProvider) {

    cfpLoadingBarProvider.includeBar = true;
    cfpLoadingBarProvider.includeSpinner = true;
  }])
  .config(
    function($httpProvider) {

      $httpProvider.responseInterceptors
        .push('myHttpInterceptor');

      var spinnerFunction = function spinnerFunction(data,
        headersGetter) {

        return data;
      };

      $httpProvider.defaults.transformRequest
        .push(spinnerFunction);
    })
  .config(
    [
      '$routeProvider',
      '$locationProvider',
      '$compileProvider',
      'cfpLoadingBarProvider',
      function($routeProvider, $locationProvider,
        cfpLoadingBarProvider, $timeout, dialogs,
        $cookies) {
        $locationProvider.html5Mode(false);
        // alert("App.js");

        $routeProvider
          .when('/otherwise', {
            templateUrl: 'please_wait.html',
            controller: QueryParamsHandlerCtrl
          })
          .
        // when('/dashboard_submit', { templateUrl:
        // 'partials/portfolios/dashboard_submit.html',
        // controller: CreateNewPrjCtrl }).
        when(
            '/dashboard', {
              templateUrl: 'partials/portfolios/clds_modelling.html',
              controller: DashboardCtrl
            })
          .
        // when('/dashboard_upload', { templateUrl:
        // 'partials/portfolios/dashboard_upload.html',
        // controller: DashboardCtrl }).
        when(
          '/activity_modelling', {
            templateUrl: 'partials/portfolios/clds_modelling.html',
            controller: DashboardCtrl
          }).when('/authenticate', {
          templateUrl: 'authenticate.html',
          controller: AuthenticateCtrl
        }).when('/invalidlogin', {
          templateUrl: 'invalid_login.html',
          controller: PageUnderConstructionCtrl
        }).otherwise({
          redirectTo: '/otherwise'
        });

      }
    ])
  .controller(
    'dialogCtrl',
    function($scope, $rootScope, $timeout, dialogs) {

      // -- Variables --//

      $scope.lang = 'en-US';
      $scope.language = 'English';

      var _progress = 100;

      $scope.name = '';
      $scope.confirmed = 'No confirmation yet!';

      $scope.custom = {
        val: 'Initial Value'
      };

      // -- Listeners & Watchers --//

      $scope.$watch('lang', function(val, old) {

        switch (val) {
          case 'en-US':
            $scope.language = 'English';
            break;
          case 'es':
            $scope.language = 'Spanish';
            break;
        }
      });

      // -- Methods --//
      $rootScope.testCaseRequirements = [];
      $rootScope.validTestRequirements = [];
      /* $rootScope.testCaseValue=[]; */
      $scope.setLanguage = function(lang) {

        $scope.lang = lang;
        $translate.use(lang);
      };

      $rootScope.launch = function(which) {

        switch (which) {
          case 'error':
            dialogs.error();
            break;
          case 'wait':
            // var dlg =
            // dialogs.wait(undefined,undefined,_progress);
            // _fakeWaitProgress();
            break;
          case 'customwait':
            // var dlg = dialogs.wait('Custom Wait
            // Header','Custom Wait Message',_progress);
            // _fakeWaitProgress();
            break;
          case 'notify':
            dialogs.notify();
            break;
          case 'confirm':
            var dlg = dialogs.confirm();
            dlg.result.then(function(btn) {

              $scope.confirmed = 'You confirmed "Yes."';
            }, function(btn) {

              $scope.confirmed = 'You confirmed "No."';
            });
            break;
          case 'custom':
            var dlg = dialogs.create('/dialogs/custom.html',
              'customDialogCtrl', {}, {
                size: 'lg',
                keyboard: true,
                backdrop: 'static',
                windowClass: 'my-class'
              });
            dlg.result
              .then(
                function(name) {

                  $scope.name = name;
                },
                function() {

                  if (angular.equals($scope.name,
                      ''))
                    $scope.name = 'You did not enter in your name!';
                });
            break;
          case 'custom2':
            var dlg = dialogs.create('/dialogs/custom2.html',
              'customDialogCtrl2', $scope.custom, {
                size: 'lg'
              });
            break;
          case 'custom3':
            var dlg = dialogs
              .notify(
                'Message',
                'All is not supported, Please select interface(s)/version(s) to fetch real time federated coverage report.');
            break;
          case 'custom4':
            var dlg = dialogs
              .confirm(
                'Message',
                'You are about to fetch real time federated coverage report.This may take sometime!!!.');
            dlg.result.then(function(btn) {

              $scope.confirmed = 'You confirmed "Yes."';
            }, function(btn) {

              $scope.confirmed = 'You confirmed "No."';
            });
            break;
          case 'custom5':
            var dlg = dialogs.notify('Success',
              'Request has been successfully processed.');
            break;
          case 'custom6':
            var dlg = dialogs.notify('Message',
              'Please type Testscenario Name');
            break;
        }
      }; // end launch

      var _fakeWaitProgress = function() {

        $timeout(function() {

          if (_progress < 100) {
            _progress += 33;
            $rootScope.$broadcast('dialogs.wait.progress', {
              'progress': _progress
            });
            _fakeWaitProgress();
          } else {
            $rootScope.$broadcast('dialogs.wait.complete');
            _progress = 0;
          }
        }, 1000);
      };
    })
  .controller(
    'MenuCtrl', [
      '$scope',
      '$rootScope',
      '$timeout',
      'dialogs',
      '$location',
      'MenuService',
      'Datafactory',
      'userPreferencesService',
      'cldsModelService',
      'extraUserInfoService',
      function($scope, $rootScope, $timeout, dialogs,
        $location, MenuService, Datafactory,
        userPreferencesService, cldsModelService, extraUserInfoService) {
        console.log("MenuCtrl");
        $rootScope.screenName = "Universal Test Modeler";
        $rootScope.testSet = null;
        var testingType = "";
        $rootScope.contactUs = function() {
          console.log("contactUs");
          var link = "mailto:onap-discuss@lists.onap.org?subject=CLAMP&body=Please send us suggestions or feature enhancements or defect. If possible, please send us the steps to replicate any defect.";
          window.location.href = link;
        };

        extraUserInfoService
          .getUserInfo()
          .then(
            function(pars) {
              $scope.userInfo = pars;
              if (!($scope.userInfo["permissionUpdateCl"])) {
                readMOnly = true;
              };
            });

        $scope.emptyMenuClick = function(value, name) {
          if ($rootScope.isNewClosed &&
            name != "Save CL" &&
            name != "Close Model" &&
            name != "Properties CL") {
            saveConfirmationNotificationPopUp();
          } else {
            isSaveCheck(name);
          }

          function saveConfirmationNotificationPopUp() {
            $scope
              .saveConfirmationNotificationPopUp(function(
                data) {
                if (data) {
                  if ($rootScope.isNewClosed) {
                    isSaveCheck("Save CL");
                  } 
                  $rootScope.isNewClosed = false;
                } else {
                  return false;
                }
              });
          }

          function isSaveCheck(name) {
            if (name == "User Info") {
              $scope.extraUserInfo();
            } else if (name == "Wiki") {
              window.open(value);
            } else if (name == "Contact Us") {
              $rootScope.contactUs();
            } else if (name == "Revert Model Changes") {
              $scope.cldsRevertModel();
            } else if (name == "Close Model") {
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
              $scope.cldsPerformAction("SAVE");
            } else if (name == "Validation Test") {
              $scope.cldsPerformAction("TEST");
            } else if (name == "Submit") {
              $scope
                .cldsConfirmPerformAction("SUBMIT");
            } else if (name == "Resubmit") {
              $scope
                .cldsConfirmPerformAction("RESUBMIT");
            } else if (name == "Update") {
              $scope
                .cldsConfirmPerformAction("UPDATE");
            } else if (name.toLowerCase() == "delete") {
              $scope
                .manageConfirmPerformAction("DELETE");
            } else if (name == "Stop") {
              $scope.cldsConfirmPerformAction("STOP");
            } else if (name == "Restart") {
              $scope
                .cldsConfirmPerformAction("RESTART");
            } else if (name == "Refresh Status") {
              $scope.refreshStatus();
            } else if (name == "Properties CL") {
              $scope.cldsOpenModelProperties();
            } else if (name == "Deploy") {
              $scope
                .cldsAskDeployParametersPerformAction();
            } else if (name == "UnDeploy") {
              $scope
                .cldsConfirmToggleDeployPerformAction("UnDeploy");
            } else {
              $rootScope.screenName = name;
              $scope.updatebreadcrumb(value);
              $location.path(value);
            }
          }
        };

        $rootScope.impAlerts = function() {

        };

        $scope.tabs = {
          "Closed Loop": [{
            link: "/cldsCreateModel",
            name: "Create CL"
          }, {
            link: "/cldsOpenModel",
            name: "Open CL"
          }, {
            link: "/cldsSaveModel",
            name: "Save CL"
          }, {
            link: "/cldsOpenModelProperties",
            name: "Properties CL"
          }, {
            link: "/RevertChanges",
            name: "Revert Model Changes"
          }, {
            link: "/Close",
            name: "Close Model"
          }],
          "Manage": [{
            link: "/cldsTestActivate",
            name: "Validation Test"
          }, {
            link: "/cldsSubmit",
            name: "Submit"
          }, {
            link: "/cldsResubmit",
            name: "Resubmit"
          }, {
            link: "/cldsUpdate",
            name: "Update"
          }, {
            link: "/cldsStop",
            name: "Stop"
          }, {
            link: "/cldsRestart",
            name: "Restart"
          }, {
            link: "/cldsDelete",
            name: "Delete"
          }, {
            link: "/cldsDeploy",
            name: "Deploy"
          }, {
            link: "/cldsUnDeploy",
            name: "UnDeploy"
          }],
          "View": [{
            link: "/refreshStatus",
            name: "Refresh Status"
          }, {
            link: "/cldsRefreshASDC",
            name: "Refresh ASDC"
          }],
          "Help": [{
            link: "http://wiki.onap.org",
            name: "Wiki"
          }, {
            link: "/contact_us",
            name: "Contact Us"
          }, {
            link: "/extraUserInfo",
            name: "User Info"
          }]
        };

        if (!Object.keys) {
          Object.keys = function(obj) {
            var keys = [];

            for (var i in obj) {
              if (obj.hasOwnProperty(i)) {
                keys.push(i);
              }
            }

            return keys;
          };
          $scope.keyList = Object.keys($scope.tabs);
        } else {
          $scope.keyList = Object.keys($scope.tabs);
        }

        $scope.updatebreadcrumb = function(path) {

          var currentURL = $location.path();
          if (path != undefined) {
            currentURL = path;
          }

          if (currentURL == "/dashboard") {
            $rootScope.screenName = "Universal Test Modeler";
            $rootScope.parentMenu = "Home";
            $rootScope.rightTabName = "UTM Build Configuration";
          }
          /*
           * else if(currentURL=="/quicksearch") {
           * $rootScope.screenName = "Quick Search";
           * $rootScope.parentMenu = "Home"; }
           */
          else {
            var found = false;

            angular
              .forEach(
                $scope.keyList,
                function(value, key) {

                  if (!found) {
                    $rootScope.parentMenu = value;

                    angular
                      .forEach(
                        $scope.tabs[value],
                        function(
                          value,
                          key) {

                          if (currentURL == value.link) {
                            $rootScope.screenName = value.name;
                            found = true;
                          }
                        });
                  }
                });
          }
        };

        $scope.updatebreadcrumb();

        $scope.createNewProject = function() {

          if ($rootScope.projectName != null) {
            var dlg = dialogs
              .confirm('Message',
                'Do you want to over-write  the project ?');

            dlg.result
              .then(
                function(btn) {

                  $scope.clearProject();
                  var dlg1 = dialogs
                    .create(
                      'partials/portfolios/create_new_project.html',
                      'CreateNewPrjCtrl', {}, {
                        size: 'sm',
                        keyboard: true,
                        backdrop: false,
                        windowClass: 'my-class'
                      });
                  dlg1.result.then(
                    function(name) {

                      // $scope.name
                      // = name;
                    },
                    function() {

                      // if(angular.equals($scope.name,''))
                      // $scope.name
                      // = 'You
                      // did not
                      // enter in
                      // your
                      // name!';
                    });
                },
                function(btn) {

                  // $modalInstance.close("closed");
                });

          } else {
            var dlg = dialogs
              .create(
                'partials/portfolios/create_new_project.html',
                'CreateNewPrjCtrl', {}, {
                  size: 'lg',
                  keyboard: true,
                  backdrop: false,
                  windowClass: 'my-class'
                });
            dlg.result.then(function(name) {

              // $scope.name = name;
            }, function() {

              // if(angular.equals($scope.name,''))
              // $scope.name = 'You did not enter in
              // your name!';
            });

          }
        };

        $scope.clearProject = function() {

          $rootScope.projectName = null;
          $rootScope.revision = -1;
          // $rootScope.models.length=0;
          $rootScope.utmModels = $rootScope.$new(true);
          $rootScope.serviceInfo = $rootScope.$new(true);
          $rootScope.serviceInfo = null;
          $rootScope.serviceInputPartInfo = $rootScope
            .$new(true);
          $rootScope.serviceOutputPartInfo = $rootScope
            .$new(true);
          $rootScope.servicefaultPartInfo = $rootScope
            .$new(true);
          $rootScope.isModel = false;
          $("#paletteDiv").load(
            './modeler/dist/index.html');
          $rootScope.isPalette = false;
          $rootScope.isTestset = false;
          $rootScope.isRequirementCoverage = false;
          $rootScope.ispropertyExplorer = false;
          // $("#propertyDiv").load('./partials/portfolios/Property_Explorer.html');
          $rootScope.modelName = "";
          // document.getElementById('propertyExplorer').classList.remove('visible');
          document.getElementById("modeler_name").textContent = "Activity Modeler";
          // $( "#propertyExplorer" ).prev().css(
          // "display", "block" );
          $("#activity_modeler").prev().css("display",
            "block");
          $('div').find('.k-expand-next').click();

          $rootScope.$apply();

        };

        $scope.homePage = function() {

          $location.path('/dashboard');
        };
        $scope.propertyExplorerErrorMessage = function(msg) {

          var dlg = dialogs.notify('Error', msg);
        }

        // $scope.fromTstMultipleFlag=false;
        /* onclicking of review testset / generate testset */

        $scope.reviewTestSet = function() {

          $rootScope.modeltestset = list_model_test_sets[selected_model];

          $rootScope.isPalette = false;
          $rootScope.isTestset = true;
          $rootScope.isRequirementCoverage = false;
          document.getElementById("modeler_name").textContent = "UTM Test Set";
          // document.getElementById('propertyExplorer').classList.add('visible');

          // $( "#propertyExplorer" ).prev().css(
          // "display", "none" );
          $('div').find('.k-collapse-next').click();

          // $rootScope.$apply();

        };
        $scope.requirementCoverage = function() {

          $rootScope.testCaseRequirements = [];
          $rootScope.validTestRequirementArray = [];
          $rootScope.validTestRequirements = {};
          $rootScope.modeltestset = list_model_test_sets[selected_model];
          var allPathDetails = [];
          $scope.currentSelectedModel = {};
          // $scope.getPathDetails($rootScope.utmModels,selected_model);
          // $scope.populatePathDetails(allPathDetails,$scope.currentSelectedModel);
          $rootScope.pathDetailsList = list_model_path_details[selected_model];
          /*
           * for(var p=0;p<100;p++){
           * $rootScope.testCaseRequirements.push("Requirement"+p); }
           * for(var p=0;p<100;p++){
           * $rootScope.testCaseValue.push("TestCase"+p); }
           */
          for (var x = 0; x < allPathDetails.length; x++) {
            var tempPathDetails = allPathDetails[x];
            if (tempPathDetails != null) {
              for (var i = 0; i < tempPathDetails.length; i++) {
                var pathDetails = tempPathDetails[i];
                if (pathDetails.requirement !== '' &&
                  pathDetails.requirement !== null) {
                  $rootScope.testCaseRequirements
                    .push(pathDetails.requirement);
                }

                /*
                 * for (var j = 0; j <
                 * pathDetails.decisionIdentifiers.length;
                 * j++) {
                 * if(pathDetails.decisionIdentifiers[j].requirement
                 * !== '' &&
                 * pathDetails.decisionIdentifiers[j].requirement
                 * !== null){
                 * $rootScope.testCaseRequirements.push(pathDetails.decisionIdentifiers[j].requirement); } }
                 */
              }
            }

          }
          for (var p = 0; p < $rootScope.modeltestset.activityTestCases.length; p++) {
            var activityTestCases = $rootScope.modeltestset.activityTestCases[p];
            if (activityTestCases.mappedRequirements != null) {
              for (var i = 0; i < activityTestCases.mappedRequirements.length; i++) {
                // $rootScope.testCaseRequirements
                // .push(activityTestCases.mappedRequirements[i]);
                var testCaseNames = $rootScope.validTestRequirements[activityTestCases.mappedRequirements[i]];
                if (testCaseNames == null) {
                  testCaseNames = [];
                }
                if (activityTestCases.version != null)
                  var testCase = activityTestCases.testCaseName +
                    "_" +
                    activityTestCases.version;
                else
                  var testCase = activityTestCases.testCaseName;
                testCaseNames.push(testCase);
                $rootScope.validTestRequirements[activityTestCases.mappedRequirements[i]] = testCaseNames;
              }
            }
          }

          $rootScope.isPalette = false;
          $rootScope.isTestset = false;
          $rootScope.isRequirementCoverage = true;
          document.getElementById("modeler_name").textContent = "Test Case / Requirement Coverage";
          // document.getElementById('propertyExplorer').classList.add('visible');
          // console.log("modeltestset"+JSON.stringify($rootScope.modeltestset));
          // $( "#propertyExplorer" ).prev().css(
          // "display", "none" );
          $('div').find('.k-collapse-next').click();
          // $rootScope.$apply();

        };

        $scope.activityModelling = function() {

          // window.open("./bpmn-js-examples-master/modeler/dist/index.html",
          // "_self");
          // $location.path('/activity_modelling');
        };
        /*
         * $scope.openProject = function(){
         * $location.path('/dashboard_upload'); };
         */

        $scope.cldsClose = function() {

          var dlg = dialogs
            .create(
              'partials/portfolios/confirmation_window.html',
              'CldsOpenModelCtrl', {
                closable: true,
                draggable: true
              }, {
                size: 'lg',
                keyboard: true,
                backdrop: 'static',
                windowClass: 'my-class'
              });

          dlg.result.then(function(name) {

            // $scope.name = name;
          }, function() {

            // if(angular.equals($scope.name,''))
            // $scope.name = 'You did not enter in your
            // name!';
          });
        };
        $scope.saveConfirmationNotificationPopUp = function(
          callBack) {

          var dlg = dialogs
            .create(
              'partials/portfolios/save_confirmation.html',
              'saveConfirmationModalPopUpCtrl', {
                closable: true,
                draggable: true
              }, {
                size: 'lg',
                keyboard: true,
                backdrop: 'static',
                windowClass: 'my-class'
              });

          dlg.result.then(function(name) {
            callBack("OK");
          }, function() {
            callBack(null);
          });

        };

        $scope.cldsRefreshASDC = function() {

          var dlg = dialogs
            .create(
              'partials/portfolios/refresh_asdc.html',
              'CldsOpenModelCtrl', {
                closable: true,
                draggable: true
              }, {
                size: 'lg',
                keyboard: true,
                backdrop: 'static',
                windowClass: 'my-class'
              });
          dlg.result.then(function(name) {

            // $scope.name = name;
          }, function() {

            // if(angular.equals($scope.name,''))
            // $scope.name = 'You did not enter in your
            // name!';
          });
        }
        $scope.cldsRevertModel = function() {

          var dlg = dialogs
            .create(
              'partials/portfolios/ConfirmRevertChanges.html',
              'CldsOpenModelCtrl', {
                closable: true,
                draggable: true
              }, {
                size: 'lg',
                keyboard: true,
                backdrop: 'static',
                windowClass: 'my-class'
              });

          dlg.result.then(function(name) {

            // $scope.name = name;
          }, function() {

            // if(angular.equals($scope.name,''))
            // $scope.name = 'You did not enter in your
            // name!';
          });

        };

        $rootScope.cldsOpenModelProperties = function() {

          var dlg = dialogs
            .create(
              'partials/portfolios/global_properties.html',
              'GlobalPropertiesCtrl', {}, {
                size: 'lg',
                keyboard: true,
                backdrop: 'static',
                windowClass: 'my-class'
              });

          dlg.result.then(function(name) {}, function() {});
        };

        $scope.cldsOpenModel = function() {

          var dlg = dialogs
            .create(
              'partials/portfolios/clds_open_model.html',
              'CldsOpenModelCtrl', {
                closable: true,
                draggable: true
              }, {
                size: 'lg',
                keyboard: true,
                backdrop: 'static',
                windowClass: 'my-class'
              });

          dlg.result.then(function(name) {

            // $scope.name = name;
          }, function() {

            // if(angular.equals($scope.name,''))
            // $scope.name = 'You did not enter in your
            // name!';
          });
        };
        $scope.cldsCreateModel = function() {

          var dlg = dialogs
            .create(
              'partials/portfolios/clds_create_model_off_Template.html',
              'CldsOpenModelCtrl', {
                closable: true,
                draggable: true
              }, {
                size: 'lg',
                keyboard: true,
                backdrop: 'static',
                windowClass: 'my-class'
              });

          dlg.result.then(function(name) {

            // $scope.name = name;
          }, function() {

            // if(angular.equals($scope.name,''))
            // $scope.name = 'You did not enter in your
            // name!';
          });

        };
        $scope.extraUserInfo = function() {

          var dlg = dialogs
            .create(
              'partials/portfolios/extra_user_info.html',
              'ExtraUserInfoCtrl', {
                closable: true,
                draggable: true
              }, {
                size: 'lg',
                keyboard: true,
                backdrop: 'static',
                windowClass: 'my-class'
              });

          dlg.result.then(function(name) {}, function() {});

        };
        $scope.cldsPerformAction = function(uiAction) {

          var modelName = selected_model;
          var controlNamePrefix = "ClosedLoop-";
          var bpmnText = modelXML;
          // serialize model properties
          var propText = JSON.stringify(elementMap);
          var templateName = selected_template

          var svgXml = $("#svgContainer").html(); 

          console.log("cldsPerformAction: " + uiAction +
            " modelName=" + modelName);
          console.log("cldsPerformAction: " + uiAction +
            " controlNamePrefix=" +
            controlNamePrefix);
          console.log("cldsPerformAction: " + uiAction +
            " bpmnText=" + bpmnText);
          console.log("cldsPerformAction: " + uiAction +
            " propText=" + propText);
          console.log("cldsPerformAction: " + uiAction +
            " typeID=" + typeID);
          console.log("cldsPerformAction: " + uiAction +
            " deploymentId=" + deploymentId);
          cldsModelService
            .processAction(uiAction, modelName,
              controlNamePrefix, bpmnText,
              propText, svgXml, templateName,
              typeID, deploymentId)
            .then(
              function(pars) {
                console
                  .log("cldsPerformAction: pars=" +
                    pars);
                cldsModelService
                  .processRefresh(pars);
              },
              function(data) {

                // alert("setModel failed: "
                // + data);
              });
        };
        $scope.refreshStatus = function() {
            var modelName = selected_model;
            var svgXml = $("#svgContainer").html(); 
            console.log("refreStatus modelName=" + modelName);
            cldsModelService
              .getModel(modelName)
              .then(
                function(pars) {
                  console
                    .log("refreStatus: pars=" +
                      pars);
                  cldsModelService
                    .processRefresh(pars);
                },
                function(data) {
                });
          };
        $scope.cldsConfirmPerformAction = function(uiAction) {
          var dlg = dialogs.confirm('Message',
            'Do you want to ' +
            uiAction.toLowerCase() +
            ' the closed loop?');
          dlg.result.then(function(btn) {

            $scope.cldsPerformAction(uiAction);
          }, function(btn) {

            // $modalInstance.close("closed");
          });
        };

        $scope.cldsAskDeployParametersPerformAction = function() {
            var dlg = dialogs.create('partials/portfolios/deploy_parameters.html',
                                     'DeploymentCtrl',
                                     {}, {keyboard: true, backdrop: true, windowClass: 'deploy-parameters'});
            dlg.result.then(function() {
                var confirm = dialogs.confirm('Deploy', 'Are you sure you want to deploy the closed loop?');
                confirm.result.then(function() {
                    cldsToggleDeploy("deploy");
                });
            });
        };

        $scope.cldsConfirmToggleDeployPerformAction = function(
          uiAction) {

          var dlg = dialogs.confirm('Message',
            'Do you want to ' +
            uiAction.toLowerCase() +
            ' the closed loop?');
          dlg.result.then(function(btn) {
            cldsToggleDeploy(uiAction.toLowerCase());
          }, function(btn) {

            // $modalInstance.close("closed");
          });
        };

        function cldsToggleDeploy(uiAction) {
          var modelName = selected_model;
          var controlNamePrefix = "ClosedLoop-";
          var bpmnText = modelXML;
          // serialize model properties
          var propText = JSON.stringify(elementMap);
          var templateName = selected_template;
          var svgXml = $("#svgContainer").html();

          console.log("cldsPerformAction: " + uiAction +
            " modelName=" + modelName);
          console.log("cldsPerformAction: " + uiAction +
            " controlNamePrefix=" +
            controlNamePrefix);
          console.log("cldsPerformAction: " + uiAction +
            " bpmnText=" + bpmnText);
          console.log("cldsPerformAction: " + uiAction +
            " propText=" + propText);
          console.log("cldsPerformAction: " + uiAction +
            " modelEventService=" +
            modelEventService);
          console.log("cldsPerformAction: " + uiAction +
            " typeID=" + typeID);
          console.log("cldsPerformAction: " + uiAction +
            " deploymentId=" + deploymentId);
          cldsModelService
            .toggleDeploy(uiAction, modelName,
              controlNamePrefix, bpmnText,
              propText, svgXml, templateName,
              typeID, controlNameUuid,
              modelEventService, deploymentId)
            .then(
              function(pars) {
                typeID = pars.typeId;
                controlNameUuid = pars.controlNameUuid;
                selected_template = pars.templateName;
                modelEventService = pars.event;
                // actionCd =
                // pars.event.actionCd;
                actionStateCd = pars.event.actionStateCd;
                deploymentId = pars.deploymentId;
                cldsModelService
                  .processActionResponse(
                    modelName,
                    pars);

              },
              function(data) {

              });
        }
        $scope.managePerformAction = function(action) {
          if (action.toLowerCase() === "delete") {
            cldsModelService
              .manageAction(
                selected_model,
                "805b9f83-261f-48d9-98c7-8011fc2cc8e8",
                "ClosedLoop-ABCD-0000.yml")
              .then(function(pars) {

              }, function(data) {

                // alert("setModel failed: " +
                // data);
              });
          }
        };
        $scope.manageConfirmPerformAction = function(
          uiAction) {

          var dlg = dialogs.confirm('Message',
            'Do you want to ' +
            uiAction.toLowerCase() +
            ' the closed loop?');
          dlg.result.then(function(btn) {

            $scope.managePerformAction(uiAction);
          }, function(btn) {

            // $modalInstance.close("closed");
          });
        };
        $scope.VesCollectorWindow = function(vesCollector) {

            var dlg = dialogs
              .create(
                'partials/portfolios/vesCollector_properties.html',
                'ImportSchemaCtrl', {
                  closable: true,
                  draggable: true
                }, {
                  size: 'lg',
                  keyboard: true,
                  backdrop: 'static',
                  windowClass: 'my-class'
                });

            dlg.result.then(function(name) {

            }, function() {

            });


        };

        $scope.HolmesWindow = function(holmes) {

          var partial = 'partials/portfolios/holmes_properties.html'

          var dlg = dialogs
            .create(
              partial,
              'ImportSchemaCtrl',
              holmes, {
                closable: true,
                draggable: true
              }, {
                size: 'lg',
                keyboard: true,
                backdrop: 'static',
                windowClass: 'my-class'
              });
        };

        $scope.TCAWindow = function(tca) {

            var dlg = dialogs
              .create(
                'partials/portfolios/tca_properties.html',
                'ImportSchemaCtrl', {
                  closable: true,
                  draggable: true
                }, {
                  size: 'lg',
                  keyboard: true,
                  backdrop: 'static',
                  windowClass: 'my-class'
                });

            dlg.result.then(function(name) {
              // $scope.name = name;
            }, function() {
              // if(angular.equals($scope.name,''))
              // $scope.name = 'You did not enter in
              // your name!';
            });

        };

        $scope.PolicyWindow = function(policy) {
            var dlg = dialogs
              .create(
                'partials/portfolios/PolicyWindow_properties.html',
                'ImportSchemaCtrl', {
                  closable: true,
                  draggable: true
                }, {
                  size: 'lg',
                  keyboard: true,
                  backdrop: 'static',
                  windowClass: 'my-class'
                });

            dlg.result.then(function(name) {

              // $scope.name = name;
            }, function() {

              // if(angular.equals($scope.name,''))
              // $scope.name = 'You did not enter in
              // your name!';
            });

        };

      }
    ]);

app.service('MenuService', ['$http', '$q', function($http, $q) {

  /*
   * this.generateMDTTestSet = function(utmMDTRequest, generateTestSetMDTURL){
   *
   * console.log("generateMDTTestSet"); //alert("In generateMDTTestSet :: " +
   * JSON.stringify(utmMDTRequest)); var def = $q.defer(); var sets = [];
   *
   * $http.post(generateTestSetMDTURL, utmMDTRequest) .success(function(data){
   * console.log("success"); sets = data; def.resolve(data); })
   * .error(function(data){ console.log("error");
   * def.reject("GenerateMDTTestSet not successful"); });
   *
   * return def.promise; };
   */
}]);

app.directive('focus', function($timeout) {

  return {
    scope: {
      trigger: '@focus'
    },
    link: function(scope, element) {
      scope.$watch('trigger', function(value) {

        if (value === "true") {
          $timeout(function() {

            element[0].focus();
          });
        }
      });
    }
  };
});
app.directive('draggable', function($document) {

  return function(scope, element, attr) {

    var startX = 0,
      startY = 0,
      x = 0,
      y = 0;
    element.css({
      position: 'relative',

      backgroundColor: 'white',
      cursor: 'move',
      display: 'block',

    });
    element.on('mousedown', function(event) {

      // Prevent default dragging of selected content
      // event.preventDefault();
      startX = event.screenX - x;
      startY = event.screenY - y;
      $document.on('mousemove', mousemove);
      $document.on('mouseup', mouseup);
    });

    function mousemove(event) {

      y = event.screenY - startY;
      x = event.screenX - startX;
      element.css({
        top: y + 'px',
        left: x + 'px'
      });
    }

    function mouseup() {

      $document.off('mousemove', mousemove);
      $document.off('mouseup', mouseup);
    }
  };
});

app.factory('myHttpInterceptor', function($q, $window) {

  return function(promise) {

    return promise.then(function(response) {

      return response;
    }, function(response) {

      return $q.reject(response);
    });
  };
});

app.run(['$route', function($route) {

  $route.reload();
}]);

function TestCtrl($scope) {

  $scope.msg = "Hello from a controller method.";
  $scope.returnHello = function() {

    return $scope.msg;
  }
}

function importshema() {

  angular.element(document.getElementById('navbar')).scope().importSchema();

}

function VesCollectorWindow(vesCollectorWin) {
  angular.element(document.getElementById('navbar')).scope()
    .VesCollectorWindow(vesCollectorWin);
}

function HolmesWindow(holmesWin) {
  angular.element(document.getElementById('navbar')).scope()
    .HolmesWindow(holmesWin);
}

function F5Window() {

  angular.element(document.getElementById('navbar')).scope().F5Window();

}

function TCAWindow(tca) {

  angular.element(document.getElementById('navbar')).scope().TCAWindow(tca);

}

function GOCWindow() {

  angular.element(document.getElementById('navbar')).scope().GOCWindow();

}

function PolicyWindow(PolicyWin) {

  angular.element(document.getElementById('navbar')).scope().PolicyWindow(
    PolicyWin);

}

function pathDetails(bpmnElementID, bpmnElementName, pathIdentifiers) {

  angular.element(document.getElementById('navbar')).scope().pathDetails(
    bpmnElementID, bpmnElementName, pathIdentifiers);

}

function setdefaultvalue() {

  angular.element(document.getElementById('navbar')).scope()
    .setDefaultValue();

}

function upgradeSchemaVersion() {

  angular.element(document.getElementById('navbar')).scope()
    .upgradeSchemaVersion();

}

function saveProject() {

  angular.element(document.getElementById('navbar')).scope().saveProject();

}

function modifySchema() {

  angular.element(document.getElementById('navbar')).scope().modifySchema();

}

function definePID() {

  angular.element(document.getElementById('navbar')).scope().definePID();

}

function defineServiceAcronym() {

  angular.element(document.getElementById('navbar')).scope()
    .defineServiceAcronym();

}

function errorProperty(msg) {

  angular.element(document.getElementById('navbar')).scope()
    .propertyExplorerErrorMessage(msg);
}

function invisiblepropertyExplorer() {

  angular.element(document.getElementById('navbar')).scope()
    .invisibleproperty();
}

function updateDecisionLabel(originalLabel, newLabel) {

  angular.element(document.getElementById('navbar')).scope()
    .updateDecisionLabels(originalLabel, newLabel);
}

// Used to logout the session , when browser window was closed
window.onunload = function() {
  window.localStorage.removeItem("isAuth");
  window.localStorage.removeItem("loginuser");
  window.localStorage.removeItem("invalidUser");
};
