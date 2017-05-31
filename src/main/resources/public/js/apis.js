angular.module('aac.controllers.apis', [])
/**
 * Single API view controller
 */
.controller('APIController', function($scope, $location, $routeParams, $uibModal, Data, Utils) {
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
		$scope.rolesDlg = $uibModal.open({
	      ariaLabelledBy: 'modal-title',
	      ariaDescribedBy: 'modal-body',
	      templateUrl: 'html/roles.modal.html',
	      scope: $scope,
	      size: 'lg'
	    });

	}
	$scope.updateRoles = function() {
		Data.updateUserRoles($routeParams.apiId, $scope.roles.sub.subscriber, $scope.roles.map, $scope.roles.sub.roles).then(function(newRoles) {
			$scope.api.subscriptions.list.forEach(function(s) {
				if (s.subscriber == $scope.roles.sub.subscriber) {
					s.roles = newRoles;
				}
			});
			$scope.rolesDlg.dismiss();
			Utils.showSuccess();
		}, Utils.showError);
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