angular.module('aac.controllers.clients', [])

/**
 * App management controller.
 * @param $scope
 * @param $resource
 * @param $http
 * @param $timeout
 */
.controller('AppListController', function ($scope, $resource, $http, $timeout, $location) {
	$scope.apps = null;
	
	// resource reference for the app API
	var ClientAppBasic = $resource('dev/apps/:clientId', {}, {
		query : { method : 'GET' }
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
			} else {
				$scope.error = 'Failed to load apps: '+response.errorMessage;
			}	
		});
	};
	init();
	
	
	/**
	 * switch to different client
	 */
	$scope.switchClient = function(client) {
		$location.path('/apps/'+client);
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
})


/**
 * App management controller.
 * @param $scope
 * @param $resource
 * @param $routeParams
 * @param $http
 * @param $timeout
 */
.controller('AppController', function ($scope, $resource, $routeParams, $http, $timeout, $location, Data) {
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
	// configuration specific for different IdPs
	$scope.providerConfig = {
			google: [
				{label:"Google Client ID", value: "client_id", required: true},
				{label:"Google Client Secret", value: "client_secret", required: true, type: "password"}				
			],
			facebook: [
				{label:"Facebook Client ID", value: "client_id", required: true},
				{label:"Facebook Client Secret", value: "client_secret", required: true, type: "password"}				
			],
	};

	$scope.GTLabels = {
			implicit: 'Implicit',
			authorization_code: 'Authorization Code',
			password: 'Password',
			client_credentials: 'Client Credentials',
			refresh_token: 'Refresh token',
			native: 'Native'
			
	}
	$scope.grantTypes = {
			implicit: false,
			authorization_code: false,
			password: false,
			client_credentials: false,
			refresh_token: false,
			native: false
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
	// resource reference for the permissions API
	var ClientAppKeys = $resource('dev/apikey/:clientId/:apiKey', {}, {
		update : { method : 'PUT' },		
	});

	// resource reference for the resources API
	var ClientAppResourceParam = $resource('dev/resourceparams/:id', {}, {
		create : { method : 'POST' },
		changeVis : {method : 'PUT'}
	});
	
	// resource reference for the services API
	var AppServices = $resource('dev/services/:clientId', {}, {
		query : {method : 'GET'},
	});
	
	var UnsubscribeService = $resource('dev/services/unsubscribe/:subscriptionId', {}, {
		unsubscribe : {method : 'DELETE'}
	});
	
	var SubscribeService = $resource('dev/services/subscribe/:apiIdentifier/:clientId', {}, {
		subscribe : {method : 'GET'}
	});		
	
	// resource reference for the app API
	var AppSubscriptions = $resource('mgmt/applications/:applicationName/subscriptions', {}, {
		query : { method : 'GET' }
	});	
	
	
	/**
	 * Initialize the app: load list of the developer's apps and reset views
	 */
	var init = function() {
		ClientAppBasic.query({clientId: $routeParams.clientId},function(response){
			if (response.responseCode == 'OK') {
				$scope.error = '';
				var app = response.data;
				$scope.app = angular.copy(app);
				$scope.initGrantTypes($scope.app);
				$scope.clientId = app.clientId;
				$scope.switchClientView('overview');

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
			if (!!$scope.GTLabels[gt]) arr.push($scope.GTLabels[gt]);
		});
		return arr.join(', ');
	}
	
	$scope.idpIcon = function(req,app) {
		if (!req) return null;
		if (app == null) return 'fas fa-clock';
		if (app) return 'fas fa-check';
		return 'fas fa-ban';
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
	 * Unsubscribe
	 */
	$scope.unsubscribe = function(subscriptionId) {
		UnsubscribeService.unsubscribe({subscriptionId: subscriptionId},function(response){
			if (response.responseCode == 'OK') {
				$scope.viewPermissions();
			} else {
				$scope.error = 'Failed to unsubscribe: '+response.responseCode;
			}	
		});	
	};
		
	/**
	 * Subscribe
	 */
	$scope.subscribe = function(apiIdentifier) {
		console.log($scope.app.name)
		console.log(apiIdentifier)
		SubscribeService.subscribe({apiIdentifier:apiIdentifier,clientId:$scope.clientId},function(response){
			if (response.responseCode == 'OK') {
				$scope.viewPermissions();
			} else {
				$scope.error = 'Failed to Subscribe: '+response.responseCode;
			}	
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
	$scope.viewKeys = function() {
		$scope.switchClientView('keys');
		loadKeys();
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
	 * delete client app key
	 */
	$scope.deleteKey = function(item) {
		if (confirm('Are you sure you want to delete?')) {
			var newClient = new ClientAppKeys();
			newClient.$remove({clientId:$scope.clientId, apiKey: item.apiKey},function(response){
				if (response.responseCode == 'OK') {
					$scope.error = '';
					loadKeys();
				} else {
					$scope.error = 'Failed to remove key: '+response.errorMessage;
				}	
			});
	    }
	};
	
	/**
	 * edit API key - open a form 
	 */
	$scope.editKey = function(item) {
		$scope.currentAPIKey = angular.copy(item);
		$scope.currentAPIKey.scope = $scope.currentAPIKey.scope != null ? $scope.currentAPIKey.scope.join(', ') : '';
		$('#keyModal').modal({keyboard:false});
	};
	/**
	 * New API key - open a form 
	 */
	$scope.newKey = function() {
		$scope.currentAPIKey = {};
		$('#keyModal').modal({keyboard:false});
	};
	
	$scope.saveAPIKey = function(key) {
		if (key.validity <= 0) {
			key.validity = null;
		}
		if (!!key.scope) {			
			key.scope = key.scope.replace(' ', '');
			key.scope = key.scope.split(',');
		} else {
			key.scope = [];
		}
		var newClient = new ClientAppKeys(key);
		$('#keyModal').modal('hide');
		if (key.apiKey != null) {
			newClient.$update({clientId:$scope.clientId, apiKey:key.apiKey},function(response){
				if (response.responseCode == 'OK') {
					$scope.error = '';
					loadKeys();
				} else {
					$scope.error = 'Failed to save api key: '+response.errorMessage;
				}
			}, function(err) {
				$scope.error = 'Failed to save api key: '+err.message;
			});
		} else {
			newClient.$save({clientId:$scope.clientId}, function(response){
				if (response.responseCode == 'OK') {
					$scope.error = '';
					loadKeys();
				} else {
					$scope.error = 'Failed to save api key: '+response.errorMessage;
				}
			}, function(err) {
				$scope.error = 'Failed to save api key: '+response.errorMessage;
			});
		}
		
	}
	
	/**
	 * load keys of the current app.
	 */
	function loadKeys() {
		var newClient = new ClientAppKeys();
		newClient.$get({clientId:$scope.clientId}, function(response) {
			if (response.responseCode == 'OK') {
				$scope.error = '';
				if (response.data) {
					var now = new Date().getTime();
					response.data.forEach(function(key) {
						key.expired = key.validity && key.validity > 0 && now > key.validity + key.issuedTime;
					});
				}
				$scope.apiKeys = response.data;
			} else {
				$scope.error = 'Failed to load app keys: '+response.errorMessage;
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
	 * delete client app
	 */
	$scope.removeClient = function() {
		if (confirm('Are you sure you want to delete?')) {
			var newClient = new ClientAppBasic();
			newClient.$remove({clientId:$scope.clientId},function(response){
				if (response.responseCode == 'OK') {
					$scope.error = '';
					$location.path('/apps');
				} else {
					$scope.error = 'Failed to remove app: '+response.errorMessage;
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
		case 1: return 'fas fa-check';
		case 2: return 'fas fa-ban';
		case 3: return 'fas fa-clock';
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
		if (val) return 'fas fa-check';
		else return 'fas fa-ban';
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
			$scope.error = 'Implicit token requires Implict grant type selected!';
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
	
	$scope.copyBearer = function(token) {
//		console.log(navigator);
//		console.log(navigator.clipboard);
//		navigator.clipboard.writeText("Bearer " + token).then(function() {
//			}, function() {
//			});
		
		var textField = document.createElement('textarea');
	    textField.innerText = "Bearer " + token;
	    document.body.appendChild(textField);
	    textField.select();
	    document.execCommand('copy');
	    textField.remove();		
		
	}
	
});
