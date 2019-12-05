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
		data.fullname = data.name + ' ' + data.surname;
		$rootScope.user = data;
	}).catch(function(err) {
		Utils.showError(err.message);
	});
})

.controller('HomeController', function($scope, $rootScope, $location) {
})
.controller('AccountsController', function($scope, $rootScope, $location, Data, Utils) {
	Data.getAccounts().then(function(data) {
		Data.getProviders().then(function(providers) {
			providers.sort(function(a,b) {
				if (data.accounts[a] && !data.accounts[b]) return -1;
				if (data.accounts[b] && !data.accounts[a]) return 1;
				return a.localeCompare(b);
			});
			$scope.providers = providers;
			var accounts = {};
			for (var p in data.accounts) {
				var amap = {};
				for (var k in data.accounts[p]) {
					if (k === 'it.smartcommunitylab.aac.surname') amap['surname'] = data.accounts[p][k];
					else if (k === 'it.smartcommunitylab.aac.givenname') amap['givenname'] = data.accounts[p][k];
					else if (k === 'it.smartcommunitylab.aac.username') amap['username'] = data.accounts[p][k];
					else amap[k] = data.accounts[p][k];
				}
				accounts[p] = amap;
			}
			$scope.accounts = accounts;
		}).catch(function(err) {
			Utils.showError(err.message);
		});
	}).catch(function(err) {
		Utils.showError(err.message);
	});
	
	$scope.confirmDeleteAccount = function() {
		$('#deleteConfirm').modal({keyboard: false});
	}
	
	$scope.deleteAccount = function() {
		$('#deleteConfirm').modal('hide');
		Data.deleteAccount().then(function() {
			window.location.href = './logout';
		}).catch(function(err) {
			Utils.showError(err.message);
		});
	}
	
})
.controller('ConnectionsController', function($scope, $rootScope, $location, Data) {
})
.controller('ProfileController', function($scope, $rootScope, $location, Data) {
})
;