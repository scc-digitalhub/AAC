angular.module('aac.services', [])

/**
 * REST API service
 */
.service('Data', function($q, $http) {
	var dataService = {};
		
	/**
	 * Read the user profile
	 */
	dataService.getProfile = function() {
		var deferred = $q.defer();
		$http.get('account/profile').then(function(data){
			deferred.resolve(data.data);
		}, function(err) {
			deferred.reject(err);
		});
		return deferred.promise;
	}

	/**
	 * Read the user accounts
	 */
	dataService.getAccounts = function() {
		var deferred = $q.defer();
		$http.get('account/accounts').then(function(data){
			deferred.resolve(data.data);
		}, function(err) {
			deferred.reject(err);
		});
		return deferred.promise;
	}
	/**
	 * Read account providers
	 */
	dataService.getProviders = function() {
		var deferred = $q.defer();
		$http.get('account/providers').then(function(data){
			deferred.resolve(data.data);
		}, function(err) {
			deferred.reject(err);
		});
		return deferred.promise;
	}
	
	/**
	 * Delete account
	 */
	dataService.deleteAccount = function() {
		var deferred = $q.defer();
		$http.delete('account/profile').then(function(data){
			deferred.resolve(data.data);
		}, function(err) {
			deferred.reject(err);
		});
		return deferred.promise;
	}
	
	return dataService;
})
/**
 * Utility functions
 */
.service('Utils', function($timeout, $rootScope) {
	var utils = {};
	
	/**
	 * Show success bar
	 */
	utils.showSuccess = function() {
		$rootScope.showSuccess = true;
		$timeout(function(){
			$rootScope.showSuccess = false;
		}, 3000);
	}
	/**
	 * Show error bar
	 */
	utils.showError = function(msg) {
		$rootScope.errorMsg = (typeof msg === 'string') ? msg : msg.data.errorMessage;
		$rootScope.showError = true;
		$timeout(function(){
			$rootScope.showError = false;
			$rootScope.errorMsg = null;
		}, 4000);
	}
	
	return utils;
})

;
