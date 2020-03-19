angular.module('aac.controllers.customservices', [])

/**
 * Service management controller.
 * @param $scope
 * @param $resource
 * @param $http
 * @param $timeout
 */
.controller('ServicesController', function ($scope, $resource, $http, $timeout, $location) {
	// error message
	$scope.error = '';
	// info message
	$scope.info = '';
	
	// resource reference for the app API
	var Services = $resource('dev/services/my/:serviceId', {}, {
		query : { method : 'GET' },
		save : {method : 'POST'},
		remove : {method : 'DELETE'}
	});

	/**
	 * reload service view from server
	 */
	$scope.reload = function() {
		$scope.editService = null;
		Services.query({},function(data) {
			$scope.services = data.data;
		});
	};
	$scope.reload();
	

	/**
	 * switch to different service
	 */
	$scope.switchService = function(service) {
		$scope.error = '';
		$scope.info = '';
		$location.path('/services/'+service.serviceId);
	};

	/** 
	 * initiate creation of new service
	 */
	$scope.newService = function() {
		$scope.error = '';
		$scope.info = '';
		$location.path('/services/new');
	}; 
})


/**
 * Service management controller.
 * @param $scope
 * @param $resource
 * @param $http
 * @param $timeout
 */
.controller('ServiceController', function ($scope, $resource, $routeParams, $http, $timeout, $location, $window, Utils) {
	// current service
	$scope.currService = null;
	// error message
	$scope.error = '';
	// info message
	$scope.info = '';
	// list of services
	$scope.services = null;
	// edited data
	$scope.editService = null;
	// edited parameter
	$scope.param = null;
	// edited mapping
	$scope.mapping = null;
	// service contexts
	$scope.contexts = null;

	$scope.claimEnabled = {checked: false, scopes: []};

	// service reference for the services API
	var Services = $resource('dev/services/my/:serviceId', {}, {
		query : { method : 'GET' },
		save : {method : 'POST'},
		remove : {method : 'DELETE'}
	});
	// service context for the context API
	var Contexts = $resource('dev/servicecontexts/my', {}, {
		query : { method : 'GET' }
	});

	
	// service reference for the scope API
	var ServiceScopes = $resource('dev/services/my/:serviceId/scope/:scope', {}, {
		add : {method : 'PUT'},
		remove : {method : 'DELETE'}
	});
	// service reference for the claim API
	var ServiceClaims = $resource('dev/services/my/:serviceId/claim/:claim', {}, {
		add : {method : 'PUT'},
		remove : {method : 'DELETE'}
	});
	
	// resource reference for the claim validation API
	var ServiceClaimValidation = $resource('dev/services/my/:serviceId/claimmapping/validate', {}, {
		validate : { method : 'POST' },		
	});

	/**
	 * reload service view from server
	 */
	$scope.reload = function() {
		$scope.editService = null;
		if (!$routeParams.serviceId) {
			$scope.editService = {};
			$('#serviceModal').modal({keyboard:false});
		} else {
			Services.query({serviceId: $routeParams.serviceId}).$promise.then(function(data) {
				$scope.currService = data.data;
				$scope.claimEnabled.checked = !!$scope.currService.claimMapping;
			});
		}
		Contexts.query({}, function(data) {
			var options = data.data.filter(function(e) { return !!e}).map(function(e) {
				return {text: e, value: e};
			});
			$('.bootstrap-select-wrapper.context-select-wrapper').setOptionsToSelect(options);
		});
	};
	$scope.reload();
	

	/**
	 * initiate editing  of the current service
	 */
	$scope.startEdit = function() {
		$scope.editService = angular.copy($scope.currService);
		$('#serviceModal').modal({keyboard:false});

	};
	
	/**
	 * close edit form
	 */
	$scope.closeEdit = function() {
		$scope.editService = null;
		if (!$routeParams.serviceId) {
			$window.history.back();
		}
	}
	
	/**
	 * Toggle claim mapping text
	 */
	$scope.toggleClaimMapping = function() {
		if (!!$scope.currService.claimMapping) {
			$scope.currService.claimMapping = null;
		} else {
			$scope.currService.claimMapping = 
				'/**\n * DEFINE YOUR OWN CLAIM MAPPING HERE\n' +
				'**/\n'+
				'function claimMapping(claims) {\n   return {};\n}';
		}
	}
	$scope.aceOption = {
	    mode: 'javascript',
	    theme: 'monokai',
	    maxLines: 30,
        minLines: 6
	  };
	/**
	 * Validate claims
	 */
	$scope.validateClaims = function() {
		$scope.validationResult = '';
		$scope.validationError = '';
		var newClient = new ServiceClaimValidation($scope.currService);
		newClient.$validate({serviceId:$scope.currService.serviceId, scopes: $scope.claimEnabled.scopes.map(function(s) { return s.text})}, function(response) {
			if (response.responseCode == 'ERROR') {
				$scope.validationError = response.errorMessage; 			
				
			} else {
				$scope.validationResult = response.data;
			}
		}, function(e) {
			$scope.validationResult = '';
			$scope.validationError = e.data.errorMessage; 			
		});
	}
	$scope.saveClaimMapping = function() {
		var copy = Object.assign({}, $scope.currService);
		copy.claimMapping = $scope.claimEnabled.checked ? $scope.currService.claimMapping : null;
		Services.save({},copy).$promise.then(function(response) {
			$('#serviceModal').modal('hide');
			$location.path('/services/'+response.data.serviceId);
			$scope.editService = null;
			Utils.showSuccess();
		}).catch(function(error) {
			Utils.showError('Failed to save service descriptor: '+error.data.errorMessage);				
		});
		
	}
	
	/**
	 * save service data (without params and mappings)
	 */
	$scope.saveService = function() {
		Services.save({},$scope.editService).$promise.then(function(response) {
			$('#serviceModal').modal('hide');
			$location.path('/services/'+response.data.serviceId);
			$scope.editService = null;
		}).catch(function(error) {
			Utils.showError('Failed to save service descriptor: '+error.data.errorMessage);				
		});
	};

	/**
	 * delete service
	 */
	$scope.removeService = function() {
		if (confirm('Are you sure you want to delete?')) {
			Services.remove({serviceId:$scope.currService.serviceId}).$promise.then(function(response) {
				$scope.error = '';
				$scope.info = 'Service deleted!';
				$scope.currService = null;
				$location.path('/services');
			}).catch(function(error) {
				Utils.showError('Failed to delete service descriptor: '+error.data.errorMessage);				
			});
		}
	};

	$scope.exportService = function() {
		$window.open('dev/services/my/'+$scope.currService.serviceId+'/yaml');
	};
	
	/**
	 * edit/create scope declaration
	 */
	$scope.editScope = function(scope) {
		if (scope) {
			$scope.updating = true;
			$scope.scope = angular.copy(scope);
		} else {
			$scope.updating = false;
			$scope.scope = {};
		}
		$('#scopeModal').modal({keyboard:false});
	};

	$scope.loadClaims = function(q) {
		return $scope.currService.claims.filter(function(c) {
			return c.claim.toLowerCase().indexOf(q.toLowerCase()) >= 0; 
		}).map(function(c) {
			return c.claim;
		});
	}
	
	/**
	 * Save scope
	 */
	$scope.saveScope = function() {
		$scope.scope.claims = ($scope.scope.claims || []).map(function(s) {
			return typeof s == 'string' ? s : s.text;
		}); 
		ServiceScopes.add({serviceId:$scope.currService.serviceId},$scope.scope).$promise.then(function(response) {
			$scope.reload(response.data);
			$scope.error = '';
			$scope.info = 'Service updated!';
			$('#scopeModal').modal('hide');
		}).catch(function(error) {
			Utils.showError('Failed to add service parameter declaration: '+error.data.errorMessage);
		});
	};
	/**
	 * delete parameter
	 */
	$scope.removeScope = function(scope) {
		if (confirm('Are you sure you want to delete?')) {
			ServiceScopes.remove({serviceId:$scope.currService.serviceId,scope:scope.scope},{}).$promise.then(function(response) {
				$scope.error = '';
				$scope.info = 'Service scope deleted!';
				$scope.currService = null;
				$scope.reload();
			}).catch(function(error) {
				Utils.showError('Failed to delete service scope declaration: '+error.data.errorMessage);				
			});
		}
	};
	/**
	 * edit/create claim declaration
	 */
	$scope.editClaim = function(claim) {
		if (claim) {
			$scope.updating = true;
			$scope.claim = claim;
		} else {
			$scope.updating = false;
			$scope.claim = {};
		}
		$('#claimModal').modal({keyboard:false});
	};
	/**
	 * Add new claim
	 */
	$scope.saveClaim = function() {
		ServiceClaims.add({serviceId:$scope.currService.serviceId},$scope.claim).$promise.then(function(response) {
			$scope.reload(response.data);
			$scope.error = '';
			$scope.info = 'Service updated!';
			$('#claimModal').modal('hide');
		}).catch(function(error) {
			Utils.showError('Failed to add service claim: '+error.data.errorMessage);				
		});
	};
	/**
	 * delete claim
	 */
	$scope.removeClaim = function(claim) {
		if (confirm('Are you sure you want to delete?')) {
			ServiceClaims.remove({serviceId:$scope.currService.serviceId,claim:claim.claim},{}).$promise.then(function(response) {
				$scope.error = '';
				$scope.info = 'Service mapping deleted!';
				$scope.currService = null;
				$scope.reload();
			}).catch(function(error) {
				Utils.showError('Failed to delete service claim declaration: '+error.data.errorMessage);				
			});
		}
	};
	$scope.toAuthority = function(val) {
		if ('ROLE_USER' == val) return 'U';
		if ('ROLE_CLIENT'==val) return 'C';
		if ('ROLE_CLIENT_TRUSTED'==val) return 'C';
		return '*';
	};
});