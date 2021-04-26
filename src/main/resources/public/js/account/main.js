angular.module('aac.controllers.main', [])

/**
 * Main layout controller
 * @param $scope
 */
.controller('MainCtrl', function($scope, $rootScope, $location, Data, Utils) {
  $scope.go = function(v) {
  	$location.path(v);
  }

	
	$scope.activeView = function(view) {
		return view == $rootScope.currentView ? 'active' : '';
	};
	$scope.signOut = function() {
	    window.document.location = "./logout";
	};
	
	Data.getProfile().then(function(data) {
		$rootScope.user = data;
		$rootScope.isDev = data.authorities.findIndex(function(a) {
		   return a.role === 'ROLE_DEVELOPER' || a.role === 'ROLE_ADMIN'; 
		})>= 0;
	}).catch(function(err) {
		Utils.showError(err);
	});
	
	//Utils.initUI();
})

.controller('HomeController', function($scope, $rootScope, $location) {
})
.controller('AccountsController', function($scope, $rootScope, $location, Data, Utils) {
	Data.getAccounts().then(function(data) {
	  var providers = [];
    var accounts = {};
    data.forEach(function(a) {
      if (a.provider !== 'internal') providers.push(a.provider);
      accounts[a.provider] = Object.assign({},a.attributes);
      accounts[a.provider].username = a.username;      
    });
    $scope.providers = providers;
    $scope.accounts = accounts;
	}).catch(function(err) {
		Utils.showError(err);
	});
	
	$scope.confirmDeleteAccount = function() {
		$('#deleteConfirm').modal({keyboard: false});
	}
	
	$scope.deleteAccount = function() {
		$('#deleteConfirm').modal('hide');
		Data.deleteAccount().then(function() {
			window.location.href = './logout';
		}).catch(function(err) {
			Utils.showError(err);
		});
	}
	
})
.controller('ConnectionsController', function($scope, $rootScope, $location, Data, Utils) {
	Data.getConnections().then(function(connections) {
		$scope.connections = connections;
	}).catch(function(err) {
		Utils.showError(err);
	});
	
	$scope.confirmDeleteApp = function(app) {
		$scope.clientId = app.clientId;
		$('#deleteConfirm').modal({keyboard: false});
	}
	
	$scope.deleteApp = function() {
		$('#deleteConfirm').modal('hide');
		Data.removeConnection($scope.clientId).then(function(connections) {
			$scope.connections = connections;
			Utils.showSuccess();
		}).catch(function(err) {
			Utils.showError(err);
		});
	}
	
})
.controller('ProfileController', function($scope, $rootScope, $location, Data, Utils) {
	$scope.profile = {name: $rootScope.user.firstName, surname: $rootScope.user.lastName, username: $rootScope.user.username, email: $rootScope.user.emailAddress};
	
	$scope.cancel = function() {
		window.history.back();
	}
	
	$scope.save = function() {
		if (!$scope.profile.name ||
			!$scope.profile.surname ||
			!$scope.profile.username ||
			$scope.profile.password && $scope.profile.password != $scope.profile.password2) 
		{
			return;
		}
		Data.saveAccount($scope.profile).then(function(data) {
			$rootScope.user = data;
			$scope.profile = Object.assign($rootScope.user);
			Utils.showSuccess();
		}).catch(function(err) {
			Utils.showError(err);
		});
	}
	Utils.initUI();
})
;