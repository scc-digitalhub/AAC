angular.module('dev', [ 'ngResource']);

/**
 * Main layout controller
 * @param $scope
 */
function MainController($scope) {
	$scope.currentView = 'apps';
	$scope.activeView = function(view) {
		return view == $scope.currentView ? 'active' : '';
	};
	$scope.signOut = function() {
	    window.document.location = "./logout";
	};
}

/**
 * App management controller.
 * @param $scope
 * @param $resource
 * @param $http
 * @param $timeout
 */
function AppController($scope, $resource, $http, $timeout, $location) {
	// current client
	$scope.app = null;
	// current client ID
	$scope.clientId = 'none';
	// current view (overview/settings/permissions)
	$scope.clientView = 'overview';
	// error message
	$scope.error = '';
	// info message
	$scope.info = '';
	$scope.services = null;
	// permissions of the current client and services
	$scope.permissions = null;
	// permissions subview (available permissions/own resources)
	$scope.permView = 'avail';
	// currently open service container in accordion
	$scope.permService = 0;
	// collapse flag for service container
	$scope.permServiceCollapsed = false;
	// client flow token of the current app
	$scope.clientToken = null;
	// implicit flow token of the current app
	$scope.implicitToken = null;

	$scope.GTLabels = {
			implicit: 'Implicit',
			authorization_code: 'Authorization Code',
			password: 'Password',
			client_credentials: 'Client Credentials',
			refresh_token: 'Refresh token'
	}
	$scope.grantTypes = {
			implicit: false,
			authorization_code: false,
			password: false,
			client_credentials: false,
			refresh_token: false
	}
	
	// resource reference for the app API
	var ClientAppBasic = $resource('dev/apps/:clientId', {}, {
		query : { method : 'GET' },
		update : { method : 'PUT' },
		reset : {method : 'POST'}
	});

	// resource reference for the permissions API
	var ClientAppPermissions = $resource('dev/permissions/:clientId/:serviceId', {}, {
		update : { method : 'PUT' },		
	});

	// resource reference for the resources API
	var ClientAppResourceParam = $resource('dev/resourceparams/:id', {}, {
		create : { method : 'POST' },
		changeVis : {method : 'PUT'}
	});
	
	// resource reference for the services API
	var AppServices = $resource('dev/services/:clientId', {}, {
		query : {method : 'GET'}
	});
	/**
	 * Initialize the app: load list of the developer's apps and reset views
	 */
	var init = function() {
		ClientAppBasic.query(function(response){
			if (response.responseCode == 'OK') {
				$scope.error = '';
				var apps = response.data;
				$scope.apps = apps;
				if (apps.length > 0) {
					$scope.app = angular.copy(apps[0]);
					$scope.initGrantTypes($scope.app);
					$scope.clientId = apps[0].clientId;
					$scope.switchClientView('overview');
				} else {
					$scope.clientId = 'none';
					$scope.app = null;
					$scope.switchClientView('overview');
				}
			} else {
				$scope.error = 'Failed to load apps: '+response.errorMessage;
			}	
		});
	};
	init();

	/**
	 * Generate string of identity providers allowed for the app
	 */
	$scope.identityProviders = function(app) {
		var res = '';
		for (var i in app.identityProviderApproval) {
			if (app.identityProviderApproval[i]) {
				res += i+' ';
			}
		}
		return res;
	};
	/**
	 * Grant types
	 */
	$scope.initGrantTypes = function(app) {
		for (var k in $scope.grantTypes) {
			$scope.grantTypes[k] = false;
		}
		if (app.grantedTypes) {
			app.grantedTypes.forEach(function(gt){
				$scope.grantTypes[gt] = true;
			});
		}
	}
	$scope.showGrantTypes = function(app) {
		var arr = [];
		if (app.grantedTypes) app.grantedTypes.forEach(function(gt) {
			arr.push($scope.GTLabels[gt]);
		});
		return arr.join(', ');
	}
	
	$scope.idpIcon = function(req,app) {
		if (!req) return null;
		if (app == null) return 'glyphicon glyphicon-time';
		if (app) return 'glyphicon glyphicon-ok';
		return 'glyphicon glyphicon-remove';
	};
	
	/**
	 * return 'active' if the specified client is selected
	 */
	$scope.activeClient = function(clientId) {
		var cls = clientId == $scope.clientId ? 'active' : '';
		return cls;
	};
	/**
	 * return 'active' if the specified view is selected
	 */
	$scope.activeView = function(view) {
		return view == $scope.clientView ? 'active' : '';
	};
	/**
	 * return 'active' if the specified permissions subview is selected
	 */
	$scope.activePermView = function(view) {
		return view == $scope.permView ? 'active' : '';
	};
	/**
	 * switch to other client app. Reset views, messages, and tokens
	 */
	$scope.switchClientView = function(view) {
		$scope.clientView = view;
		$scope.error = '';
		$scope.info = '';
		
		$scope.clientToken = null;  
		$scope.implicitToken = null;  
	};

	/**
	 * switch to other permissions subview. reset messages
	 */
	$scope.switchPermView = function(view) {
		$scope.permView = view;
		$scope.error = '';
		$scope.info = '';
	};

	/**
	 * open permissions of a service 
	 */
	$scope.changeServicePermissions = function(item) {
			$scope.service = item;
			loadPermissions(item, function() {
				$('#myModal').modal({keyboard:false});
			});
	};
	
	/**
	 * Whether the specified service container is collapsed
	 */
	$scope.isPermServiceCollapsed = function(idx) {
		return $scope.permService != idx || $scope.permServiceCollapsed;
	};
	/**
	 * switch to app 'overview'
	 */
	$scope.viewOverview = function() {
		$scope.switchClientView('overview');
	};
	/**
	 * switch to app 'settings'
	 */
	$scope.viewSettings = function() {
		$scope.switchClientView('settings');
	};
	/**
	 * switch to app 'permissions'
	 */
	$scope.viewPermissions = function() {
		$scope.permissions = null;
		$scope.switchClientView('permissions');
		loadServices();
	};

	/**
	 * load permissions of the current app.
	 */
	function loadPermissions(service, callback) {
		var newClient = new ClientAppPermissions();
		newClient.$get({clientId:$scope.clientId, serviceId: service.id}, function(response) {
			if (response.responseCode == 'OK') {
				$scope.error = '';
				$scope.permissions = response.data;
				callback();
			} else {
				$scope.error = 'Failed to load app permissions: '+response.errorMessage;
			}	
		});
	}
	/**
	 * load services available
	 */
	function loadServices() {
		var newClient = new AppServices();
		newClient.$query({clientId:$scope.clientId}, function(response) {
			if (response.responseCode == 'OK') {
				$scope.error = '';
				$scope.services = response.data;
			} else {
				$scope.error = 'Failed to load app permissions: '+response.errorMessage;
			}	
		});
	}
	
	/**
	 * switch to different client
	 */
	$scope.switchClient = function(client) {
		for (var i = 0; i < $scope.apps.length; i++) {
			if ($scope.apps[i].clientId == client) {
				$scope.clientId = $scope.apps[i].clientId;
				$scope.app = angular.copy($scope.apps[i]);
				$scope.initGrantTypes($scope.app);	
				$scope.switchClientView('overview');
				return;
			}
		}
	};

	/**
	 * delete client app
	 */
	$scope.removeClient = function() {
		if (confirm('Are you sure you want to delete?')) {
			var newClient = new ClientAppBasic();
			newClient.$remove({clientId:$scope.clientId},function(response){
				if (response.responseCode == 'OK') {
					$scope.error = '';
					init();
				} else {
					$scope.error = 'Failed to remove app: '+response.errorMessage;
				}	
			});
	    }
	};

	/**
	 * create new client app
	 */
	$scope.newClient = function() {
		var n = prompt("Create new client app", "client name");
		if (n != null && n.trim().length > 0) {
			var newClient = new ClientAppBasic({name:n});
			newClient.$save(function(response){
				if (response.responseCode == 'OK') {
					$scope.error = '';
					var app = response.data;
					$scope.apps.push(app);
					$scope.switchClient(app.clientId);
				} else {
					$scope.error = 'Failed to create new app: '+response.errorMessage;
				}
			});
		}
	};

	/**
	 * Save current app settings
	 */
	$scope.saveSettings = function() {
		var newGt = [];
		for (var k in $scope.grantTypes) if ($scope.grantTypes[k]) newGt.push(k);
		$scope.app.grantedTypes = newGt;
		var newClient = new ClientAppBasic($scope.app);
		newClient.$update({clientId:$scope.clientId}, function(response) {
			if (response.responseCode == 'OK') {
				$scope.error = '';
				$scope.info = 'App settings saved!';

				var app = response.data;
				$scope.app = angular.copy(app);
				$scope.initGrantTypes($scope.app);
				for (var i = 0; i < $scope.apps.length; i++) {
					if ($scope.apps[i].clientId == app.clientId) {
						$scope.apps[i] = app;
						return;
					}
				}
			} else {
				$scope.error = 'Failed to save settings: '+response.errorMessage;
			}
		});
	};
	/**
	 * Save current app permissions
	 */
	$scope.savePermissions = function(service) {
		var perm = new ClientAppPermissions($scope.permissions);
		perm.$update({clientId:$scope.clientId, serviceId:service.id}, function(response) {
			$('#myModal').modal('hide');
			if (response.responseCode == 'OK') {
				$scope.error = '';
				$scope.info = 'App permissions saved!';
				$scope.permissions = response.data;
			} else {
				$scope.error = 'Failed to save app permissions: '+response.errorMessage;
			}	
		});
	};
	/**
	 * create new resource parameter value for the specified resource parameter, service, and client app
	 */
	$scope.saveResourceParam = function(parameter,serviceId,clientId) {
		var perm = new ClientAppResourceParam({parameter:parameter,service:{serviceId:serviceId},clientId:clientId});
		var n = prompt("Create new resource", "value");
		if (n == null || n.trim().length==0) return;
		perm.value = n.trim();
		
		perm.$create(function(response) {
			$('#myModal').modal('hide');
			if (response.responseCode == 'OK') {
				$scope.error = '';
				$scope.info = 'Resource added!';
			} else {
				$scope.error = 'Failed to save own resource: '+response.errorMessage;
			}	
		});
	};

	
	/**
	 * delete resource parameter
	 */
	$scope.removeResourceParam = function(r) {
		if (confirm('Are you sure you want to delete this resource and subresources?')) {
			var perm = new ClientAppResourceParam();
			perm.$delete({id:r.id},function(response){
				$('#myModal').modal('hide');
				if (response.responseCode == 'OK') {
					$scope.error = '';
					$scope.info = 'Resource removed!';
				} else {
					$scope.error = 'Failed to remove resource: '+response.errorMessage;
				}	
			});
	    }
	};

	/**
	 * change resource parameter visibility
	 */
	$scope.changeResourceParamVis = function(r) {
		if (confirm('Change visibility of the resource?')) {
			var perm = new ClientAppResourceParam();
			perm.$changeVis({id:r.id,vis:r.visibility},function(response){
				$('#myModal').modal('hide');
				if (response.responseCode == 'OK') {
					$scope.error = '';
					$scope.info = 'Resource visibility updated!';
				} else {
					$scope.error = 'Failed to change resource visibility: '+response.errorMessage;
					$scope.info = '';
				}	
			});
	    }
	};

	/**
	 * return icon depending on the permission status
	 */
	$scope.permissionIcon = function(val) {
		switch (val){
		case 1: return 'glyphicon glyphicon-ok';
		case 2: return 'glyphicon glyphicon-remove';
		case 3: return 'glyphicon glyphicon-time';
		default: return null;
		}
		
	};
	
	$scope.toAuthority = function(val) {
		if ('ROLE_USER' == val) return 'U';
		if ('ROLE_CLIENT'==val) return 'C';
		if ('ROLE_CLIENT_TRUSTED'==val) return 'C';
		return '*';
	};
	
	/**
	 * return icon for the app access type
	 */
	$scope.statusIcon = function(val) {
		if (val) return 'glyphicon glyphicon-ok';
		else return 'glyphicon glyphicon-remove';
	};
	/**
	 * reset value for client id or secret
	 */
	$scope.reset = function(client,param) {
		if (confirm('Are you sure you want to reset '+param+'?')) {
			var newClient = new ClientAppBasic($scope.app);
			newClient.$reset({clientId:$scope.clientId,reset:param}, function(response) {
				if (response.responseCode == 'OK') {
					$scope.error = '';
					$scope.info = param + ' successfully reset!';

					var app = response.data;
					for (var i = 0; i < $scope.apps.length; i++) {
						if ($scope.apps[i].clientId == client) {
							$scope.apps[i] = app;
							if ($scope.clientId == client) {
								$scope.clientId = app.clientId;
								$scope.app = angular.copy(app);
								$scope.initGrantTypes($scope.app);
							}
							return;
						}
					}
				} else {
					$scope.error = 'Failed to reset '+param+': '+response.errorMessage;
				}
			});
		}
	};
	/**
	 * generate or retrieve client access token through the client credentials OAuth2 flow.
	 */
	$scope.getClientToken = function() {
		$http(
				{method:'POST',
				 url:'oauth/token',
				 params:{client_id:$scope.app.clientId,client_secret:$scope.app.clientSecret,grant_type:'client_credentials'},
				 headers:{}
				})
		.success(function(data) {
			$scope.clientToken = data.access_token;
		}).error(function(data) {
			$scope.error = data.error_description;
		});
	};
	/**
	 * generate or retrieve client access token through the implicit OAuth2 flow.
	 */
	$scope.getImplicitToken = function() {
		if ($scope.app.grantedTypes.indexOf('implicit') < 0) {
			$scope.info = '';
			$scope.error = 'Implicit token requires Implicit grant type selected!';
			return;
		}
		var hostport = $location.host()+(($location.absUrl().indexOf(':'+$location.port())>0)?(":"+$location.port()):"");
		var ctx = $location.absUrl().substring($location.absUrl().indexOf(hostport)+hostport.length);
		ctx = ctx.substring(0,ctx.indexOf('/',1));
		var win = window.open('oauth/authorize?client_id='+$scope.app.clientId+'&response_type=token&grant_type=implicit&redirect_uri='+ctx+'/testtoken');
		win.onload = function() {
			var at = processAuthParams(win.location.hash.substring(1));
			$timeout(function(){
				if (at) {
					$scope.implicitToken = at;
				} else {
					$scope.info = '';
					$scope.error = 'Problem retrieving the token!';
				}
			},100);
			win.close();
		};
	};
};

/**
 * Parse authentication parameters obtained from implicit flow authorization request 
 * @param input
 * @returns
 */
function processAuthParams(input) {
	var params = {}, queryString = input;
	var regex = /([^&=]+)=([^&]*)/g;
	while (m = regex.exec(queryString)) {
	  params[m[1]] = m[2];
	}
	return params.access_token;
}

/**
 * Service management controller.
 * @param $scope
 * @param $resource
 * @param $http
 * @param $timeout
 */
function ServiceController($scope, $resource, $http, $timeout, $location) {
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
}