angular.module('aac.controllers.apis', [])

/**
 * List of Tenant space User Roles controller
 */
.controller('TenantUsersController', function($scope, $rootScope, $location, Data, Utils) {
	$scope.contexts = {selected: null, all : null}; 
	
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

	// load users
	var loadUsers = function(){
	    Data.getContextUsers($scope.page.offset, $scope.page.limit, $scope.contexts.selected).then(function(data){
	    	data.list.forEach(function(d) {
	    		d.roles = d.roles.map(function(r) {
	    			return r.role;
	    		});
	    	});
	    	$scope.users = data.list;
	    	if (!data.count) data.count = data.list.length;
			var count = (($scope.page.currentPage-1) * $scope.page.limit + data.count);
			$scope.page.totalItems = 
				data.count < $scope.page.limit ? count : (count + 1);			
	    }, Utils.showError);
	}
	
	// load data
	var loadData = function(){
		if (!$scope.contexts.all) {
			Data.getMyContexts().then(function(data) {
				$scope.contexts.all = data;
				$scope.contexts.selected = data && data.length > 0 ? data[0] : null;
				if ($scope.contexts.selected) loadUsers();
			}, Utils.showError);
		} else {
			loadUsers();
		}
	}
	$scope.loadData = loadData;
	
	reset();
	loadData();

	// toggle roles of the subscribed user
	$scope.changeRoles = function(sub) {
		if (!$scope.contexts.selected) return;
		
		var roleMap = {};
		if (sub.roles){
			sub.roles.forEach(function(r){
				roleMap[r] = true;
			});
		}
		$scope.roles = {map: roleMap, sub: sub};

		$('#rolesModal').modal({backdrop: 'static', focus: true})
	}
	
	// add roles to a new user
	$scope.newUser = function() {
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
		// TODO fix the implementation and the error handling
		Data.updateUserRolesInContext($scope.roles.sub.username, $scope.roles.map, $scope.roles.sub.roles, $scope.contexts.selected).then(function(newRoles) {
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
	$scope.changeRole = function() {
		console.log('changed');
	}
	
})

/**
 * List of Tenant space User Owners controller
 */
.controller('TenantOwnersController', function($scope, $rootScope, $location, Data, Utils) {
	$scope.contexts = {selected: null, all: null}; 
	
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

	var loadUsers = function() {
	    Data.getContextOwners($scope.page.offset, $scope.page.limit, $scope.contexts.selected).then(function(data){
	    	data.list.forEach(function(d) {
	    		d.roles = d.roles.map(function(r) {
	    			return r.space;
	    		});
	    	});
	    	$scope.users = data.list;
	    	if (!data.count) data.count = data.list.length;
			var count = (($scope.page.currentPage-1) * $scope.page.limit + data.count);
			$scope.page.totalItems = 
				data.count < $scope.page.limit ? count : (count + 1);			
	    }, Utils.showError);		
	}	
	
	// load data
	var loadData = function(){
		if (!$scope.contexts.all) {
			Data.getMyContexts().then(function(data) {
				$scope.contexts.all = data;
				$scope.contexts.selected = data && data.length > 0 ? data[0] : null;
				if ($scope.contexts.selected) loadUsers();
			}, Utils.showError);
		} else {
			loadUsers();
		}
	}
	var updateContexts = function() {
		Data.getMyContexts().then(function(data) {
			$scope.contexts.all = data;
		});	

	}
	
	$scope.loadData = loadData;
	
	reset();
	loadData();

	// toggle roles of the subscribed user
	$scope.changeRoles = function(sub) {
		if (!$scope.contexts.selected) return;
		
		var roleMap = {};
		if (sub.roles){
			sub.roles.forEach(function(r){
				roleMap[r] = true;
			});
		}
		$scope.roles = {map: roleMap, sub: sub};

		$('#rolesModal').modal({backdrop: 'static', focus: true})
	}
	
	// add roles to a new user
	$scope.newUser = function() {
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
		Data.updateOwnersInContext($scope.roles.sub.username, $scope.roles.map, $scope.roles.sub.roles, $scope.contexts.selected).then(function(newRoles) {
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
			updateContexts();
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