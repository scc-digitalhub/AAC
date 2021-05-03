angular.module('aac.controllers.realmservices', [])

.service('RealmServicesData', function($q, $http, $rootScope) {
  var rsService = {};
  
  rsService.getServices = function(realm) {
    return $http.get('console/dev/realms/' + realm + '/services').then(function(data) {
      return data.data;
    });
  }
  rsService.getService = function(realm, serviceId) {
    return $http.get('console/dev/realms/' + realm + '/services/'+serviceId).then(function(data) {
      return data.data;
    });
  }
  
  rsService.addService = function(realm, service) {
    return $http.post('console/dev/realms/' + realm + '/services', service).then(function(data) {
      return data.data;
    });
  }
  rsService.updateService = function(realm, service) {
    return $http.put('console/dev/realms/' + realm + '/services/' + service.serviceId, service).then(function(data) {
      return data.data;
    });
  }
  rsService.deleteService = function(realm, serviceId) {
    return $http.delete('console/dev/realms/' + realm + '/services/' + serviceId).then(function(data) {
      return data.data;
    });
  }
  rsService.addClaim = function(realm, serviceId, claim) {
    return $http.post('console/dev/realms/' + realm + '/services/' + serviceId +'/claims', claim).then(function(data) {
      return data.data;
    });
  }
  rsService.updateClaim = function(realm, serviceId, claim) {
    return $http.put('console/dev/realms/' + realm + '/services/' + serviceId + '/claims/' + claim.key, claim).then(function(data) {
      return data.data;
    });
  }
  rsService.deleteClaim = function(realm, serviceId, key) {
    return $http.delete('console/dev/realms/' + realm + '/services/' + serviceId + '/claims/' + key).then(function(data) {
      return data.data;
    });
  }
  
  rsService.addScope = function(realm, serviceId, scope) {
    return $http.post('console/dev/realms/' + realm + '/services/' + serviceId +'/scopes', scope).then(function(data) {
      return data.data;
    });
  }
  rsService.updateScope = function(realm, serviceId, scope) {
    return $http.put('console/dev/realms/' + realm + '/services/' + serviceId + '/scopes/' + scope.scope, scope).then(function(data) {
      return data.data;
    });
  }
  rsService.deleteScope = function(realm, serviceId, scope) {
    return $http.delete('console/dev/realms/' + realm + '/services/' + serviceId + '/scopes/' + scope).then(function(data) {
      return data.data;
    });
  }
  rsService.validateClaims = function(realm, serviceId, mapping, scopes) {
    return $http.post('console/dev/realms/' + realm + '/services/' + serviceId +'/claims/validate', {scopes: scopes, mapping: mapping}).then(function(data) {
      return data.data;
    });
  }

  rsService.checkServiceNamespace = function(serviceNs) {
    return $http.get('console/dev/services/nsexists?ns=' + encodeURIComponent(serviceNs)).then(function(data) {
      return data.data;
    });
  }
  return rsService;
})

/**
 * Service management controller.
 * @param $scope
 * @param $resource
 * @param $http
 * @param $timeout
 */
.controller('RealmServicesController', function ($scope, $state, $stateParams, RealmServicesData, Utils, $timeout, $location) {	
  var slug = $stateParams.realmId;
	$scope.reload = function() {
		$scope.editService = null;
		RealmServicesData.getServices(slug)
		.then(function(services) {
		  $scope.services = services;
		})
	  .catch(function (err) {
      Utils.showError('Failed to load realm: ' + err.data.message);
    });
		
	};
	
	$scope.reload();
	

	/**
	 * switch to different service
	 */
	$scope.switchService = function(service) {
		$state.go('realm.service', { realmId: $stateParams.realmId, serviceId: service.serviceId });
		//$location.path('/realm/services/'+service.serviceId);
	};

	/** 
	 * initiate creation of new service
	 */
	$scope.newService = function() {
	  $scope.editService = {};  
    $('#serviceModal').modal({backdrop: 'static', focus: true})
    Utils.refreshFormBS();
	}; 
	
	$scope.saveService = function() {
	   RealmServicesData.addService(slug, $scope.editService)
    .then(function() {
      $('#serviceModal').modal('hide');
      $scope.reload();
    })
    .catch(function (err) {
      Utils.showError('Failed to save service: ' + err.data.message);
    });
	}
	
	var doCheck = function() {
	  var oldCheck = $scope.nsChecking;
    $scope.nsError = true;
	  RealmServicesData.checkServiceNamespace($scope.editService.namespace).then(function(data) {
     if (!data) {
         $scope.nsError = false;
     }
     if ($scope.nsChecking = oldCheck) $scope.nsChecking = null;
   });
	}
	
	$scope.changeNS = function() {
     $scope.nsError = false;
     if ($scope.nsChecking) {
        clearTimeout($scope.nsChecking);
     }
     $scope.nsChecking = setTimeout(doCheck, 300);
	}
})


/**
 * Service management controller.
 * @param $scope
 * @param $resource
 * @param $http
 * @param $timeout
 */
.controller('RealmServiceController', function ($scope, $state, $stateParams, RealmServicesData, Utils) {
  var slug = $stateParams.realmId;
  var serviceId = $stateParams.serviceId;
  
  $scope.aceOption = {
    mode: 'javascript',
    theme: 'monokai',
    maxLines: 30,
      minLines: 6
  };
  
  $scope.reload = function() {
    RealmServicesData.getService(slug, serviceId)
    .then(function(service) {
      $scope.service = service;
      $scope.claimEnabled = {
        client: {checked: service.claimMapping && !!service.claimMapping['client'], scopes: []},
        user: {checked: service.claimMapping && !!service.claimMapping['user'], scopes: []}
      };
      $scope.validationResult = {};
      $scope.validationError = {};
    })
    .catch(function (err) {
      Utils.showError('Failed to load realm service: ' + err.data.message);
    });
    
  };
  $scope.reload();
  
  /**
   * delete service
   */
  $scope.removeService = function() {
    $scope.doDelete = function() {
      $('#deleteConfirm').modal('hide');   
      RealmServicesData.deleteService($scope.service.realm, $scope.service.serviceId)
      .then(function() {
        Utils.showSuccess();
        $state.go('realm.services', { realmId: $stateParams.realmId });
      })
      .catch(function (err) {
        Utils.showError('Failed to load realm service: ' + err.data.message);
      });
    }
    $('#deleteConfirm').modal({ keyboard: false });
  };

  /**
   * Export service
   */
  $scope.exportService = function() {
    window.open('console/dev/realms/' + $scope.service.realm + '/services/' + $scope.service.serviceId + '/yaml');
  };
  
  /** 
   * Edit service
   */
  $scope.editService = function() {
    $scope.editService = Object.assign({}, $scope.service);  
    $('#serviceModal').modal({backdrop: 'static', focus: true})
    Utils.refreshFormBS();
  }; 
  /** 
   * Save service
   */ 
  $scope.saveService = function() {
     RealmServicesData.updateService(slug, $scope.editService)
    .then(function(service) {
      $('#serviceModal').modal('hide');
      $scope.service =  service;
    })
    .catch(function (err) {
      Utils.showError('Failed to save service: ' + err.data.message);
    });
  }
  
  /**
   * edit/create scope declaration
   */
  $scope.editScope = function(scope) {
    if (scope) {
      $scope.updating = true;
      $scope.scope = Object.assign({}, scope);
      $scope.approvalFunction = {checked: !!scope.approvalFunction};
    } else {
      $scope.updating = false;
      $scope.scope = {};
      $scope.approvalFunction = {checked: false};
    }
    $('#scopeModal').modal({keyboard:false});
    Utils.refreshFormBS();
  } 
  $scope.toggleApprovalFunction = function() {
    if (!$scope.approvalFunction.checked) {
      $scope.scope.approvalFunction = null;
    } else {
      $scope.scope.approvalFunction = 
        '/**\n * DEFINE YOUR OWN APPROVAL FUNCTION HERE\n' +
        ' * input is a map containing user, client, and scopes\n' +
        '**/\n'+
        'function approver(inputData) {\n   return {};\n}';
    }
  }
  $scope.saveScope = function() {
    $('#scopeModal').modal('hide');    
    if ($scope.updating) {
      RealmServicesData.updateScope($scope.service.realm, $scope.service.serviceId, $scope.scope)
      .then(function() {
        $scope.reload();
      })
      .catch(function (err) {
        Utils.showError('Failed to save scope: ' + err.data.message);
      });
    } else {
      RealmServicesData.addScope($scope.service.realm, $scope.service.serviceId, $scope.scope)
      .then(function() {
        $scope.reload();
      })
      .catch(function (err) {
        Utils.showError('Failed to save scope: ' + err.data.message);
      });
    }
  }
    /**
   * delete scope
   */
  $scope.removeScope = function(scope) {
    $scope.doDelete = function() {
      $('#deleteConfirm').modal('hide');   
      RealmServicesData.deleteScope($scope.service.realm, $scope.service.serviceId, scope.scope)
      .then(function() {
        Utils.showSuccess();
        $scope.reload();
      })
      .catch(function (err) {
        Utils.showError('Failed to load delete scope: ' + err.data.message);
      });
    }
    $('#deleteConfirm').modal({ keyboard: false });  
  };
  
  /**
   * edit/create claim declaration
   */
  $scope.editClaim = function(claim) {
    if (claim) {
      $scope.updating = true;
      $scope.claim = Object.assign({}, claim);
    } else {
      $scope.updating = false;
      $scope.claim = {};
    }
    $('#claimModal').modal({keyboard:false});
    Utils.refreshFormBS();
  };
  /**
   * Add new claim
   */
  $scope.saveClaim = function() {
    $('#claimModal').modal('hide');    
    if ($scope.updating) {
      RealmServicesData.updateClaim($scope.service.realm, $scope.service.serviceId, $scope.claim)
      .then(function() {
        $scope.reload();
      })
      .catch(function (err) {
        Utils.showError('Failed to save claim: ' + err.data.message);
      });
    } else {
      RealmServicesData.addClaim($scope.service.realm, $scope.service.serviceId, $scope.claim)
      .then(function() {
        $scope.reload();
      })
      .catch(function (err) {
        Utils.showError('Failed to save claim: ' + err.data.message);
      });
    }
  };
  /**
   * delete claim
   */
  $scope.removeClaim = function(claim) {
    $scope.doDelete = function() {
      $('#deleteConfirm').modal('hide');   
      RealmServicesData.deleteClaim($scope.service.realm, $scope.service.serviceId, claim.key)
      .then(function() {
        Utils.showSuccess();
        $scope.reload();
      })
      .catch(function (err) {
        Utils.showError('Failed to load delete claim: ' + err.data.message);
      });
    }
    $('#deleteConfirm').modal({ keyboard: false });  
  };

  /**
   * Toggle claim mapping text
   */
  $scope.toggleClaimMapping = function(m) {
    if (!$scope.service.claimMapping) $scope.service.claimMapping = {};
    if (!!$scope.service.claimMapping[m]) {
      $scope.service.claimMapping[m] = null;
    } else {
      $scope.service.claimMapping[m] = 
        '/**\n * DEFINE YOUR OWN CLAIM MAPPING HERE\n' +
        '**/\n'+
        'function claimMapping(claims) {\n   return {};\n}';
    }
  }
  /**
   * Validate claims
   */
  $scope.validateClaims = function(m) {
    $scope.validationResult[m] = '';
    $scope.validationError[m] = '';
    
    RealmServicesData.validateClaims($scope.service.realm, $scope.service.serviceId, $scope.service.claimMapping[m], $scope.claimEnabled[m].scopes.map(function(s) { return s.text}))
    .then(function(data) {
      if (data.errorMessage) {
        $scope.validationError[m] = data.errorMessage;       
      } else {
        $scope.validationResult[m] = data.data;
      }
    })
    .catch(function (e) {
      $scope.validationResult[m] = '';
      $scope.validationError[m] = e.data.message;       
    });
  }
  $scope.saveClaimMapping = function(m) {
    var copy = Object.assign({}, $scope.service);
    if (!copy.claimMapping) copy.claimMapping = {};
    copy.claimMapping[m] = $scope.claimEnabled[m].checked ? $scope.service.claimMapping[m] : null;
    
    RealmServicesData.updateService($scope.service.realm, copy)
    .then(function() {
      $scope.reload();
      Utils.showSuccess();
    })
    .catch(function (err) {
      Utils.showError('Failed to save claim mapping: ' + err.data.message);
    });
  }
  
  $scope.loadClaims = function(q) {
    return $scope.service.claims.filter(function(c) {
      return c.key.toLowerCase().indexOf(q.toLowerCase()) >= 0; 
    }).map(function(c) {
      return c.key;
    });
  }
  
})


/**
 * Service management controller.
 * @param $scope
 * @param $resource
 * @param $http
 * @param $timeout
 */
.controller('ServiceController', function ($scope, $stateParams, $timeout, $location, $window, Utils) {


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
		$scope.scope.roles = ($scope.scope.roles || []).map(function(s) {
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