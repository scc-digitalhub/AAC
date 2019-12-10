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
	/**
	 * Save account
	 */
	dataService.saveAccount = function(profile) {
		var deferred = $q.defer();
		delete profile.password2;
		$http.post('account/profile', profile).then(function(data){
			deferred.resolve(data.data);
		}, function(err) {
			deferred.reject(err);
		});
		return deferred.promise;
	}
	
	/**
	 * Read the user connected apps
	 */
	dataService.getConnections = function() {
		var deferred = $q.defer();
		$http.get('account/connections').then(function(data){
			deferred.resolve(data.data);
		}, function(err) {
			deferred.reject(err);
		});
		return deferred.promise;
	}
	/**
	 * Remove app connection
	 */
	dataService.removeConnection = function(clientId) {
		var deferred = $q.defer();
		$http.delete('account/connections/'+ clientId).then(function(data){
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
		$rootScope.errorMsg = (typeof msg === 'string') ? msg : (msg.data.errorMessage || msg.data.error);
		$rootScope.showError = true;
		$timeout(function(){
			$rootScope.showError = false;
			$rootScope.errorMsg = null;
		}, 4000);
	}

	utils.initUI = function() {
		setTimeout(function() {
			$.getScript('./italia/js/bootstrap-italia.bundle.min.js');
		});
	}
	
	return utils;
})

;
