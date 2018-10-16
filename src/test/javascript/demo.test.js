require('jquery/dist/jquery.min.js');
require('angular/angular.min.js');
require('angular-mocks/angular-mocks.js');
require('angular-route/angular-route.min.js');
require('angular-resource/angular-resource.min.js');
require('angular-cookies/angular-cookies.min.js');
require('angular-animate/angular-animate.min.js');
require('angular-sanitize/angular-sanitize.min.js');
require('angular-touch/angular-touch.min.js');
require('popper.js/dist/umd/popper.min.js');
require('bootstrap/dist/js/bootstrap.min.js');
require('angular-ui-bootstrap/dist/ui-bootstrap-tpls.js');
require('angular-loading-bar/src/loading-bar.js');
require('angular-dialog-service/dist/dialogs.js');
require('scripts/app.js');
require('scripts/DashboardCtrl.js');


describe('Dashboard ctrl tests', function() {

	beforeEach(angular.mock.module('clds-app'));

	var $controllerService;
	
	beforeEach(angular.mock.inject(function(_$controller_) {
		$controllerService = _$controller_;
	}));

	describe('$scope.showPalette', function() {

		it('test showPalette', function() {

			var $scopeTest = {};
			var $rootScopeTest = {};
			var $resourceTest = {};
			var $httpTest = {};
			var $timeoutTest = {};
			var $locationTest = {};
			var $intervalTest = function(){};
			var $controllerDashboard = $controllerService('DashboardCtrl', {
			    '$scope' : $scopeTest,
			    '$rootScope' : $rootScopeTest,
			    '$resource' : $resourceTest,
			    '$http' : $httpTest,
			    '$timeout' : $timeoutTest,
			    '$location' : $locationTest,
			    '$interval' : $intervalTest
			});
			$scopeTest.showPalette();
			expect($rootScopeTest.isModel).toEqual(true);
		});
	});
});