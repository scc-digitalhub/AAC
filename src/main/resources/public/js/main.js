angular.module('aac.controllers.main', [])

/**
 * Main layout controller
 * @param $scope
 */
.controller('MainCtrl', function($scope, $location) {
	$scope.currentView = 'apps';
    $scope.go = function(v) {
    	$location.path(v);
    }

	
	$scope.activeView = function(view) {
		return view == $scope.currentView ? 'active' : '';
	};
	$scope.signOut = function() {
	    window.document.location = "./logout";
	};
});