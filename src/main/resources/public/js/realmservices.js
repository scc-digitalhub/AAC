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
  rsService.validateClaims = function(realm, serviceId, kind, mapping, scopes) {
    return $http.post('console/dev/realms/' + realm + '/services/' + serviceId +'/claims/validate', {kind: kind, scopes: scopes, mapping: mapping}).then(function(data) {
      return data.data;
    });
  }

  rsService.checkServiceNamespace = function(serviceNs) {
    return $http.get('console/dev/services/nsexists?ns=' + encodeURIComponent(serviceNs)).then(function(data) {
      return data.data;
    });
  }
  
  rsService.getApprovals = function(realm, serviceId) {
    return $http.get('console/dev/realms/' + realm + '/services/'+serviceId +'/approvals').then(function(data) {
      return data.data;
    });
  }
  
  rsService.addApproval = function(realm, serviceId, approval) {
    return $http.post('console/dev/realms/' + realm + '/services/' + serviceId +'/scopes/' + approval.scope + '/approvals?clientId=' + approval.clientId).then(function(data) {
      return data.data;
    });
  }
  rsService.deleteApproval = function(realm, serviceId, approval) {
    return $http.delete('console/dev/realms/' + realm + '/services/' + serviceId +'/scopes/' + approval.scope + '/approvals?clientId=' + approval.clientId).then(function(data) {
      return data.data;
    });
  }
  
  return rsService;
})

/**
 * Service list management controller.
 * @param $scope
 * @param $resource
 * @param $http
 * @param $timeout
 */
.controller('RealmServicesController', function ($scope, $state, $stateParams, RealmServicesData, Utils) {	
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
	
	$scope.serviceApprovals = function(service) {
	   $state.go('realm.serviceapprovals', { realmId: $stateParams.realmId, serviceId: service.serviceId });
	}

	/**
	 * switch to different service
	 */
	$scope.switchService = function(service) {
		$state.go('realm.service', { realmId: $stateParams.realmId, serviceId: service.serviceId });
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
        'function claimMapping(context) {\n let client = context.client; \n let user = context.user; \n let scopes = context.scopes; \n  return {};\n}';
    }
  }
  /**
   * Validate claims
   */
  $scope.validateClaims = function(m) {
    $scope.validationResult[m] = '';
    $scope.validationError[m] = '';
    
    RealmServicesData.validateClaims($scope.service.realm, $scope.service.serviceId, m, $scope.service.claimMapping[m], $scope.claimEnabled[m].scopes.map(function(s) { return s.text}))
    .then(function(data) {
        $scope.validationResult[m] = data;   
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
 * Service list management controller.
 * @param $scope
 * @param $resource
 * @param $http
 * @param $timeout
 */
.controller('RealmServiceApprovalsController', function ($scope, $state, $stateParams, RealmServicesData, Utils) {
  var slug = $stateParams.realmId;
  var serviceId = $stateParams.serviceId;
  
  RealmServicesData.getService(slug, serviceId)
  .then(function(service) {
    $scope.service = service;
    $scope.reloadApprovals();   
  })
  .catch(function (err) {
    Utils.showError('Failed to load realm service: ' + err.data.message);
  });
    
  $scope.reloadApprovals = function() {
    $scope.editService = null;
    RealmServicesData.getApprovals(slug, serviceId)
    .then(function(approvals) {
      $scope.approvals = approvals;
    })
    .catch(function (err) {
      Utils.showError('Failed to load service approvals: ' + err.data.message);
    });
    
  };
  
  $scope.newApproval = function() {
    $scope.approval = {};
    $('#approvalModal').modal({keyboard:false});
    Utils.refreshFormBS();
  }
  
  $scope.saveApproval = function() {
    $('#approvalModal').modal('hide');   
    
    RealmServicesData.addApproval($scope.service.realm, $scope.service.serviceId, $scope.approval)
    .then(function() {
      $scope.reloadApprovals();
    })
    .catch(function (err) {
      Utils.showError('Failed to save service approval: ' + err.data.message);
    });
  }
  
    /**
   * delete claim
   */
  $scope.removeApproval = function(approval) {
    $scope.approval = approval;
    $('#deleteConfirm').modal({ keyboard: false });  
  };
  $scope.doDelete = function() {
      $('#deleteConfirm').modal('hide');   
      RealmServicesData.deleteApproval($scope.service.realm, $scope.service.serviceId, $scope.approval)
      .then(function() {
        Utils.showSuccess();
        $scope.reloadApprovals();
      })
      .catch(function (err) {
        Utils.showError('Failed to load delete approval: ' + err.data.message);
      });
    }
  
})
;