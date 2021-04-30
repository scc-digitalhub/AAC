angular.module('aac.controllers.realm', [])

  /**
	 * Main realm layout controller
	 */
  .controller('RealmController', function ($scope, $rootScope, $state, $stateParams, RealmData, Utils) {
    var slug = $stateParams.realmId;

    if (slug) {
      // global admin or realm admin
      $scope.realmAdmin = $rootScope.user.authorities.findIndex(function (a) { return a.realm == slug && a.role == 'ROLE_ADMIN' || a.authority == 'ROLE_ADMIN' }) >= 0;
      // realm admin or developer
      $scope.realmDeveloper = $rootScope.user.authorities.findIndex(function (a) { return a.realm == slug && a.role == 'ROLE_DEVELOPER' }) >= 0 || $scope.realmAdmin;
      RealmData.getRealm(slug)
        .then(function (data) {
          $scope.realm = data;
          $scope.realmImmutable = data.slug == 'system' || data.slug == '';
        })
        .catch(function (err) {
          Utils.showError('Failed to load realm: ' + err.data.message);
        });
    } else {
      RealmData.getMyRealms()
        .then(function (data) {
          $scope.realm = data[0];
          $state.go('realm', { realmId: $scope.realm.slug, isGlobal: !$scope.realm.slug });
        })
        .catch(function (err) {
          Utils.showError('Failed to load realms: ' + err.data.message);
        });
    }
  })
  
  .controller('RealmDashboardController', function ($scope, $rootScope, $state, $stateParams, RealmData, Utils) {
    var slug = $stateParams.realmId;
    RealmData.getRealmStats(slug).then(function(stats) {
      $scope.stats = stats;
    })
    .catch(function (err) {
      Utils.showError('Failed to load realm: ' + err.data.message);
    });
    
    

  })

  /**
	 * Realm users controller
	 */
  .controller('RealmUsersController', function ($scope, $stateParams, RealmData, Utils) {
    var slug = $stateParams.realmId;
    $scope.query = {
      page: 0,
      size: 20,
      sort: { username: 1 },
      q: ''
    }

    $scope.load = function () {
      RealmData.getRealmUsers(slug, $scope.query)
        .then(function (data) {
          $scope.users = data;
          $scope.users.content.forEach(function (u) {
            u._providers = u.identities.map(function (i) {
              return $scope.providers[i.provider] ? $scope.providers[i.provider].name : i.provider;
            });
            u._authorities = u.authorities
              .filter(function (a) { return a.realm === $scope.realm.slug })
              .map(function (a) { return a.role });
          });
        })
        .catch(function (err) {
          Utils.showError('Failed to load realm users: ' + err.data.message);
        });
    }

    /**
	 * Initialize the app: load list of the users
	 */
    var init = function () {
      RealmData.getRealmProviders(slug)
        .then(function (providers) {
          var pMap = {};
          providers.forEach(function (p) { pMap[p.provider] = p });
          $scope.providers = pMap;
          $scope.load();
        })
        .catch(function (err) {
          Utils.showError('Failed to load realm users: ' + err.data.message);
        });
    };

    $scope.deleteUserDlg = function (user) {
      $scope.modUser = user;
      $('#deleteConfirm').modal({ keyboard: false });
    }

    $scope.deleteUser = function () {
      $('#deleteConfirm').modal('hide');
      RealmData.removeUser($scope.realm.slug, $scope.modUser).then(function () {
        $scope.load();
      }).catch(function (err) {
        Utils.showError(err.data.message);
      });
    }

    $scope.setPage = function (page) {
      $scope.query.page = page;
      $scope.load();
    }

    init();

    $scope.editRoles = function (user) {
      $scope.modUser = user;
      var systemRoles = ['ROLE_ADMIN', 'ROLE_DEVELOPER'];
      $scope.roles = {
        system_map: {}, map: {}, custom: ''
      }
      user._authorities.forEach(function (a) {
        if (systemRoles.indexOf(a) >= 0) $scope.roles.system_map[a] = true;
        else $scope.roles.map[a] = true;
      });
      $('#rolesModal').modal({ backdrop: 'static', focus: true })

    }

    $scope.hasRoles = function (m1, m2) {
      var res = false;
      for (var r in m1) res |= m1[r];
      for (var r in m2) res |= m2[r];
      return res;
    }

    // save roles
    $scope.updateRoles = function () {
      var roles = [];
      for (var k in $scope.roles.system_map) if ($scope.roles.system_map[k]) roles.push(k);
      for (var k in $scope.roles.map) if ($scope.roles.map[k]) roles.push(k);

      $('#rolesModal').modal('hide');
      RealmData.updateRealmRoles($scope.realm.slug, $scope.modUser, roles)
        .then(function () {
          $scope.load();
          Utils.showSuccess();
        })
        .catch(function (err) {
          Utils.showError(err);
        });
    }
    $scope.dismiss = function () {
      $('#rolesModal').modal('hide');
    }

    $scope.addRole = function () {
      $scope.roles.map[$scope.roles.custom] = true;
      $scope.roles.custom = null;
    }

    $scope.invalidRole = function (role) {
      return !role || !(/^[a-zA-Z0-9_]{3,63}((\.[a-zA-Z0-9_]{2,63})*\.[a-zA-Z]{2,63})?$/g.test(role))
    }

})

/**
 * Realm providers controller
 */
.controller('RealmProvidersController', function ($scope, $stateParams, RealmData, Utils) {  
  var slug = $stateParams.realmId;
  
  $scope.load = function() {
    RealmData.getRealmProviders(slug)
    .then(function(data) {
      $scope.providers = data;
    })
    .catch(function(err) {
       Utils.showError('Failed to load realm providers: '+err.data.message);
    });
  }  
  
  /**
   * Initialize the app: load list of the providers
   */
  var init = function() {
    RealmData.getRealmProviderTemplates(slug)
    .then(function(data) {
      $scope.providerTemplates = data;
    });
    $scope.load();
  };
  
  $scope.deleteProviderDlg = function(provider) {
    $scope.modProvider = provider;
    $('#deleteConfirm').modal({keyboard: false});
  }
  
  $scope.deleteProvider = function() {
    $('#deleteConfirm').modal('hide');
    RealmData.removeProvider($scope.realm.slug, $scope.modProvider.provider).then(function() {
      $scope.load();
    }).catch(function(err) {
      Utils.showError(err.data.message);
    });
  }

  $scope.editProviderDlg = function(provider) {
    if (provider.authority == 'internal') {
      $scope.internalProviderDlg(provider);
    } else if (provider.authority == 'oidc') {
      $scope.oidcProviderDlg(provider);
    } else if (provider.authority == 'saml') {
      $scope.samlProviderDlg(provider);
    }
  }
  
  var toChips = function(str) {
    return str.split(',').map(function(e) { return e.trim() }).filter(function(e) {return !!e });
  }
  
  $scope.oidcProviderDlg = function(provider) {
    $scope.providerId = provider ? provider.provider : null;
    $scope.providerAuthority = 'oidc';
    $scope.provider = provider ? Object.assign({}, provider.configuration) : 
    {clientAuthenticationMethod: 'client_secret_basic', scope: 'openid,profile,email', userNameAttributeName: 'sub'};
    $scope.provider.name = provider ? provider.name : null;
    $scope.provider.clientName = $scope.provider.clientName || '';
    $scope.provider.scope = toChips($scope.provider.scope);
    $scope.provider.persistence = provider ? provider.persistence : 'none';

    $scope.oidcProviderTemplates = $scope.providerTemplates ? $scope.providerTemplates.filter(function(pt) {return pt.authority === 'oidc'}) : [];
    
    $('#oidcModal').modal({backdrop: 'static', focus: true})
    Utils.refreshFormBS();
  }
  $scope.internalProviderDlg = function (provider) {
    $scope.providerId = provider ? provider.provider : null;
    $scope.providerAuthority = 'internal';
    $scope.provider = provider ? Object.assign({}, provider.configuration) :
      { enableUpdate: true, enableDelete: true, enableRegistration: true, enablePasswordReset: true, enablePasswordSet: true, confirmationRequired: true, passwordMinLength: 8 };
    $scope.provider.name = provider ? provider.name : null;
    $('#internalModal').modal({ backdrop: 'static', focus: true })
    Utils.refreshFormBS();
  }

  $scope.samlProviderDlg = function (provider) {
    $scope.providerId = provider ? provider.provider : null;
    $scope.providerAuthority = 'saml';
    $scope.provider = provider ? Object.assign({}, provider.configuration) :
      { signAuthNRequest: 'true', ssoServiceBinding: 'HTTP-POST' };
    $scope.provider.name = provider ? provider.name : null;
    $scope.provider.persistence = provider ? provider.persistence : 'none';
    $('#samlModal').modal({ backdrop: 'static', focus: true })
    Utils.refreshFormBS();
  }

  $scope.saveProvider = function () {
    $('#' + $scope.providerAuthority + 'Modal').modal('hide');

    // HOOK: OIDC contains scopes to be converted to string
    if ($scope.providerAuthority === 'oidc' && $scope.provider.scope) {
      $scope.provider.scope = $scope.provider.scope.map(function (s) { return s.text }).join(',');
    }
    var name = $scope.provider.name;
    delete $scope.provider.name;
    var persistence = $scope.provider.persistence;
    delete $scope.provider.persistence;
    
    var data = { realm: $scope.realm.slug, name: name, persistence: persistence, configuration: $scope.provider, authority: $scope.providerAuthority, type: 'identity', providerId: $scope.providerId };
    RealmData.saveProvider($scope.realm.slug, data)
      .then(function () {
        $scope.load();
        Utils.showSuccess();
      })
      .catch(function (err) {
        Utils.showError(err.data.message);
      });
  }
  
  $scope.toggleProviderState = function(item) {
    RealmData.changeProviderState($scope.realm.slug, item.provider, item.enabled)
    .then(function() {
      Utils.showSuccess();    
    })
    .catch(function(err) {
      Utils.showError(err.data.message);
    });
  
  }
  
  $scope.updateProviderType = function()  {
    if ($scope.provider.clientName) {
      var ptIdx = $scope.oidcProviderTemplates.findIndex(function(pt) { return pt.name === $scope.provider.clientName});
      if (ptIdx >= 0) {
        var pt = Object.assign({}, $scope.oidcProviderTemplates[ptIdx].configuration);
        pt.name = $scope.provider.name;
        pt.clientId = $scope.provider.clientId;
        pt.clientSecret = $scope.provider.clientSecret;
        pt.scope = toChips(pt.scope);
        $scope.provider = pt; // 167055061711-7i61n2hlbum3arejn6cf6bf5t6jb0e6u.apps.googleusercontent.com 39fdo9LTog7YzcLhS0nkLDpe
      }
    }
    Utils.refreshFormBS();
  }
  
  $scope.updatePersistenceType = function()  {
    Utils.refreshFormBS();
  }
  

  init();
})  

/**
   * Realm client controller
   */
  .controller('RealmAppsController', function ($scope, $stateParams, $state, RealmData, Utils) {
    var slug = $stateParams.realmId;

    $scope.load = function () {
      RealmData.getClientApps(slug)
        .then(function (data) {
          $scope.apps = data;
        })
        .catch(function (err) {
          Utils.showError('Failed to load realm client apps: ' + err.data.message);
        });
    }

    /**
   * Initialize the app: load list of apps
   */
    var init = function () {
      $scope.load();
    };

    $scope.createClientAppDlg = function () {
      $scope.modClientApp = {
        name: '',
        type: '',
        realm: slug
      };

      $('#createClientAppDlg').modal({ keyboard: false });
    }
    
    $scope.createClientApp = function() {
      $('#createClientAppDlg').modal('hide');

    	
      RealmData.saveClientApp($scope.realm.slug, $scope.modClientApp)
        .then(function (res) {
          $state.go('realm.app',{realmId:res.realm, clientId:res.clientId} );
          Utils.showSuccess();
        })
        .catch(function (err) {
          Utils.showError(err.data.message);
        });    	
    	
    }
    
    $scope.deleteClientAppDlg = function (clientApp) {
      $scope.modClientApp = clientApp;
      //add confirm field
      $scope.modClientApp.confirmId = '';
      $('#deleteClientAppConfirm').modal({ keyboard: false });
    }

    $scope.deleteClientApp = function () {
      $('#deleteClientAppConfirm').modal('hide');
      if($scope.modClientApp.clientId === $scope.modClientApp.confirmId) {
	      RealmData.removeClientApp($scope.realm.slug, $scope.modClientApp.clientId).then(function () {
	        $scope.load();
	      }).catch(function (err) {
	        Utils.showError(err.data.message);
	      });
      } else {
    	  Utils.showError("confirmId not valid");
      }
    }


    init();
  })
  .controller('RealmAppController', function ($scope, $stateParams, RealmData, Utils) {
    var slug = $stateParams.realmId;
    var clientId = $stateParams.clientId;



    /**
   * Initialize the app: load list of apps
   */
    var init = function () {
      //we load provider resources only at first load since it's expensive
      RealmData.getResources(slug)
        .then(function (resources) {
          $scope.resources = resources;
          return resources;
        })
        .then(function () {
          return RealmData.getRealmProviders(slug)
        })
        .then(function (providers) {
          $scope.identityProviders = providers.filter(p => p.type === 'identity');
          return $scope.identityProviders;
        })
        .then(function () {
          return RealmData.getClientApp(slug, clientId);
        })
        .then(function (data) {
          $scope.load(data);
          $scope.clientView = 'overview';
          return data;
        })
        .catch(function (err) {
          Utils.showError('Failed to load realm client app: ' + err.data.message);
        });


    };

    $scope.load = function (data) {
      //set
      $scope.app = data;
      $scope.appname = data.name;

      // process idps
      var idps = [];

      // process scopes scopes
      var scopes = [];
      if (data.scopes) {
        data.scopes.forEach(function (s) {
          scopes.push({ 'text': s });
        });
      }
      $scope.appScopes = scopes;
  	console.log($scope.appScopes);

      $scope.updateResources(data.scopes);
      $scope.updateIdps(data.providers);

      if (data.type == 'oauth2') {
        $scope.initConfiguration(data.type, data.configuration, data.schema);
      }

      return;
    }

    $scope.initConfiguration = function (type, config, schema) {

      if (type === 'oauth2') {
        // grantTypes
        var grantTypes = [];
        schema.properties.authorizedGrantTypes.items["enum"].forEach(function (e) {
          grantTypes.push({
            "key": e,
            "value": (config.authorizedGrantTypes.includes(e))
          })
        });
        $scope.oauth2GrantTypes = grantTypes;

        // authMethods
        var authMethods = [];
        schema.properties.authenticationMethods.items["enum"].forEach(function (e) {
          authMethods.push({
            "key": e,
            "value": (config.authenticationMethods.includes(e))
          })
        });
        $scope.oauth2AuthenticationMethods = authMethods;


        // redirects
        var redirectUris = [];
        if (config.redirectUris) {
          config.redirectUris.forEach(function (u) {
            if (u && u.trim()) {
              redirectUris.push({ 'text': u });
            }
          });
        }
        $scope.oauth2RedirectUris = redirectUris;
      }


    }



    extractConfiguration = function (type, config) {

      var conf = config;

      if (type === 'oauth2') {
        // extract grantTypes
        var grantTypes = [];
        for (var gt of $scope.oauth2GrantTypes) {
          if (gt.value) {
            grantTypes.push(gt.key);
          }
        }
        conf.authorizedGrantTypes = grantTypes;

        var authMethods = [];
        for (var am of $scope.oauth2AuthenticationMethods) {
          if (am.value) {
            authMethods.push(am.key);
          }
        }
        conf.authenticationMethods = authMethods;

        var redirectUris = $scope.oauth2RedirectUris.map(function (r) {
          if (r.hasOwnProperty('text')) {
            return r.text;
          }
          return r;
        });
        conf.redirectUris = redirectUris;


      }

      return conf;

    }
   

    $scope.saveClientApp = function (clientApp) {

      var configuration = extractConfiguration(clientApp.type, clientApp.configuration);

      var scopes = $scope.appScopes.map(function (s) {
          if (s.hasOwnProperty('text')) {
            return s.text;
          }
          return s;
        });
      
      var providers = $scope.identityProviders.map(function (idp) {
          if (idp.value === true) {
            return idp.provider;
          }
        }).filter(function(idp) {return !!idp });
      
      var data = {
        realm: clientApp.realm,
        clientId: clientApp.clientId,
        type: clientApp.type,
        name: clientApp.name,
        description: clientApp.description,
        configuration: configuration,
        scopes: scopes,
        providers: providers,
        resourceIds: clientApp.resourceIds,
        hookFunctions: clientApp.hookFunctions
      };

      RealmData.saveClientApp($scope.realm.slug, data)
        .then(function (res) {
          $scope.load(res);
          Utils.showSuccess();
        })
        .catch(function (err) {
          Utils.showError(err.data.message);
        });
    }

    $scope.resetClientCredentialsDlg = function (clientApp) {
      $scope.modClientApp = clientApp;
      $('#resetClientCredentialsConfirm').modal({ keyboard: false });
    }

    $scope.resetClientCredentials = function () {
      $('#resetClientCredentialsConfirm').modal('hide');
      RealmData.resetClientAppCredentials($scope.realm.slug, $scope.modClientApp.clientId).then(function (res) {
        $scope.load(res);
        Utils.showSuccess();
      }).catch(function (err) {
        Utils.showError(err.data.message);
      });
    }



    $scope.deleteClientAppDlg = function (clientApp) {
      $scope.modClientApp = clientApp;
      $('#deleteClientAppConfirm').modal({ keyboard: false });
    }

    $scope.deleteClientApp = function () {
      $('#deleteClientAppConfirm').modal('hide');
      RealmData.removeClientApp($scope.realm.slug, $scope.modClientApp.clientId).then(function () {
        $scope.load();
      }).catch(function (err) {
        Utils.showError(err.data.message);
      });
    }


    $scope.activeClientView = function (view) {
      return view == $scope.clientView ? 'active' : '';
    };

    $scope.switchClientView = function (view) {
      console.log("switch view to " + view);
      $scope.clientView = view;
    }

    $scope.updateResources = function (scopes) {
      var resources = [];

      for (var res of $scope.resources) {
        //inflate value for scopes
        res.scopes.forEach(function (s) {
          s.value = scopes.includes(s.scope)
        });

        resources.push(res);

      }

      $scope.scopeResources = resources;
    }

    $scope.updateIdps = function (providers) {
      var idps = [];

      for (var idp of $scope.identityProviders) {
        //inflate value
        idp.value = providers.includes(idp.provider)
        idps.push(idp);
      }

      $scope.identityProviders = idps;
    }    
    
    $scope.updateClientAppScopeResource = function() {
    	var resource = $scope.scopeResource;
    	var scopesToRemove = resource.scopes
    	.filter(function(s) {
    		return !s.value
    	})
    	.map( function(s) {
    		return s.scope;
    	});
    	var scopesToAdd = resource.scopes
    	.filter(function(s) {
    		return s.value
    	})
    	.map( function(s) {
    		return s.scope;
    	});    	    	    
    	
	    var scopes = $scope.appScopes.map(function (s) {
	          if (s.hasOwnProperty('text')) {
	            return s.text;
	          }
	          return s;
	    }).filter(s => !scopesToRemove.includes(s));
	    	    
	    scopesToAdd.forEach(function (s) {
    		 if(!scopes.includes(s)) {
    			scopes.push(s); 
    		 } 
    	 });
    	 
    	 //inflate again
         var appScopes = [];
           scopes.forEach(function (s) {
             appScopes.push({ 'text': s });
           });
         
    	 $scope.appScopes = appScopes;
    	console.log($scope.appScopes);
    }
    

    $scope.scopesDlg = function (resource) {
	    var scopes = $scope.appScopes.map(function (s) {
	          if (s.hasOwnProperty('text')) {
	            return s.text;
	          }
	          return s;
	    });
     	resource.scopes.forEach(function (s) {
     		s.value  = scopes.includes(s.scope);
   		});
      
      $scope.scopeResource = resource;
      $('#scopesModal').modal({ backdrop: 'static', focus: true })
      Utils.refreshFormBS();
    }

    init();
  })
;