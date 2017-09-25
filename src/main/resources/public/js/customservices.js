angular.module('aac.controllers.customservices', [])

/**
 * Service management controller.
 * @param $scope
 * @param $resource
 * @param $http
 * @param $timeout
 */
.controller('ServiceController', function ($scope, $resource, $http, $timeout, $location) {
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

	// resource reference for the app API
	var Services = $resource('dev/services/my/:serviceId', {}, {
		query : { method : 'GET' },
		save : {method : 'POST'},
		remove : {method : 'DELETE'}
	});
	// resource reference for the app API
	var ServiceProps = $resource('dev/services/my/:serviceId/:prop/:id', {}, {
		add : {method : 'PUT'},
		remove : {method : 'DELETE'}
	});
	
	/**
	 * reload service view from server
	 */
	$scope.reload = function(service) {
		$scope.editService = null;
		Services.query({},function(data) {
			$scope.services = data.data;
			if ($scope.services && $scope.services.length > 0) {
				if (service) {
					$scope.currService = angular.copy(service);
				} else {
					$scope.currService = angular.copy($scope.services[0]);
				}
			}
		});
	};
	$scope.reload();
	
	
	/**
	 * return 'active' if the specified service is selected
	 */
	$scope.activeService = function(service) {
		var cls = service.id == $scope.currService.id ? 'active' : '';
		return cls;
	};

	/**
	 * switch to different service
	 */
	$scope.switchService = function(service) {
		$scope.error = '';
		$scope.info = '';
		$scope.editService = null;
		if (service != null && service.id != null) {
			for (var i = 0; i < $scope.services.length; i++) {
				if ($scope.services[i].id == service.id) {
					$scope.currService = angular.copy($scope.services[i]);
					return;
				}
			}
		} else if ($scope.services != null && $scope.services.length > 0) {
			$scope.currService = angular.copy($scope.services[0]);
		}
	};

	/** 
	 * initiate creation of new service
	 */
	$scope.newService = function() {
		$scope.editService = {};
	}; 
	/**
	 * initiate editing  of the current service
	 */
	$scope.startEdit = function() {
		$scope.editService = angular.copy($scope.currService);
	};
	
	/**
	 * close edit form
	 */
	$scope.closeEdit = function() {
		$scope.editService = null;
	}
	/**
	 * save service data (without params and mappings)
	 */
	$scope.saveService = function() {
		Services.save({},$scope.editService, function(response) {
			if (response.responseCode == 'OK') {
				$scope.reload(response.data);
				$scope.error = '';
				$scope.info = 'Service created!';
			} else {
				$scope.error = 'Failed to save service descriptor: '+response.errorMessage;
			}	
			$scope.editService = null;
		});
	};
	
	/**
	 * delete service
	 */
	$scope.removeService = function() {
		if (confirm('Are you sure you want to delete?')) {
			Services.remove({serviceId:$scope.currService.id}, function(response) {
				if (response.responseCode == 'OK') {
					$scope.error = '';
					$scope.info = 'Service deleted!';
					$scope.currService = null;
					$scope.reload();
				} else {
					$scope.error = 'Failed to delete service descriptor: '+response.errorMessage;
				}	
			});
		}
	};

	/**
	 * edit/create parameter declaration
	 */
	$scope.editParameter = function(param) {
		if (param) {
			$scope.updating = true;
			$scope.param = param;
		} else {
			$scope.updating = false;
			$scope.param = {};
		}
		$('#paramModal').modal({keyboard:false});
	};
	
	/**
	 * Add new parameter
	 */
	$scope.addParameter = function() {
		ServiceProps.add({serviceId:$scope.currService.id,prop:'parameter'},$scope.param, function(response) {
			if (response.responseCode == 'OK') {
				$scope.reload(response.data);
				$scope.error = '';
				$scope.info = 'Service updated!';
			} else {
				$scope.error = 'Failed to add service parameter declaration: '+response.errorMessage;
			}	
			$('#paramModal').modal('hide');
		});
	};
	/**
	 * delete parameter
	 */
	$scope.removeParameter = function(param) {
		if (confirm('Are you sure you want to delete?')) {
			ServiceProps.remove({serviceId:$scope.currService.id,prop:'parameter',id:param.id},{}, function(response) {
				if (response.responseCode == 'OK') {
					$scope.error = '';
					$scope.info = 'Service parameter deleted!';
					$scope.currService = null;
					$scope.reload();
				} else {
					$scope.error = 'Failed to delete service parameter declaration: '+response.errorMessage;
				}	
			});
		}
	};
	/**
	 * edit/create mapping declaration
	 */
	$scope.editMapping = function(mapping) {
		if (mapping) {
			$scope.updating = true;
			$scope.mapping = mapping;
		} else {
			$scope.updating = false;
			$scope.mapping = {};
		}
		$('#mappingModal').modal({keyboard:false});
	};
	/**
	 * Add new mapping
	 */
	$scope.addMapping = function() {
		// TODO dialog
		ServiceProps.add({serviceId:$scope.currService.id,prop:'mapping'},$scope.mapping, function(response) {
			if (response.responseCode == 'OK') {
				$scope.reload(response.data);
				$scope.error = '';
				$scope.info = 'Service updated!';
			} else {
				$scope.error = 'Failed to add service mapping: '+response.errorMessage;
			}	
		});
		$('#mappingModal').modal('hide');
	};
	/**
	 * delete mapping
	 */
	$scope.removeMapping = function(mapping) {
		if (confirm('Are you sure you want to delete?')) {
			ServiceProps.remove({serviceId:$scope.currService.id,prop:'mapping',id:mapping.id},{}, function(response) {
				if (response.responseCode == 'OK') {
					$scope.error = '';
					$scope.info = 'Service mapping deleted!';
					$scope.currService = null;
					$scope.reload();
				} else {
					$scope.error = 'Failed to delete service mapping declaration: '+response.errorMessage;
				}	
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