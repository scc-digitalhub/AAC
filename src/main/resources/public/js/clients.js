angular.module('aac.controllers.clients', [])

/**
 * App management controller.
 * @param $scope
 * @param $resource
 * @param $http
 * @param $timeout
 */
.controller('AppListController', function ($scope, $resource, $http, $timeout, $location, Utils) {
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
				var apps = response.data;
				$scope.apps = apps;
			} else {
				Utils.showError('Failed to load apps: '+response.errorMessage);
			}	
		}, Utils.showError);
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
					var app = response.data;
					$scope.apps.push(app);
					$scope.switchClient(app.clientId);
				} else {
					Utils.showError('Failed to create new app: '+response.errorMessage);
				}
			}, Utils.showError);
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
.controller('AppController', function ($scope, $resource, $routeParams, $http, $timeout, $location, Data, Utils) {
	// current client
	$scope.app = null;
	// current client ID
	$scope.clientId = 'none';
	// current view (overview/settings/permissions)
	$scope.clientView = 'overview';
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
				{label:"Google Client Secret", value: "client_secret", required: true, type: "password"},
				{label:"Google Client IDs for id_token validation", value: "client_ids", required: false}
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
	
	$scope.claimEnabled = {checked: false};
	
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
	
	// resource reference for the claim validation API
	var ClientClaimValidation = $resource('dev/apps/:clientId/claimmapping/validate', {}, {
		validate : { method : 'POST' },		
	});

	
	/**
	 * Initialize the app: load list of the developer's apps and reset views
	 */
	var init = function() {	    
		ClientAppBasic.query({clientId: $routeParams.clientId},function(response){
			if (response.responseCode == 'OK') {
				var app = response.data;
				$scope.app = angular.copy(app);
				$scope.redirectUris = ($scope.app.redirectUris || '').split(',').map(function(r) {
					return {text: r.trim()};
				});
				$scope.uniqueSpaces = ($scope.app.uniqueSpaces || []);
				$scope.initGrantTypes($scope.app);
				$scope.clientId = app.clientId;
				$scope.claimEnabled.checked = !!app.claimMapping;
				$scope.switchClientView('overview');
			} else {
				Utils.showError('Failed to load apps: '+response.errorMessage);
			}	
		}, Utils.showError);
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
		
		$scope.clientToken = null;  
		$scope.implicitToken = null;  
	};

	/**
	 * switch to other permissions subview. reset messages
	 */
	$scope.switchPermView = function(view) {
		$scope.permView = view;
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
				Utils.showError('Failed to unsubscribe: '+response.responseCode);
			}	
		}, Utils.showError);
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
				Utils.showError('Failed to subscribe: '+response.responseCode);
			}	
		}, Utils.showError);
		
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
	 * switch to app 'roles and claims'
	 */
	$scope.viewClaims = function() {
		$scope.switchClientView('claims');
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
		newClient.$get({clientId:$scope.clientId, serviceId: service.serviceId}, function(response) {
			if (response.responseCode == 'OK') {
				$scope.permissions = response.data;
				callback();
			} else {
				Utils.showError('Failed to load app permissions: '+response.errorMessage);
			}	
		}, Utils.showError);
	}
	
	
	/**
	 * delete client app key
	 */
	$scope.deleteKey = function(item) {
		if (confirm('Are you sure you want to delete?')) {
			var newClient = new ClientAppKeys();
			newClient.$remove({clientId:$scope.clientId, apiKey: item.apiKey},function(response){
				if (response.responseCode == 'OK') {
					loadKeys();
				} else {
					Utils.showError('Failed to remove key: '+response.errorMessage);
				}	
			}, Utils.showError);
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
					loadKeys();
				} else {
					Utils.showError('Failed to save api key: '+response.errorMessage);
				}
			}, function(err) {
				Utils.showError('Failed to save api key: '+err.message);
			});
		} else {
			newClient.$save({clientId:$scope.clientId}, function(response){
				if (response.responseCode == 'OK') {
					loadKeys();
				} else {
					Utils.showError('Failed to save api key: '+response.data.errorMessage);
				}
			}, Utils.showError);
		}
		
	}
	
	/**
	 * load keys of the current app.
	 */
	function loadKeys() {
		var newClient = new ClientAppKeys();
		newClient.$get({clientId:$scope.clientId}, function(response) {
			if (response.responseCode == 'OK') {
				if (response.data) {
					var now = new Date().getTime();
					response.data.forEach(function(key) {
						key.expired = key.validity && key.validity > 0 && now > key.validity + key.issuedTime;
					});
				}
				$scope.apiKeys = response.data;
			} else {
				Utils.showError('Failed to load app keys: '+response.errorMessage);
			}	
		}, Utils.showError);
	}
	/**
	 * load services available
	 */
	function loadServices() {
		var newClient = new AppServices();
		newClient.$query({clientId:$scope.clientId}, function(response) {
			if (response.responseCode == 'OK') {
				$scope.services = response.data.content;
			} else {
				Utils.showError('Failed to load app permissions: '+response.errorMessage);
			}	
		}, Utils.showError);
	}

	/**
	 * delete client app
	 */
	$scope.removeClient = function() {
		if (confirm('Are you sure you want to delete?')) {
			var newClient = new ClientAppBasic();
			newClient.$remove({clientId:$scope.clientId},function(response){
				if (response.responseCode == 'OK') {
					$location.path('/apps');
				} else {
					Utils.showError('Failed to remove app: '+response.errorMessage);
				}	
			}, Utils.showError);
	    }
	};

	/**
	 * Save current app settings
	 */
	$scope.saveSettings = function() {
		var newGt = [];
		for (var k in $scope.grantTypes) if ($scope.grantTypes[k]) newGt.push(k);
		if ($scope.redirectUris) {
			$scope.app.redirectUris = $scope.redirectUris.map(function(r) {
				return r.text;
			}).join(',');
		}
		$scope.app.grantedTypes = newGt;
		var newClient = new ClientAppBasic($scope.app);
		newClient.$update({clientId:$scope.clientId}, function(response) {
			if (response.responseCode == 'OK') {
				Utils.showSuccess();
				var app = response.data;
				$scope.app = angular.copy(app);
				$scope.initGrantTypes($scope.app);
			} else {
				Utils.showError('Failed to save settings: '+response.errorMessage);
			}
		}, Utils.showError);
	};
	/**
	 * Save current app claim and role data
	 */
	$scope.saveClaims = function() {
		if ($scope.uniqueSpaces) {
			$scope.uniqueSpaces = $scope.uniqueSpaces ? $scope.uniqueSpaces.filter(function(r) { return r && r.text}) : [];
			$scope.app.uniqueSpaces = $scope.uniqueSpaces.map(function(r) {
				return r.text;
			});
		}
		var newClient = new ClientAppBasic($scope.app);
		newClient.$update({clientId:$scope.clientId}, function(response) {
			if (response.responseCode == 'OK') {
				Utils.showSuccess();
				var app = response.data;
				$scope.app = angular.copy(app);
				$scope.initGrantTypes($scope.app);
			} else {
				Utils.showError('Failed to save claims: '+response.errorMessage);
			}
		}, Utils.showError);
	};
	/**
	 * Toggle claim mapping text
	 */
	$scope.toggleClaimMapping = function() {
		$scope.claimEnabled.checked = !$scope.claimEnabled.checked;
		if (!!$scope.app.claimMapping) {
			$scope.app.claimMapping = null;
		} else {
			$scope.app.claimMapping = 
				'/**\n * DEFINE YOUR OWN CLAIM MAPPING HERE\n' +
				'**/\n'+
				'function claimMapping(claims) {\n   return claims;\n}';
		}
	}
	$scope.aceOption = {
	    mode: 'javascript',
	    theme: 'monokai',
	    maxLines: 30,
        minLines: 6,	
//        autoScrollEditorIntoView: true
	  };
	
	$scope.validateClaims = function() {
		$scope.validationResult = '';
		$scope.validationError = '';
		var newClient = new ClientClaimValidation($scope.app);
		newClient.$validate({clientId:$scope.clientId}, function(response) {
			if (response.responseCode == 'ERROR') {
				$scope.validationError = response.errorMessage; 			
				
			} else {
				$scope.validationResult = response.data;
			}
		}, function(e) {
			$scope.validationResult = '';
			$scope.validationError = e; 			
		});
	}
	
	
	/**
	 * Save current app permissions
	 */
	$scope.savePermissions = function(service) {
		var perm = new ClientAppPermissions($scope.permissions);
		perm.$update({clientId:$scope.clientId, serviceId:service.serviceId}, function(response) {
			$('#myModal').modal('hide');
			if (response.responseCode == 'OK') {
				Utils.showSuccess();
				$scope.permissions = response.data;
			} else {
				Utils.showError('Failed to save app permissions: '+response.errorMessage);
			}	
		}, Utils.showError);
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
					Utils.showSuccess();

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
					Utils.showError('Failed to reset '+param+': '+response.errorMessage);
				}
			}, Utils.showError);
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
			Utils.showError(data.error_description);
		});
	};
	/**
	 * generate or retrieve client access token through the implicit OAuth2 flow.
	 */
	$scope.getImplicitToken = function() {
		if ($scope.app.grantedTypes.indexOf('implicit') < 0) {
			Utils.showError('Implicit token requires Implict grant type selected!');
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
					Utils.showError('Problem retrieving the token!');
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
