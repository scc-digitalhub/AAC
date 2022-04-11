angular.module('aac.controllers.realmapps', [])
    .directive('fileModel', ['$parse', function ($parse) {
        return {
            restrict: 'A',
            link: function (scope, element, attrs) {
                var model = $parse(attrs.fileModel);
                var modelSetter = model.assign; element.bind('change', function () {
                    scope.$apply(function () {
                        modelSetter(scope, element[0].files[0]);
                    });
                });
            }
        };
    }])
    /**
      * Realm Data Services
      */
    .service('RealmAppsData', function ($http, $httpParamSerializer) {
        var service = {};

        var buildQuery = function (params) {
            var q = Object.assign({}, params);
            if (q.sort) {
                var sort = [];
                for (var [key, value] of Object.entries(q.sort)) {
                    var s = key + ',' + (value > 0 ? 'asc' : 'desc');
                    sort.push(s);
                }
                q.sort = sort;
            }
            var queryString = $httpParamSerializer(q);
            return queryString;
        }

        service.getClientApps = function (slug) {
            return $http.get('console/dev/realms/' + slug + '/apps').then(function (data) {
                return data.data;
            });
        }
        service.searchClientApps = function (slug, params) {
            return $http.get('console/dev/realms/' + slug + '/apps/search?' + buildQuery(params)).then(function (data) {
                return data.data;
            });
        }
        service.getClientApp = function (slug, clientId) {
            return $http.get('console/dev/realms/' + slug + '/apps/' + clientId).then(function (data) {
                return data.data;
            });
        }

        service.removeClientApp = function (slug, clientId) {
            return $http.delete('console/dev/realms/' + slug + '/apps/' + clientId).then(function (data) {
                return data.data;
            });
        }

        service.resetClientAppCredentials = function (slug, clientId) {
            return $http.delete('console/dev/realms/' + slug + '/apps/' + clientId + '/credentials').then(function (data) {
                return data.data;
            });
        }


        service.saveClientApp = function (slug, clientApp) {
            if (clientApp.clientId) {
                return $http.put('console/dev/realms/' + slug + '/apps/' + clientApp.clientId, clientApp).then(function (data) {
                    return data.data;
                });
            } else {
                return $http.post('console/dev/realms/' + slug + '/apps', clientApp).then(function (data) {
                    return data.data;
                });
            }
        }

        service.importClientApp = function (slug, file, reset) {
            var fd = new FormData();
            fd.append('file', file);
            return $http({
                url: 'console/dev/realms/' + slug + '/apps' + (reset ? "?reset=true" : ""),
                headers: { "Content-Type": undefined }, //set undefined to let $http manage multipart declaration with proper boundaries
                data: fd,
                method: "PUT"
            }).then(function (data) {
                return data.data;
            });

        }

        service.testOAuth2ClientApp = function (slug, clientId, grantType) {
            return $http.get('console/dev/realms/' + slug + '/apps/' + clientId + "/oauth2/" + grantType).then(function (data) {
                return data.data;
            });
        }

        service.testClientAppClaimMapping = function (slug, clientId, functionCode) {
            return $http.post('console/dev/realms/' + slug + '/apps/' + clientId + "/claims", functionCode).then(function (data) {
                return data.data;
            });
        }

        service.getOAuth2Metadata = function (slug, clientId) {
            return $http.get('console/dev/realms/' + slug + '/well-known/oauth2').then(function (data) {
                return data.data;
            });
        }

        service.getRoles = function (slug, clientId) {
            return $http.get('console/dev/realms/' + slug + '/apps/' + clientId + '/roles').then(function (data) {
                return data.data;
            });
        }
        service.updateRoles = function (slug, clientId, roles) {
            return $http.put('console/dev/realms/' + slug + '/apps/' + clientId + '/roles', roles).then(function (data) {
                return data.data;
            });
        }

        service.getAuthorities = function (slug, subject) {
            return $http.get('console/dev/realms/' + slug + '/apps/' + subject + '/authorities').then(function (data) {
                return data.data;
            });
        }
        service.updateAuthorities = function (slug, subject, authorities) {
            return $http.put('console/dev/realms/' + slug + '/apps/' + subject + '/authorities', authorities).then(function (data) {
                return data.data;
            });
        }

        service.getSpaceRoles = function (slug, clientId) {
            return $http.get('console/dev/realms/' + slug + '/apps/' + clientId + '/spaceroles').then(function (data) {
                return data.data;
            });
        }
        service.updateSpaceRoles = function (slug, clientId, roles) {
            return $http.put('console/dev/realms/' + slug + '/apps/' + clientId + '/spaceroles', roles).then(function (data) {
                return data.data;
            });
        }

        service.getProviders = function (slug, clientId) {
            return $http.get('console/dev/realms/' + slug + '/apps/' + clientId + '/providers').then(function (data) {
                return data.data;
            });
        }

        service.getAudit = function (slug, clientId) {
            return $http.get('console/dev/realms/' + slug + '/apps/' + clientId + '/audit').then(function (data) {
                return data.data;
            });
        }
        service.getApprovals = function (slug, clientId) {
            return $http.get('console/dev/realms/' + slug + '/apps/' + clientId + '/approvals').then(function (data) {
                return data.data;
            });
        }
        service.getTokens = function (slug, clientId) {
            return $http.get('console/dev/realms/' + slug + '/apps/' + clientId + '/tokens').then(function (data) {
                return data.data;
            });
        }


        return service;

    })

    /**
   * Realm client controller
   */
    .controller('RealmAppsController', function ($scope, $stateParams, $state, $window, RealmAppsData, Utils) {
        var slug = $stateParams.realmId;
        $scope.query = {
            page: 0,
            size: 20,
            sort: { name: 1 },
            q: ''
        }
        $scope.keywords = '';

        $scope.load = function () {
            RealmAppsData.searchClientApps(slug, $scope.query)
                .then(function (data) {
                    data.content.forEach(function (app) {
                        //add icon
                        app.icon = iconProvider(app);
                        if ('roles' in app) {
                            app._roles = app.roles
                                .filter(function (a) { return slug == a.realm })
                                .map(function (a) { return a.role });
                        }
                        if ('authorities' in app) {
                            app._authorities = app.authorities
                                .filter(function (a) { return slug == a.realm })
                                .map(function (a) { return a.role });
                        }
                    });
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

        $scope.setPage = function (page) {
            $scope.query.page = page;
            $scope.load();
        }

        $scope.setQuery = function (query) {
            $scope.query.q = query;
            $scope.page = 0;
            $scope.load();
        }

        $scope.runQuery = function () {
            $scope.setQuery($scope.keywords);
        }

        $scope.createClientAppDlg = function (applicationType) {
            $scope.modClientApp = {
                name: '',
                type: 'oauth2',
                realm: slug,
                configuration: {
                    applicationType: applicationType
                }
            };

            $('#createClientAppDlgOAuth2').modal({ keyboard: false });
        }

        $scope.createClientApp = function () {
            $('#createClientAppDlgOAuth2').modal('hide');


            RealmAppsData.saveClientApp($scope.realm.slug, $scope.modClientApp)
                .then(function (res) {
                    $state.go('realm.app', { realmId: res.realm, clientId: res.clientId });
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
            if ($scope.modClientApp.clientId === $scope.modClientApp.confirmId) {
                RealmAppsData.removeClientApp($scope.realm.slug, $scope.modClientApp.clientId).then(function () {
                    $scope.load();
                    Utils.showSuccess();
                }).catch(function (err) {
                    Utils.showError(err.data.message);
                });
            } else {
                Utils.showError("confirmId not valid");
            }
        }

        $scope.importClientAppDlg = function () {
            $('#importClientAppDlg').modal({ keyboard: false });
        }


        $scope.importClientApp = function () {
            $('#importClientAppDlg').modal('hide');
            var file = $scope.importFile;
            var resetID = !!file.resetID;
            var mimeTypes = ['text/yaml', 'text/yml', 'application/x-yaml'];
            if (file == null || !mimeTypes.includes(file.type) || file.size == 0) {
                Utils.showError("invalid file");
            } else {
                RealmAppsData.importClientApp(slug, file, resetID)
                    .then(function () {
                        $scope.importFile = null;
                        $scope.load();
                        Utils.showSuccess();
                    })
                    .catch(function (err) {
                        Utils.showError(err.data.message);
                    });
            }
        }

        $scope.exportClientApp = function (clientApp) {
            $window.open('console/dev/realms/' + clientApp.realm + '/apps/' + clientApp.clientId + '/export');
        };



        $scope.editRoles = function (clientApp) {
            var roles = clientApp.authorities
                .filter(r => slug == r.realm)
                .map(r => {
                    return {
                        'text': r.role
                    };
                });

            $scope.modClient = {
                ...clientApp,
                'roles': roles
            };

            $('#rolesModal').modal({ backdrop: 'static', focus: true })

        }

        // save roles
        $scope.updateRoles = function () {
            $('#rolesModal').modal('hide');

            if ($scope.modClient) {
                var roles = $scope.modClient.roles.map(r => r.text);

                var data = roles.map(r => {
                    return { 'realm': slug, 'role': r }
                });

                RealmAppsData.updateRoles(slug, $scope.modClient.clientId, data)
                    .then(function () {
                        $scope.load();
                        Utils.showSuccess();
                    })
                    .catch(function (err) {
                        Utils.showError(err);
                    });

                $scope.modClient = null;
            }
        }



        $scope.copyText = function (txt) {
            var textField = document.createElement('textarea');
            textField.innerText = txt;
            document.body.appendChild(textField);
            textField.select();
            document.execCommand('copy');
            textField.remove();
        }

        var iconProvider = function (clientApp) {
            var icon = './italia/svg/sprite.svg#it-piattaforme';
            if (clientApp.type == 'oauth2') {
                if (clientApp.configuration.applicationType == 'web') {
                    icon = './italia/svg/sprite.svg#it-star-outline';
                }
                if (clientApp.configuration.applicationType == 'native') {
                    icon = './italia/svg/sprite.svg#it-card';
                }
                if (clientApp.configuration.applicationType == 'machine') {
                    icon = './italia/svg/sprite.svg#it-software';
                }
                if (clientApp.configuration.applicationType == 'spa') {
                    icon = './italia/svg/sprite.svg#it-presentation';
                }
            }

            return icon;
        }

        init();
    })
    .controller('RealmAppController', function ($scope, $stateParams, $state, RealmData, RealmAppsData, RealmServices, RealmRoles, RoleSpaceData, Utils, $window) {
        var slug = $stateParams.realmId;
        var clientId = $stateParams.clientId;
        $scope.clientView = 'overview';

        $scope.aceOption = {
            mode: 'javascript',
            theme: 'monokai',
            showGutter: false,
            maxLines: 30,
            minLines: 6
        };

        $scope.activeView = function (view) {
            return view == $scope.clientView ? 'active' : '';
        };

        $scope.switchView = function (view) {
            $scope.clientView = view;
            Utils.refreshFormBS(300);
        }

        var init = function () {
            //we load provider resources only at first load since it's expensive
            RealmData.getResources(slug)
                .then(function (resources) {
                    $scope.resources = resources;
                    return resources;
                })
                .then(function () {
                    return RealmAppsData.getProviders(slug, clientId)
                })
                .then(function (providers) {
                    return providers.filter(p => p.type === 'identity');
                })
                .then(function (providers) {
                    return providers.map(idp => {
                        return {
                            ...idp,
                            'icon': idpIconProvider(idp)
                        };
                    });
                })
                .then(function (providers) {
                    var activeProviders = providers.filter(p => p.registered);
                    var inactiveProviders = providers.filter(p => !p.registered);
                    return activeProviders.concat(inactiveProviders);
                })
                .then(function (providers) {
                    $scope.identityProviders = providers;
                    return providers;
                })
                .then(function () {
                    return RealmServices.getServices(slug);
                })
                .then(function (services) {
                    var sMap = new Map(services.map(e => [e.serviceId, e]));
                    $scope.services = sMap;
                })
                .then(function () {
                    return RealmRoles.getRoles(slug);
                })
                .then(function (roles) {
                    return Promise.all(
                        roles.map(r => {
                            return RealmRoles.getApprovals(slug, r.roleId)
                                .then(function (approvals) {
                                    return {
                                        ...r,
                                        approvals: approvals
                                    }
                                });
                        })
                    );
                })
                .then(function (roles) {
                    var rMap = new Map(roles.map(e => [e.role, e]));
                    $scope.realmRoles = rMap;
                })
                .then(function () {
                    return RoleSpaceData.getMySpaces();
                })
                .then(function (data) {
                    $scope.myspaces = data.map(s =>
                        ((s.context ? s.context + '/' : '') + (s.space || '')));
                })
                .then(function () {
                    $scope.load();
                })
                .catch(function (err) {
                    Utils.showError('Failed to load realm client app: ' + err.data.message);
                });


        };


        $scope.load = function () {
            RealmAppsData.getClientApp(slug, clientId)
                .then(function (data) {
                    $scope.reload(data);
                    $scope.clientView = 'overview';
                    return data;
                })
                .then(function (data) {
                    if (data.type == 'oauth2') {
                        var oauth2Tokens = {};

                        $scope.oauth2GrantTypes.forEach(function (gt) {
                            if ("authorization_code" == gt.key || "implicit" == gt.key || "client_credentials" == gt.key) {
                                oauth2Tokens[gt.key] = {
                                    token: null,
                                    decoded: null
                                }
                            }
                        });

                        $scope.oauth2Tokens = oauth2Tokens;
                    }

                    return data;
                })
                .then(function (data) {
                    //authorities
                    $scope.reloadAuthorities(data.authorities);
                    return data;
                })
                .then(function (data) {
                    $scope.reloadRoles(data.roles);
                    return data;
                })
                .then(function (data) {
                    $scope.reloadSpaceRoles(data.spaceRoles);
                    return;
                })
                .then(function () {
                    return RealmAppsData.getAudit(slug, clientId);
                })
                .then(function (events) {
                    $scope.audit = events;
                    return;
                })
                .then(function () {
                    return RealmAppsData.getApprovals(slug, clientId);
                })
                .then(function (approvals) {
                    $scope.reloadApprovals(approvals, $scope.app.roles.map(r => r.role));
                    return;
                })
                .catch(function (err) {
                    Utils.showError('Failed to load realm client app: ' + err.data.message);
                });


        }

        $scope.reload = function (data) {
            //set
            data.icon = iconProvider(data);
            $scope.app = data;
            $scope.appname = data.name;
            $scope.configurationMap = data.configuration;
            $scope.configurationSchema = data.schema;
            // process idps
            // var idps = [];

            // process scopes scopes
            var scopes = [];
            if (data.scopes) {
                data.scopes.forEach(function (s) {
                    scopes.push({ 'text': s });
                });
            }
            $scope.appScopes = scopes;

            updateResources(data.scopes);
            updateIdps(data.providers);

            if (data.type == 'oauth2') {
                initConfiguration(data.type, data.configuration, data.schema);
            }

            var claimMapping = {
                enabled: false,
                code: "",
                scopes: [],
                result: null,
                error: null,
                context: '{}'
            };
            if (data.hookFunctions != null && "claimMapping" in data.hookFunctions) {
                claimMapping.enabled = true;
                claimMapping.code = atob(data.hookFunctions["claimMapping"]);
            }

            $scope.claimMapping = claimMapping;


            //flow extensions hook
            var webHooks = {
                "afterTokenGrant": null
            }
            if (data.hookWebUrls != null) {
                if ("afterTokenGrant" in data.hookWebUrls) {
                    webHooks["afterTokenGrant"] = data.hookWebUrls["afterTokenGrant"];
                }
            }

            $scope.webHooks = webHooks;

            return;
        }

        var initConfiguration = function (type, config, schema) {
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



        var extractConfiguration = function (type, config) {

            var conf = config;

            if (type === 'oauth2') {
                //                // extract grantTypes
                //                var grantTypes = [];
                //                for (var gt of $scope.oauth2GrantTypes) {
                //                    if (gt.value) {
                //                        grantTypes.push(gt.key);
                //                    }
                //                }
                //                conf.authorizedGrantTypes = grantTypes;
                //
                //                var authMethods = [];
                //                for (var am of $scope.oauth2AuthenticationMethods) {
                //                    if (am.value) {
                //                        authMethods.push(am.key);
                //                    }
                //                }
                //                conf.authenticationMethods = authMethods;

                var redirectUris = $scope.oauth2RedirectUris.map(function (r) {
                    if ('text' in r) {
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
                if ('text' in s) {
                    return s.text;
                }
                return s;
            });

            var providers = $scope.identityProviders.map(function (idp) {
                if (idp.value === true) {
                    return idp.provider;
                }
            }).filter(function (idp) { return !!idp });

            //flow extensions hooks
            var hookFunctions = clientApp.hookFunctions != null ? clientApp.hookFunctions : {};
            if ($scope.claimMapping.enabled == true && $scope.claimMapping.code != null && $scope.claimMapping.code != "") {
                hookFunctions["claimMapping"] = btoa($scope.claimMapping.code);
            } else {
                delete hookFunctions["claimMapping"];
            }

            var hookWebUrls = clientApp.hookWebUrls != null ? clientApp.hookWebUrls : {};
            hookWebUrls["afterTokenGrant"] = $scope.webHooks["afterTokenGrant"];

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
                hookFunctions: hookFunctions,
                hookWebUrls: hookWebUrls,
                hookUniqueSpaces: clientApp.hookUniqueSpaces
            };

            RealmAppsData.saveClientApp($scope.realm.slug, data)
                .then(function (res) {
                    $scope.reload(res);
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
            RealmAppsData.resetClientAppCredentials($scope.realm.slug, $scope.modClientApp.clientId).then(function (res) {
                $scope.reload(res);
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
            RealmAppsData.removeClientApp($scope.realm.slug, $scope.modClientApp.clientId).then(function () {
                $state.go('realm.apps', { realmId: $stateParams.realmId });
            }).catch(function (err) {
                Utils.showError(err.data.message);
            });
        }


        $scope.exportClientApp = function (clientApp) {
            $window.open('console/dev/realms/' + clientApp.realm + '/apps/' + clientApp.clientId + '/export');
        };

        $scope.inspectDlg = function (obj) {
            $scope.modObj = obj;
            $scope.modObj.json = JSON.stringify(obj, null, 3);
            $('#inspectModal').modal({ keyboard: false });
        }

        $scope.copyText = function (txt) {
            var textField = document.createElement('textarea');
            textField.innerText = txt;
            document.body.appendChild(textField);
            textField.select();
            document.execCommand('copy');
            textField.remove();
        }

        var updateResources = function (scopes) {
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

        var updateIdps = function (providers) {
            var idps = [];

            for (var idp of $scope.identityProviders) {
                //inflate value
                idp.value = providers.includes(idp.provider)
                idps.push(idp);
            }

            $scope.identityProviders = idps;
        }

        $scope.updateClientAppScopeResource = function () {
            var resource = $scope.scopeResource;
            var scopesToRemove = resource.scopes
                .filter(function (s) {
                    return !s.value
                })
                .map(function (s) {
                    return s.scope;
                });
            var scopesToAdd = resource.scopes
                .filter(function (s) {
                    return s.value
                })
                .map(function (s) {
                    return s.scope;
                });

            var scopes = $scope.appScopes.map(function (s) {
                if ('text' in s) {
                    return s.text;
                }
                return s;
            }).filter(s => !scopesToRemove.includes(s));

            scopesToAdd.forEach(function (s) {
                if (!scopes.includes(s)) {
                    scopes.push(s);
                }
            });

            //inflate again
            var appScopes = [];
            scopes.forEach(function (s) {
                appScopes.push({ 'text': s });
            });

            $scope.appScopes = appScopes;
        }


        $scope.scopesDlg = function (resource) {
            var scopes = $scope.appScopes.map(function (s) {
                if ('text' in s) {
                    return s.text;
                }
                return s;
            });
            resource.scopes.forEach(function (s) {
                s.value = scopes.includes(s.scope);
            });

            $scope.scopeResource = resource;
            $('#scopesModal').modal({ backdrop: 'static', focus: true })
            Utils.refreshFormBS();
        }

        $scope.loadScopes = function (query) {
            var scopes = [];
            for (var res of $scope.resources) {
                res.scopes.forEach(function (s) {
                    if (s.scope.toLowerCase().includes(query.toLowerCase())) {
                        scopes.push({
                            text: s.scope
                        });
                    }
                });


            }
            return scopes;
        }

        /*
        * authorities
        */
        $scope.loadAuthorities = function () {
            RealmAppsData.getAuthorities(slug, clientId)
                .then(function (data) {
                    $scope.reloadAuthorities(data);
                })
                .catch(function (err) {
                    Utils.showError('Failed to load client authorities: ' + err.data.message);
                });
        }
        $scope.reloadAuthorities = function (data) {
            var authorities = data.filter(a => a.realm && slug == a.realm).map(auth => {
                var a = {
                    ...auth,
                    name: auth.role.replaceAll("_", " ").slice(5).toUpperCase(),
                    description: '',
                }

                if (a.role == 'ROLE_DEVELOPER') {
                    a.description = 'Manage realm applications and services';
                }
                if (a.role == 'ROLE_ADMIN') {
                    a.description = "Manage realm settings and configuration (in addition to developer permissions)";
                }

                return a;
            });

            $scope.authorities = authorities;

            //flatten for display
            $scope._authorities = authorities.map(a => a.role);

            //also update client model
            $scope.app.authorities = data;
        }

        $scope.manageAuthoritiesDlg = function () {
            var authorities = $scope.authorities;
            var roles = authorities.map(a => a.role);

            $scope.modAuthorities = {
                realm: slug,
                admin: roles.includes('ROLE_ADMIN'),
                developer: roles.includes('ROLE_DEVELOPER')
            };
            $('#authoritiesModal').modal({ keyboard: false });
        }

        $scope.updateAuthorities = function () {
            $('#authoritiesModal').modal('hide');
            if ($scope.modAuthorities) {
                var roles = $scope.modAuthorities;
                var authorities = [];


                if (roles.admin === true) {
                    authorities.push({
                        realm: slug,
                        role: 'ROLE_ADMIN'
                    });
                }
                if (roles.developer === true) {
                    authorities.push({
                        realm: slug,
                        role: 'ROLE_DEVELOPER'
                    });
                }


                RealmAppsData.updateAuthorities(slug, clientId, authorities)
                    .then(function (data) {
                        $scope.reloadAuthorities(data);
                        Utils.showSuccess();
                    })
                    .catch(function (err) {
                        Utils.showError('Failed to update authorities: ' + err.data.message);
                    });

            }
        }

        /*
       * realm roles
       */

        $scope.loadRoles = function () {
            RealmAppsData.getRoles(slug, clientId)
                .then(function (data) {
                    $scope.reloadRoles(data);
                })
                .catch(function (err) {
                    Utils.showError('Failed to load client roles: ' + err.data.message);
                });
        }

        $scope.reloadRoles = function (data) {
            var realmRoles = $scope.realmRoles;
            var roles = data.map(r => {

                if (!realmRoles.has(r.role)) {
                    return null;
                }

                var { role, name, description, roleId } = realmRoles.get(r.role);
                return {
                    ...r,
                    role, name, description, roleId
                }
            })
            $scope.roles = roles;

            //flatten for display
            $scope._roles = roles.map(r => r.role);

            //also update app model
            $scope.app.roles = data;
        }


        $scope.removeRole = function (role) {
            if (role.realm && role.realm != slug) {
                Utils.showError('Failed to remove role');
                return;
            }

            updateRoles(null, [role]);
        }

        $scope.addRoleDlg = function () {
            $scope.modRole = {
                realm: slug,
                role: '',
                roles: Array.from($scope.realmRoles.values())
            };
            $('#rolesModal').modal({ keyboard: false });
            Utils.refreshFormBS(300);
        }

        $scope.addRole = function () {
            $('#rolesModal').modal('hide');
            if ($scope.modRole && $scope.modRole.role) {
                var role = $scope.modRole.role;

                updateRoles([role], null);
                $scope.modRole = null;
            }
        }

        // save roles
        var updateRoles = function (rolesAdd, rolesRemove) {
            //map cur realm
            var curRoles = $scope.roles
                .map(a => a.role);

            var realmRoles = Array.from($scope.realmRoles.values()).map(r => r.role);

            //handle only same realm
            var rolesToAdd = [];
            if (rolesAdd) {
                rolesToAdd = rolesAdd.map(r => {
                    if (r.role) {
                        return r.role;
                    }
                    return r;
                }).filter(r => !curRoles.includes(r));
            }


            if (rolesToAdd.some(r => !realmRoles.includes(r))) {
                Utils.showError('Invalid roles');
                return;
            }

            var rolesToRemove = [];
            if (rolesRemove) {
                rolesToRemove = rolesRemove.map(r => {
                    if (r.role) {
                        return r.role;
                    }
                    return r;
                }).filter(r => curRoles.includes(r));
            }

            var keepRoles = curRoles.filter(r => !rolesToRemove.includes(r));
            var roles = keepRoles.concat(rolesToAdd);

            var data = roles.map(r => {
                return { 'realm': slug, 'role': r }
            });


            RealmAppsData.updateRoles(slug, clientId, data)
                .then(function (data) {
                    $scope.reloadRoles(data);
                    return;
                })
                .then(function () {
                    return RealmAppsData.getApprovals(slug, clientId);
                })
                .then(function (data) {
                    $scope.reloadApprovals(data, $scope._roles);
                    Utils.showSuccess();
                })
                .catch(function (err) {
                    Utils.showError('Failed to update roles: ' + err.data.message);
                });
        }

        /*
        * space roles
        */
        $scope.loadSpaceRoles = function () {
            RealmAppsData.getSpaceRoles(slug, clientId)
                .then(function (data) {
                    $scope.reloadSpaceRoles(data);
                })
                .catch(function (err) {
                    Utils.showError('Failed to load user space roles: ' + err.data.message);
                });
        }

        $scope.reloadSpaceRoles = function (data) {
            var roles = data;
            if (roles) {
                roles.sort(function (a, b) {
                    var authA = a.authority.toUpperCase();
                    var authB = b.authority.toUpperCase();

                    if (authA < authB) {
                        return -1;
                    }
                    if (authA > authB) {
                        return 1;
                    }
                    return 0;
                })
            }
            $scope.spaceRoles = roles.map(r => {
                return {
                    ...r,
                    namespace: ((r.context ? r.context + '/' : '') + (r.space || ''))
                }
            });

            //also update app model
            $scope.app.spaceRoles = data;
        }

        $scope.addSpaceRoleDlg = function () {
            $scope.modSpaceRole = {
                role: null,
                spaces: $scope.myspaces
            };
            $('#spaceRolesModal').modal({ keyboard: false });
            Utils.refreshFormBS(300);
        }

        $scope.addSpaceRole = function () {
            $('#spaceRolesModal').modal('hide');
            if ($scope.modSpaceRole && $scope.modSpaceRole.role && $scope.modSpaceRole.space) {
                var { space, role } = $scope.modSpaceRole;
                // workaround for root space
                if (space == '-- ROOT --') space = '';

                var authority = (space ? (space + ':') : '') + role.trim();

                updateSpaceRoles([authority], null);
                $scope.modSpaceRole = null;
            }
        }

        $scope.removeSpaceRole = function (authority) {
            updateSpaceRoles(null, [authority]);
        }


        var updateSpaceRoles = function (rolesAdd, rolesRemove) {
            var curRoles = $scope.spaceRoles.map(a => a.authority);

            var rolesToAdd = [];
            if (rolesAdd) {
                rolesToAdd = rolesAdd.filter(r => !curRoles.includes(r));
            }


            var rolesToRemove = [];
            if (rolesRemove) {
                rolesToRemove = rolesRemove.filter(r => curRoles.includes(r));
            }

            var keepRoles = curRoles.filter(r => !rolesToRemove.includes(r));
            var roles = keepRoles.concat(rolesToAdd);

            var data = roles;


            RealmAppsData.updateSpaceRoles(slug, clientId, data)
                .then(function (data) {
                    $scope.reloadSpaceRoles(data);
                    Utils.showSuccess();
                })
                .catch(function (err) {
                    Utils.showError('Failed to update roles: ' + err.data.message);
                });


        }


        /*
        * approvals
        */
        $scope.loadApprovals = function () {
            RealmAppsData.getApprovals(slug, clientId)
                .then(function (data) {
                    $scope.reloadApprovals(data, $scope._roles);
                })
                .catch(function (err) {
                    Utils.showError('Failed to load approvals: ' + err.data.message);
                });
        }


        $scope.reloadApprovals = function (data, rr) {
            var services = $scope.services;
            var realmRoles = $scope.realmRoles;

            var roleApprovals = [];
            Array.from(realmRoles.values()).filter(r => rr.includes(r.role)).forEach(r => {
                var { roleId, role, name, description } = r;
                r.approvals.forEach(a => {
                    roleApprovals.push({
                        roleId, role, name, description,
                        ...a
                    });
                })
            });


            var approvals = Array.from(services.values()).map(s => {
                var { serviceId, namespace, realm, name, description } = s;

                var ua = data.filter(a => a.userId == s.serviceId);
                var ra = roleApprovals.filter(a => a.userId == s.serviceId);
                return {
                    serviceId, namespace, realm, name, description,
                    approvals: ua.concat(ra)
                }
            });

            $scope.approvals = approvals;

            //flatten to permission for display
            var permissions = data.map(a => a.scope);
            $scope.permissions = permissions;


        }


        $scope.editPermissionsDlg = function (service) {
            var { serviceId, namespace, realm, name, description } = service;
            var scopes = $scope.services.get(serviceId).scopes;

            $scope.modApprovals = {
                serviceId, namespace, realm, name, description,
                scopes: scopes.filter(s => s.type == 'client' || s.type == 'generic').map(s => {
                    return {
                        ...s,
                        value: service.approvals.some(a => s.scope == a.scope),
                        locked: service.approvals.some(a => (s.scope == a.scope && a.role))

                    }
                }),
                clientId: clientId,

            }
            $('#permissionsModal').modal({ keyboard: false });
        }

        $scope.updatePermissions = function () {
            $('#permissionsModal').modal('hide');


            if ($scope.modApprovals) {

                //unpack
                var serviceId = $scope.modApprovals.serviceId;
                var service = $scope.services.get(serviceId);
                var approved = $scope.modApprovals.scopes.filter(s => (s.value && !s.locked)).map(s => s.scope);

                var approval = $scope.approvals.find(a => serviceId == a.serviceId)
                var approvals = approval.approvals.filter(a => !a.role);

                //build request
                var toRemove = approvals.filter(a => !approved.includes(a.scope));
                var toKeep = approvals.filter(a => approved.includes(a.scope)).map(s => s.scope);

                var toAdd = approved.filter(a => !toKeep.includes(a)).map(s => {
                    return {
                        userId: serviceId,
                        clientId: clientId,
                        scope: s
                    }
                });


                var updates = toRemove.concat(toAdd);

                Promise.all(
                    updates
                        .map(a => {
                            if (a.status) {
                                return RealmServices.deleteApproval(service.realm, serviceId, a);
                            } else {
                                return RealmServices.addApproval(service.realm, serviceId, a);
                            }
                        })
                )
                    .then(function () {
                        Utils.showSuccess();
                        $scope.loadApprovals();
                    })
                    .catch(function (err) {
                        Utils.showError('Failed to load realm role: ' + err.data.message);
                    });

            }
        }


        $scope.testOAuth2ClientApp = function (grantType) {

            if (!$scope.app.type == 'oauth2') {
                Utils.showError("invalid app type");
                return;
            }

            RealmAppsData.testOAuth2ClientApp($scope.realm.slug, $scope.app.clientId, grantType).then(function (token) {
                $scope.oauth2Tokens[grantType].token = token.access_token;
                $scope.oauth2Tokens[grantType].decoded = null;
            }).catch(function (err) {
                Utils.showError(err.data.message);
            });
        }

        $scope.decodeJwt = function (grantType) {
            var token = $scope.oauth2Tokens[grantType].token;
            var d = '';
            try {
                d = JSON.parse(atob(token.split('.')[1]));
            } catch (e) {
                d = '';
            }

            $scope.oauth2Tokens[grantType].decoded = d;
        }

        $scope.toggleClientAppClaimMapping = function () {
            var claimMapping = $scope.claimMapping;

            if (claimMapping.enabled && claimMapping.code == '') {
                claimMapping.code =
                    '/**\n * DEFINE YOUR OWN CLAIM MAPPING HERE\n' +
                    '**/\n' +
                    'function claimMapping(claims) {\n   return claims;\n}';
            }

            claimMapping.error = null;
            claimMapping.result = null;
            claimMapping.context = '{}';

            $scope.claimMapping = claimMapping;

        }

        $scope.testClientAppClaimMapping = function () {
            var functionCode = $scope.claimMapping.code;

            if (functionCode == '') {
                Utils.showError("empty function code");
                return;
            }

            var data = {
                code: btoa(functionCode),
                name: 'claimMapping',
                scopes: $scope.claimMapping.scopes.map(function (s) { return s.text })
            }

            RealmAppsData.testClientAppClaimMapping($scope.realm.slug, $scope.app.clientId, data)
                .then(function (res) {
                    $scope.claimMapping.result = res.result;
                    $scope.claimMapping.errors = res.errors;
                    $scope.claimMapping.context = (res.context ? JSON.stringify(res.context, null, 4) : '{}');
                }).catch(function (err) {
                    $scope.claimMapping.result = null;
                    $scope.claimMapping.context = (err.context ? JSON.stringify(err.context, null, 4) : '{}');
                    $scope.claimMapping.errors = [err.data.message];
                });
        }

        $scope.toggleIdp = function (idp) {
            if (idp) {
                idp.value = idp.value ? false : true;
            }
        }

        var iconProvider = function (clientApp) {
            var icon = './italia/svg/sprite.svg#it-piattaforme';
            if (clientApp.type == 'oauth2') {
                if (clientApp.configuration.applicationType == 'web') {
                    icon = './italia/svg/sprite.svg#it-star-outline';
                }
                if (clientApp.configuration.applicationType == 'native') {
                    icon = './italia/svg/sprite.svg#it-card';
                }
                if (clientApp.configuration.applicationType == 'machine') {
                    icon = './italia/svg/sprite.svg#it-software';
                }
                if (clientApp.configuration.applicationType == 'spa') {
                    icon = './italia/svg/sprite.svg#it-presentation';
                }
            }

            return icon;
        }


        var idpIconProvider = function (idp) {
            var icons = ['facebook', 'google', 'microsoft', 'apple', 'instagram', 'github'];

            if (idp.authority === "oidc" && 'clientName' in idp.configuration) {
                var logo = null;
                if (icons.includes(idp.configuration.clientName.toLowerCase())) {
                    logo = idp.configuration.clientName.toLowerCase();
                } else if (icons.includes(idp.name.toLowerCase())) {
                    logo = idp.name.toLowerCase();
                }

                if (logo) {
                    return './svg/sprite.svg#logo-' + logo;
                }
            }
            if (idp.authority === "spid") {
                return './spid/sprite.svg#spid-ico-circle-bb';
            }
            return './italia/svg/sprite.svg#it-unlocked';
        }


        init();
    })
    .controller('RealmAppStartController', function ($scope, $stateParams, RealmAppsData, Utils) {
        var slug = $stateParams.realmId;
        var clientId = $stateParams.clientId;
        $scope.clientView = 'overview';

        $scope.aceOption = {
            mode: 'javascript',
            theme: 'monokai',
            showGutter: false,
            maxLines: 30,
            minLines: 6
        };


        /**
       * Initialize the app: load list of apps
       */
        var init = function () {
            RealmAppsData.getOAuth2Metadata(slug, clientId)
                .then(function (metadata) {
                    $scope.oauth2Metadata = metadata;
                })
                .then(function () {
                    return RealmAppsData.getClientApp(slug, clientId);
                })
                .then(function (data) {
                    $scope.load(data);
                    return data;
                })
                .then(function (data) {
                    var quickstarters = [];
                    if (data.type == 'oauth2') {


                        var oauth2Tokens = {};

                        $scope.oauth2GrantTypes.forEach(function (gt) {
                            quickstarters.push(
                                {
                                    'key': 'oauth2.' + gt.key,
                                    'name': gt.key
                                }
                            );
                            if ("authorization_code" == gt.key) {
                                quickstarters.push(
                                    {
                                        'key': 'oauth2.authorization_code_pkce',
                                        'name': 'authorization_code_pkce'
                                    }
                                );
                            }

                            if ("authorization_code" == gt.key || "implicit" == gt.key || "client_credentials" == gt.key) {
                                oauth2Tokens[gt.key] = {
                                    token: null
                                }
                            }
                        });

                        $scope.oauth2Tokens = oauth2Tokens;

                        $scope.oauth2AccessToken = "ACCESS_TOKEN";
                        $scope.oauth2RefreshToken = "REFRESH_TOKEN";
                        $scope.oauth2IdToken = "ID_TOKEN";
                        $scope.oauth2Username = "USERNAME";
                        $scope.oauth2Password = "PASSWORD";
                        $scope.oauth2TestEndpoint = "ENDPOINT";
                        $scope.oauth2RedirectUrl = "REDIRECT_URL";
                        $scope.oauth2State = Math.random().toString(36).substr(2, 5);
                        $scope.oauth2Nonce = Math.random().toString(36).substr(2, 5);
                        $scope.oauth2Scopes = data.scopes.join(' ');
                        $scope.oauth2Code = 'CODE';
                        $scope.oauth2PKCEVerifier = 'Ux9otAaZ0NwI7nYsWrE-DTgDA4AecMOf3bn9bNNQnmk';
                        $scope.oauth2PKCEChallenge = '-uE-LdYx2fzfr9CuTZ9LO-Xe5ZkugIvlYQdrNT9kKXY';


                    }
                    $scope.quickstart = {
                        'name': '',
                        'view': ''
                    };
                    $scope.quickstarters = quickstarters;

                    return data;
                })
                .then(function () {
                    //$scope.clientView = 'quickstart.' + data.type;
                    $scope.reloadQuickstart();
                })
                .catch(function (err) {
                    Utils.showError('Failed to load realm client app: ' + err.data.message);
                });


        };

        $scope.load = function (data) {
            //set
            data.icon = iconProvider(data);
            $scope.app = data;
            $scope.appname = data.name;


            // process scopes scopes
            var scopes = [];
            if (data.scopes) {
                data.scopes.forEach(function (s) {
                    scopes.push({ 'text': s });
                });
            }
            $scope.appScopes = scopes;

            if (data.type == 'oauth2') {
                initConfiguration(data.type, data.configuration);
                //auth header
                $scope.oauth2AuthHeader = btoa(data.clientId + ':' + data.configuration.clientSecret);
            }

            return;
        }

        var initConfiguration = function (type, config) {

            if (type === 'oauth2') {
                // grantTypes
                var grantTypes = [];
                if (config.authorizedGrantTypes) {
                    config.authorizedGrantTypes.forEach(function (gt) {
                        if (gt) {
                            grantTypes.push({
                                "key": gt,
                                "value": true
                            })
                        }
                    });
                }

                $scope.oauth2GrantTypes = grantTypes;

                // authMethods
                $scope.oauth2AuthenticationMethods = config.authenticationMethods;

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




        $scope.activeClientView = function (view) {
            return view == $scope.clientView ? 'active' : '';
        };

        $scope.switchClientView = function (view) {
            $scope.clientView = view;
            Utils.refreshFormBS(300);
        }

        $scope.reloadQuickstart = function () {
            if ($scope.quickstart.view) {
                $scope.quickstartView = $scope.quickstart.view;
            } else {
                $scope.quickstartView = null;
            }
            $scope.curStep = 1;
        }

        $scope.prevStep = function () {
            $scope.curStep--;
        }
        $scope.nextStep = function () {
            $scope.curStep++;
        }
        $scope.setStep = function (s) {
            $scope.curStep = s;
        }

        $scope.copyText = function (txt) {
            var textField = document.createElement('textarea');
            textField.innerText = txt;
            document.body.appendChild(textField);
            textField.select();
            document.execCommand('copy');
            textField.remove();
        }

        $scope.testOAuth2ClientApp = function (grantType) {

            if (!$scope.app.type == 'oauth2') {
                Utils.showError("invalid app type");
                return;
            }

            RealmAppsData.testOAuth2ClientApp($scope.realm.slug, $scope.app.clientId, grantType).then(function (token) {
                $scope.oauth2Tokens[grantType].token = token.access_token;
                $scope.oauth2Tokens[grantType].decoded = null;
            }).catch(function (err) {
                Utils.showError(err.data.message);
            });
        }


        $scope.decodeJwt = function (grantType) {
            var token = $scope.oauth2Tokens[grantType].token;
            var d = '';
            try {
                d = JSON.parse(atob(token.split('.')[1]));
            } catch (e) {
                d = '';
            }

            $scope.oauth2Tokens[grantType].decoded = d;
        }

        var iconProvider = function (clientApp) {
            var icon = './italia/svg/sprite.svg#it-piattaforme';
            if (clientApp.type == 'oauth2') {
                if (clientApp.configuration.applicationType == 'web') {
                    icon = './italia/svg/sprite.svg#it-star-outline';
                }
                if (clientApp.configuration.applicationType == 'native') {
                    icon = './italia/svg/sprite.svg#it-card';
                }
                if (clientApp.configuration.applicationType == 'machine') {
                    icon = './italia/svg/sprite.svg#it-software';
                }
                if (clientApp.configuration.applicationType == 'spa') {
                    icon = './italia/svg/sprite.svg#it-presentation';
                }
            }

            return icon;
        }

        init();
    })
    ;