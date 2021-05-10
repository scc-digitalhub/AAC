angular.module('aac.controllers.main', [])

/**
 * Main layout controller
 * @param $scope
 */
.controller('MainCtrl', function($scope, $rootScope, $location, Data, Utils) {
  Data.getProfile().then(function(p) {
    Data.getProfileRoles().then(function(roles) {
      $scope.hasRolespaces = !!roles && roles.length > 0;
    })
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
})
.controller('HomeController', function($scope, $rootScope, $location, Data, Utils) {
  if (!$rootScope.user) {
    Data.getProfile().then(function() {
      if ($rootScope.isAdmin) $scope.go('admin');
      else $scope.go('realm');
    }).catch(function(err) {
      Utils.showError(err);
    });
  } else {
    if ($rootScope.isAdmin) $scope.go('admin');
    else $scope.go('realm');
  }
});