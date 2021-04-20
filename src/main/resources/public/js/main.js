angular.module('aac.controllers.main', [])

/**
 * Main layout controller
 * @param $scope
 */
.controller('MainCtrl', function($scope, $rootScope, $location, Data, Utils) {
  Data.getProfile().then(function(data) {
    $rootScope.user = data;
    
    $rootScope.isAdmin = data.authorities.findIndex(function(a) {
        return a.authority === 'ROLE_ADMIN';
    }) >= 0;
    if ($rootScope.isAdmin) $scope.go('admin');
    
  }).catch(function(err) {
    Utils.showError(err);
  });
  
  $scope.go = function(v) {
  	$location.path(v);
  }
	
	$scope.activeView = function(view) {
		return view == $rootScope.currentView ? 'active' : '';
	};
	$scope.signOut = function() {
	    window.document.location = "./logout";
	};
});