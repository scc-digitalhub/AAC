angular.module('aac.controllers.apis', [])
/**
 * Single API view controller
 */
.controller('APIController', function($scope, $location, $routeParams, Data, Utils) {
	$scope.page = {
	  offset: 0,
	  limit: 10,
	  totalItems: 0,
	  currentPage: 1
	};
	$scope.nextPage = function() {
		$scope.page.currentPage++;
		$scope.page.offset = ($scope.page.currentPage - 1) * $scope.page.limit;
		loadSubscriptions();
	};
	$scope.prevPage = function() {
		$scope.page.currentPage--;
		$scope.page.offset = ($scope.page.currentPage - 1) * $scope.page.limit;
		loadSubscriptions();
	};
		
	// load API subscriptions
	var loadSubscriptions = function() {
		Data.getAPISubscriptions($routeParams.apiId, $scope.page.offset, $scope.page.limit).then(function(subs){
			$scope.api.subscriptions = subs;
	    	if (!subs.count) subs.count = subs.list.length;
			var count = (($scope.page.currentPage-1) * $scope.page.limit + subs.count);
			$scope.page.totalItems = 
				subs.count < $scope.page.limit ? count : (count + 1);			
		}, Utils.showError);
	}
	// page changed
	$scope.pageChanged = function() {
		$scope.page.offset = ($scope.page.currentPage - 1)*$scope.page.limit;
		loadSubscriptions();
	};
	
	// load API info and subscriptions
	var load = function() {
		Data.getAPI($routeParams.apiId).then(function(data){
			$scope.api = data;
			loadSubscriptions();
		}, Utils.showError);	
	}
	load();
	
	$scope.addRole = function(){
		$scope.roles.map[$scope.roles.custom] = true;
		$scope.roles.custom = null;
	}
	
	// toggle roles of the subscribed user
	$scope.changeRoles = function(sub) {
		var roleMap = {};
		if (sub.roles){
			sub.roles.forEach(function(r){
				roleMap[r] = true;
			});
		}
		if ($scope.api.applicationRoles) {
			$scope.api.applicationRoles.forEach(function(r) {
				if (roleMap[r] == null) {
					roleMap[r] = false;
				}
			});
		}	
		$scope.roles = {map: roleMap, sub: sub};
		$('#rolesModal').modal({backdrop: 'static', focus: true})
//
//		$scope.rolesDlg = $uibModal.open({
//	      ariaLabelledBy: 'modal-title',
//	      ariaDescribedBy: 'modal-body',
//	      templateUrl: 'html/roles.modal.html',
//	      scope: $scope,
//	      size: 'lg'
//	    });

	}
	$scope.updateRoles = function() {
		Data.updateUserRoles($scope.roles.sub.subscriber, $scope.roles.map, $scope.roles.sub.roles).then(function(newRoles) {
			$scope.api.subscriptions.list.forEach(function(s) {
				if (s.subscriber == $scope.roles.sub.subscriber) {
					s.roles = newRoles;
				}
			});
			$('#rolesModal').modal('hide');
			Utils.showSuccess();
		}, Utils.showError);
	}
	$scope.dismiss = function(){
		$('#rolesModal').modal('hide');
	}

})

/**
 * List of APIs controller
 */
.controller('APIListController', function($scope, $location, Data, Utils) {
	$scope.page = {
	  offset: 0,
	  limit: 10,
	  totalItems: 0,
	  currentPage: 1
	};
	$scope.nextPage = function() {
		$scope.page.currentPage++;
		$scope.page.offset = ($scope.page.currentPage - 1) * $scope.page.limit;
		loadData();
	};
	$scope.prevPage = function() {
		$scope.page.currentPage--;
		$scope.page.offset = ($scope.page.currentPage - 1) * $scope.page.limit;
		loadData();
	};

	// page changed
	$scope.pageChanged = function() {
		$scope.page.offset = ($scope.page.currentPage - 1) * $scope.page.limit;
		loadData();
	};

	// load APIs
	var loadData = function(){
	    Data.getAPIs($scope.page.offset, $scope.page.limit).then(function(data){
	    	$scope.apis = data.list;
	    	if (!data.count) data.count = data.list.length;
			var count = (($scope.page.currentPage-1) * $scope.page.limit + data.count);
			$scope.page.totalItems = 
				data.count < $scope.page.limit ? count : (count + 1);			
	    }, Utils.showError);
	}
	
	loadData();

	// view API
    $scope.goAPI = function(api) {
    	$location.path('/apis/'+api.id);
    }
})

/**
 * List of APIs controller
 */
.controller('TenantUsersController', function($scope, $location, Data, Utils) {

	var reset = function() {
		$scope.page = {
		  offset: 0,
		  limit: 10,
		  totalItems: 0,
		  currentPage: 1
		};		
	}
	
	// page changed
	$scope.pageChanged = function() {
		$scope.page.offset = ($scope.page.currentPage - 1) * $scope.page.limit;
		loadData();
	};
	$scope.nextPage = function() {
		$scope.page.currentPage++;
		$scope.page.offset = ($scope.page.currentPage - 1) * $scope.page.limit;
		loadData();
	};
	$scope.prevPage = function() {
		$scope.page.currentPage--;
		$scope.page.offset = ($scope.page.currentPage - 1) * $scope.page.limit;
		loadData();
	};

	// load APIs
	var loadData = function(){
	    Data.getProviderUsers($scope.page.offset, $scope.page.limit).then(function(data){
	    	$scope.users = data.list;
	    	if (!data.count) data.count = data.list.length;
			var count = (($scope.page.currentPage-1) * $scope.page.limit + data.count);
			$scope.page.totalItems = 
				data.count < $scope.page.limit ? count : (count + 1);			
	    }, Utils.showError);
	}
	
	reset();
	loadData();

	// toggle roles of the subscribed user
	$scope.changeRoles = function(sub) {
		var roleMap = {};
		if (sub.roles){
			sub.roles.forEach(function(r){
				roleMap[r] = true;
			});
		}
		$scope.roles = {map: roleMap, sub: sub};

		$('#rolesModal').modal({backdrop: 'static', focus: true})
	}
	
	// toggle roles of the subscribed user
	$scope.newUser = function(sub) {
		var roleMap = {};
		$scope.roles = {map: roleMap, sub: {username: null, usernameRequired: true}};
		$('#rolesModal').modal({backdrop: 'static', focus: true})
	}

	$scope.hasRoles = function(map){
		var res = false;
		for (var r in map) res |= map[r];
		return res;
	}
	
	// save roles
	$scope.updateRoles = function() {
		Data.updateUserRoles($scope.roles.sub.username, $scope.roles.map, $scope.roles.sub.roles).then(function(newRoles) {
			$scope.users.forEach(function(u) {
				if (newRoles == null || newRoles.length == 0) {
					loadData();
				}
				else if (u.userId == $scope.roles.sub.userId) {
					u.roles = newRoles;
				}
			});
			if ($scope.roles.sub.usernameRequired) {
				reset();
				loadData();
			}
			$('#rolesModal').modal('hide');
			Utils.showSuccess();
			$scope.roles = null;
		}, function(err) {
			$('#rolesModal').modal('hide');
			Utils.showError(err);
		});
	}
	$scope.dismiss = function(){
		$('#rolesModal').modal('hide');
	}
	
	$scope.addRole = function(){
		$scope.roles.map[$scope.roles.custom] = true;
		$scope.roles.custom = null;
	}
	
})