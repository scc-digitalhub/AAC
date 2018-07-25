angular.module('aac.services', [])

/**
 * REST API service
 */
.service('Data', function($q, $http) {
	var dataService = {};
		
	/**
	 * Read a specific page of the  user apps matching the specified query 
	 */
	dataService.getApps = function(offset, limit, query) {
		var deferred = $q.defer();
		var conf = {};
		conf.params = {
			offset: offset,
			limit: limit,
			query: query
		}
		$http.get('rest/apps', conf).then(function(data){
			deferred.resolve(data.data);
		}, function(err) {
			deferred.reject(err);
		});
		return deferred.promise;
	}
	/**
	 * Read the app with the specified ID
	 */
	dataService.getApp = function(appId) {
		var deferred = $q.defer();
		var conf = {};
		$http.get('rest/apps/'+appId).then(function(data){
			deferred.resolve(data.data);
		}, function(err) {
			deferred.reject(err);
		});
		return deferred.promise;
	}
	/**
	 * Read a specific page of the  user APIs matching the specified query 
	 */
	dataService.getAPIs = function(offset, limit, query) {
		var deferred = $q.defer();
		var conf = {};
		conf.params = {
			offset: offset,
			limit: limit,
			query: query
		}
		$http.get('mgmt/apis', conf).then(function(data){
			deferred.resolve(data.data);
		}, function(err) {
			deferred.reject(err);
		});
		return deferred.promise;
	}
	/**
	 * Read a specific page of the users matching the specified context 
	 */
	dataService.getContextUsers = function(offset, limit, context) {
		var deferred = $q.defer();
		var conf = {};
		conf.params = {
			offset: offset,
			limit: limit,
			context: context
		}
		$http.get('mgmt/users', conf).then(function(data){
			deferred.resolve(data.data);
		}, function(err) {
			deferred.reject(err);
		});
		return deferred.promise;
	}
	/**
	 * Read a specific page of the owners matching the specified context 
	 */
	dataService.getContextOwners = function(offset, limit, context) {
		var deferred = $q.defer();
		var conf = {};
		conf.params = {
			offset: offset,
			limit: limit,
			context: context
		}
		$http.get('mgmt/spaceowners', conf).then(function(data){
			deferred.resolve(data.data);
		}, function(err) {
			deferred.reject(err);
		});
		return deferred.promise;
	}
	dataService.getMyContexts = function() {
		var deferred = $q.defer();
		$http.get('mgmt/spaceowners/me').then(function(data){
			deferred.resolve(data.data);
		}, function(err) {
			deferred.reject(err);
		});
		return deferred.promise;
	}
	
	/**
	 * Read the API with the specified ID
	 */
	dataService.getAPI = function(apiId) {
		var deferred = $q.defer();
		var conf = {};
		$http.get('mgmt/apis/'+apiId).then(function(data){
			data.data.roles = dataService.getRolesOfAPI(data.data);
			deferred.resolve(data.data);
		}, function(err) {
			deferred.reject(err);
		});
		return deferred.promise;
	}
	/**
	 * Read the API subscriptions
	 */
	dataService.getAPISubscriptions = function(apiId, offset, limit) {
		var deferred = $q.defer();
		var conf = {};
		conf.params = {
			offset: offset,
			limit: limit
		}
		
		$http.get('mgmt/apis/'+apiId+'/subscriptions',conf).then(function(data){
			deferred.resolve(data.data);
		}, function(err) {
			deferred.reject(err);
		});
		return deferred.promise;
	}
	/**
	 * Read roles required by the API scopes
	 */
	dataService.getRolesOfAPI = function(api) {
		return api.applicationRoles;
//		var res = [];
//		var apiDef = JSON.parse(api.apiDefinition);
//		if (   apiDef 
//		    && apiDef['x-wso2-security'] 
//		    && apiDef['x-wso2-security']['apim'] 
//		    && apiDef['x-wso2-security']['apim']['x-wso2-scopes']) 
//		{
//			apiDef['x-wso2-security']['apim']['x-wso2-scopes'].forEach(function(s) {
//				if (s.roles) {
//					s.roles.split(',').forEach(function(r){
//						res.push(r.trim());
//					});
//				}
//			});
//		}
//		return res;
	}
	
	var updateUserRoles = function(user, map, old, url) {
		var deferred = $q.defer();
		var toAdd = [];
		var toRemove = [];
		if (!old) old = [];
		old.forEach(function(r) {
			if (map[r] == false) {
				toRemove.push(r);
			}
			map[r] = null;
		});
		for (var r in map) {
			if (map[r] == true) {
				toAdd.push(r);
			}
		}
		
		var body = {
			user: user,
			addRoles: toAdd,
			removeRoles: toRemove
		};
		$http.put(url,body).then(function(data){
			deferred.resolve(data.data);
		}, function(err) {
			deferred.reject(err);
		});
//		console.log(body);
//		deferred.resolve(old);
		return deferred.promise;		
	}
	
	/**
	 * Update roles of the specified user: roles to add and to remove.
	 * API Subscribers are considered (registered users with email)
	 */
	dataService.updateUserRoles = function(user, map, old) {
		return updateUserRoles(user, map, old, 'mgmt/apis/userroles');
	}
	
	/**
	 * Update roles of the specified user: roles to add and to remove.
	 * User roles are considered in the context
	 */
	dataService.updateUserRolesInContext = function(user, map, old, context) {
		return updateUserRoles(user, map, old, 'mgmt/userroles?context='+context);
	}

	/**
	 * Update child spaces of the specified user: space ownership roles to add and to remove.
	 */
	dataService.updateOwnersInContext = function(user, map, old, context) {
		return updateUserRoles(user, map, old, 'mgmt/spaceowners?context='+context);
	}
	/**
	 * Read Service Providers corresponding to the specified app (PRODUCTION and SANDBOX)
	 */
	dataService.getSPs = function(app) {
		var deferred = $q.defer();
		var conf = {};
		$http.get('rest/apps/'+app+'/serviceProviders').then(function(data){
			deferred.resolve(data.data);
		}, function(err) {
			deferred.reject(err);
		});
		return deferred.promise;
	}
	/**
	 * Read all available Identity Providers
	 */
	dataService.getIdPs = function() {
		var deferred = $q.defer();
		if (dataService.idps == null){ 
			$http.get('rest/identityProviders').then(function(data){
				data.idps = data.data;
				deferred.resolve(data.data);
			}, function(err) {
				deferred.reject(err);
			});
		} else {
			deferred.resolve(data.idps);
		}
		return deferred.promise;
	}
	/**
	 * Update association of the IdPs (federated and local) for the specific app
	 */
	dataService.updateSPIdPs = function(applicationName, idps, local) {
		var deferred = $q.defer();
		var conf = {};
		conf.params = {local: local, idps: idps.join(',')};
		$http.post('rest/serviceProviders/'+applicationName+'/idps',{}, conf).then(function(data){
			data.idps = data.data;
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
		$rootScope.errorMsg = msg.data.errorMessage;
		$rootScope.showError = true;
		$timeout(function(){
			$rootScope.showError = false;
			$rootScope.errorMsg = null;
		}, 5000);
	}
	
	return utils;
})

/**
 * REST API service
 */
.service('APIProviders', function($q, $http) {
	var providersService = {};
		
	/**
	 * Read a specific page of the  providers list
	 */
	providersService.getProviders = function(offset, limit) {
		var deferred = $q.defer();
		var conf = {};
		conf.params = {
			offset: offset,
			limit: limit
		}
		$http.get('admin/apiproviders', conf).then(function(data){
			deferred.resolve(data.data);
		}, function(err) {
			deferred.reject(err);
		});
		return deferred.promise;
	}
	/**
	 * Create a provider instance
	 */
	providersService.createProvider = function(provider) {
		var deferred = $q.defer();
		$http.post('admin/apiproviders', provider).then(function(data){
			if (data.status >= 300) {
				deferred.reject(data);
			} else {
				deferred.resolve(data.data);
			}
		}, function(err) {
			deferred.reject(err);
		});
		return deferred.promise;
	}
	
	
	return providersService;
})	
;
