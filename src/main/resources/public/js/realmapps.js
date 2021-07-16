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
    .service('RealmAppsData', function ($q, $http) {
        var raService = {};

        raService.getClientApps = function (slug) {
            return $http.get('console/dev/realms/' + slug + '/apps').then(function (data) {
                return data.data;
            });
        }

        raService.getClientApp = function (slug, clientId) {
            return $http.get('console/dev/realms/' + slug + '/apps/' + clientId).then(function (data) {
                return data.data;
            });
        }

        raService.removeClientApp = function (slug, clientId) {
            return $http.delete('console/dev/realms/' + slug + '/apps/' + clientId).then(function (data) {
                return data.data;
            });
        }

        raService.resetClientAppCredentials = function (slug, clientId) {
            return $http.delete('console/dev/realms/' + slug + '/apps/' + clientId + '/credentials').then(function (data) {
                return data.data;
            });
        }


        raService.saveClientApp = function (slug, clientApp) {
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

        raService.importClientApp = function (slug, file) {
            var fd = new FormData();
            fd.append('file', file);
            return $http({
                url: 'console/dev/realms/' + slug + '/apps',
                headers: { "Content-Type": undefined }, //set undefined to let $http manage multipart declaration with proper boundaries
                data: fd,
                method: "PUT"
            }).then(function (data) {
                return data.data;
            });

        }

        raService.testOAuth2ClientApp = function (slug, clientId, grantType) {
            return $http.get('console/dev/realms/' + slug + '/apps/' + clientId + "/oauth2/" + grantType).then(function (data) {
                return data.data;
            });
        }

        raService.testClientAppClaimMapping = function (slug, clientId, functionCode) {
            return $http.post('console/dev/realms/' + slug + '/apps/' + clientId + "/claims", functionCode).then(function (data) {
                return data.data;
            });
        }

        raService.getOAuth2Metadata = function (slug, clientId) {
            return $http.get('console/dev/realms/' + slug + '/well-known/oauth2').then(function (data) {
                return data.data;
            });
        }

        return raService;

    })

    /**
   * Realm client controller
   */
    .controller('RealmAppsController', function ($scope, $stateParams, $state, RealmData, RealmAppsData, Utils) {
        var slug = $stateParams.realmId;

        $scope.load = function () {
            RealmAppsData.getClientApps(slug)
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
                type: 'oauth2',
                realm: slug
            };

            $('#createClientAppDlg').modal({ keyboard: false });
        }

        $scope.createClientApp = function () {
            $('#createClientAppDlg').modal('hide');


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
            var mimeTypes = ['text/yaml', 'text/yml', 'application/x-yaml'];
            if (file == null || !mimeTypes.includes(file.type) || file.size == 0) {
                Utils.showError("invalid file");
            } else {
                RealmAppsData.importClientApp($scope.realm.slug, file)
                    .then(function (res) {
                        $scope.importFile = null;
                        $state.go('realm.app', { realmId: res.realm, clientId: res.clientId });
                        Utils.showSuccess();
                    })
                    .catch(function (err) {
                        Utils.showError(err.data.message);
                    });
            }
        }


        init();
    })
    .controller('RealmAppController', function ($scope, $stateParams, $state, RealmData, RealmAppsData, RealmProviders, Utils, $window) {
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
            //we load provider resources only at first load since it's expensive
            RealmData.getResources(slug)
                .then(function (resources) {
                    $scope.resources = resources;
                    return resources;
                })
                .then(function () {
                    return RealmProviders.getIdentityProviders(slug)
                })
                .then(function (providers) {
                    return providers.filter(p => p.type === 'identity');
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
                    return RealmAppsData.getClientApp(slug, clientId);
                })
                .then(function (data) {
                    $scope.load(data);
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

                })
                .catch(function (err) {
                    Utils.showError('Failed to load realm client app: ' + err.data.message);
                });


        };

        $scope.load = function (data) {
            //set
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

            $scope.updateResources(data.scopes);
            $scope.updateIdps(data.providers);

            if (data.type == 'oauth2') {
                $scope.initConfiguration(data.type, data.configuration, data.schema);
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
                for (var h in data.hookWebUrls) {
                    if (h in webHooks) {
                        webHooks[h] = data.hookWebUrls[h];
                    }
                }
            }
            $scope.webHooks = webHooks;

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



        let extractConfiguration = function (type, config) {

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
            for (var h in $scope.webHooks) {
                if ($scope.webHooks[h] != null && $scope.webHooks[h] != '') {
                    hookWebUrls[h] = $scope.webHooks[h];
                } else {
                    delete hookWebUrls[h];
                }
            }


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
            RealmAppsData.resetClientAppCredentials($scope.realm.slug, $scope.modClientApp.clientId).then(function (res) {
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
            RealmAppsData.removeClientApp($scope.realm.slug, $scope.modClientApp.clientId).then(function () {
                $state.go('realm.apps', { realmId: $stateParams.realmId });
            }).catch(function (err) {
                Utils.showError(err.data.message);
            });
        }


        $scope.exportClientApp = function (clientApp) {
            $window.open('console/dev/realms/' + clientApp.realm + '/apps/' + clientApp.clientId + '/export');
        };

        $scope.activeClientView = function (view) {
            return view == $scope.clientView ? 'active' : '';
        };

        $scope.switchClientView = function (view) {
            $scope.clientView = view;
            Utils.refreshFormBS(300);
        }

        $scope.copyText = function (txt) {
            var textField = document.createElement('textarea');
            textField.innerText = txt;
            document.body.appendChild(textField);
            textField.select();
            document.execCommand('copy');
            textField.remove();
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


        init();
    })
    .controller('RealmAppStartController', function ($scope, $stateParams, $state, RealmAppsData, Utils) {
        var slug = $stateParams.realmId;
        var clientId = $stateParams.clientId;
        $scope.clientView = 'quickstart.oauth2';

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
                .then(function (data) {
                    $scope.clientView = 'quickstart.' + data.type;
                    $scope.reloadQuickstart();
                })
                .catch(function (err) {
                    Utils.showError('Failed to load realm client app: ' + err.data.message);
                });


        };

        $scope.load = function (data) {
            //set
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
                $scope.initConfiguration(data.type, data.configuration, data.schema);
                //auth header
                $scope.oauth2AuthHeader = btoa(data.clientId + ':' + data.configuration.clientSecret);
            }

            return;
        }

        $scope.initConfiguration = function (type, config, schema) {

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
            console.log("switch to " + view)
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
                oauth2Tokens
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

        init();
    })
    ;