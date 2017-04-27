angular.module('aac.controllers.apis', [])
/**
 * Single API view controller
 */
.controller('APIController', function($scope, $location, $routeParams, Data, Utils) {
	$scope.page = {
	  offset: 0,
	  limit: 25,
	  totalItems: 0,
	  currentPage: 1
	};
		
	// load API subscriptions
	var loadSubscriptions = function() {
		Data.getAPISubscriptions($routeParams.apiId, $scope.page.offset, $scope.page.limit).then(function(subs){
			$scope.api.subscriptions = subs;
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
			if (data.roles && data.roles.length > 0) {
				loadSubscriptions();
			}
		}, Utils.showError);	
	}
	load();
	
	// toggle roles of the subscribed user
	$scope.toggleRoles = function(sub) {
		// has less roles as required: assign all
		if ($scope.api.roles.length > sub.roles.length) {
			Data.updateUserRoles($routeParams.apiId, sub.subscriber, $scope.api.roles).then(function(newRoles) {
				sub.roles = newRoles;
			  	Utils.showSuccess();
			}, Utils.showError);
		// otherwise remove all the roles	
		} else {
			Data.updateUserRoles($routeParams.apiId, sub.subscriber, null, sub.roles).then(function(newRoles) {
				sub.roles = newRoles;
				Utils.showSuccess();
			}, Utils.showError);
		}
	}
})

/**
 * List of APIs controller
 */
.controller('APIListController', function($scope, $location, Data, Utils) {
	$scope.page = {
	  offset: 0,
	  limit: 25,
	  totalItems: 0,
	  currentPage: 1
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