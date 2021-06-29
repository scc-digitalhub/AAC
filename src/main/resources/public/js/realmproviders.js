angular.module('aac.controllers.realmproviders', [])
    /**
      * Realm Data Services
      */
    .service('RealmProviders', function ($q, $http, $httpParamSerializer) {
        var rService = {};
        rService.getIdentityProvider = function (slug, providerId) {
            return $http.get('console/dev/realms/' + slug + '/providers/' + providerId).then(function (data) {
                return data.data;
            });
        }

        rService.getIdentityProviders = function (slug) {
            return $http.get('console/dev/realms/' + slug + '/providers').then(function (data) {
                return data.data;
            });
        }

        rService.getIdentityProviderTemplates = function (slug) {
            return $http.get('console/dev/realms/' + slug + '/providertemplates').then(function (data) {
                return data.data;
            });
        }

        rService.removeIdentityProvider = function (slug, providerId) {
            return $http.delete('console/dev/realms/' + slug + '/providers/' + providerId).then(function (data) {
                return data.data;
            });
        }

        rService.saveIdentityProvider = function (slug, provider) {
            if (provider.provider) {
                return $http.put('console/dev/realms/' + slug + '/providers/' + provider.provider, provider).then(function (data) {
                    return data.data;
                });
            } else {
                return $http.post('console/dev/realms/' + slug + '/providers', provider).then(function (data) {
                    return data.data;
                });
            }
        }

        rService.changeIdentityProviderState = function (slug, providerId, provider) {
            return $http.put('console/dev/realms/' + slug + '/providers/' + providerId + '/state', provider).then(function (data) {
                return data.data;
            });
        }

        rService.importIdentityProvider = function (slug, file) {
            var fd = new FormData();
            fd.append('file', file);
            return $http({
                url: 'console/dev/realms/' + slug + '/providers',
                headers: { "Content-Type": undefined }, //set undefined to let $http manage multipart declaration with proper boundaries
                data: fd,
                method: "PUT"
            }).then(function (data) {
                return data.data;
            });

        }

        return rService;

    })
    /**
      * Realm providers controller
      */
    .controller('RealmProvidersController', function ($scope, $state, $stateParams, RealmData, RealmProviders, Utils) {
        var slug = $stateParams.realmId;

        $scope.load = function () {
            RealmProviders.getIdentityProviders(slug)
                .then(function (data) {
                    $scope.providers = data;
                    return data;
                })
                .catch(function (err) {
                    Utils.showError('Failed to load realm providers: ' + err.data.message);
                });
        }

        /**
         * Initialize the app: load list of the providers
         */
        var init = function () {
            RealmProviders.getIdentityProviderTemplates(slug)
                .then(function (data) {
                    $scope.providerTemplates = data.filter(function (pt) { return pt.authority != 'internal' });
                })
            $scope.load();
        };

        $scope.deleteProviderDlg = function (provider) {
            $scope.modProvider = provider;
            //add confirm field
            $scope.modProvider.confirmId = '';
            $('#deleteProviderConfirm').modal({ keyboard: false });
        }

        $scope.deleteProvider = function () {
            $('#deleteProviderConfirm').modal('hide');
            if ($scope.modProvider.provider === $scope.modProvider.confirmId) {
                RealmProviders.removeIdentityProvider($scope.realm.slug, $scope.modProvider.provider).then(function () {
                    $scope.load();
                    Utils.showSuccess();
                }).catch(function (err) {
                    Utils.showError(err.data.message);
                });
            } else {
                Utils.showError("confirmId not valid");
            }
        }

        $scope.editProviderDlg = function (provider) {
            if (provider.authority == 'internal') {
                $scope.internalProviderDlg(provider);
            } else if (provider.authority == 'oidc') {
                $scope.oidcProviderDlg(provider);
            } else if (provider.authority == 'saml') {
                $scope.samlProviderDlg(provider);
            }
        }

        $scope.createProviderDlg = function (authority, name) {
            provider = {
                type: 'identity',
                name: '',
                authority: authority,
                realm: slug,
                configuration: {}
            };
            if (name) {
                var ptIdx = $scope.providerTemplates.findIndex(function (pt) { return pt.name === name });
                if (ptIdx >= 0) {
                    var template = $scope.providerTemplates[ptIdx];
                    provider.name = name;
                    provider.configuration = Object.assign({}, template.configuration);
                }
            }

            $scope.modProvider = provider;

            $('#createProviderDlg').modal({ keyboard: false });
            Utils.refreshFormBS();
        }

        $scope.createProvider = function () {
            $('#createProviderDlg').modal('hide');
            var data = $scope.modProvider
            // HOOK: OIDC contains scopes to be converted to string
            if (data.authority === 'oidc' && data.scope) {
                data.scope = data.scope.map(function (s) { return s.text }).join(',');
            }

            data.realm = slug;

            RealmProviders.saveIdentityProvider(slug, data)
                .then(function () {
                    $scope.load();
                    Utils.showSuccess();
                })
                .catch(function (err) {
                    Utils.showError(err.data.message);
                });


        }


        var toChips = function (str) {
            return str.split(',').map(function (e) { return e.trim() }).filter(function (e) { return !!e });
        }

        $scope.oidcProviderDlg = function (provider) {
            $scope.providerId = provider ? provider.provider : null;
            $scope.providerAuthority = 'oidc';
            $scope.provider = provider ? Object.assign({}, provider.configuration) :
                { clientAuthenticationMethod: 'client_secret_basic', scope: 'openid,profile,email', userNameAttributeName: 'sub' };
            $scope.provider.name = provider ? provider.name : null;
            $scope.provider.clientName = $scope.provider.clientName || '';
            $scope.provider.scope = toChips($scope.provider.scope);
            $scope.provider.persistence = provider ? provider.persistence : 'none';

            $scope.oidcProviderTemplates = $scope.providerTemplates ? $scope.providerTemplates.filter(function (pt) { return pt.authority === 'oidc' }) : [];

            $('#oidcModal').modal({ backdrop: 'static', focus: true })
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

            var data = { realm: $scope.realm.slug, name: name, persistence: persistence, configuration: $scope.provider, authority: $scope.providerAuthority, type: 'identity', provider: $scope.providerId };
            RealmProviders.saveIdentityProvider($scope.realm.slug, data)
                .then(function () {
                    $scope.load();
                    Utils.showSuccess();
                })
                .catch(function (err) {
                    Utils.showError(err.data.message);
                });
        }

        //        $scope.toggleProviderState = function (item) {
        //            RealmProviders.changeIdentityProviderState($scope.realm.slug, item.provider, item)
        //                .then(function () {
        //                    Utils.showSuccess();
        //                })
        //                .catch(function (err) {
        //                    Utils.showError(err.data.message);
        //                });
        //
        //        }

        $scope.toggleProviderState = function (provider) {

            RealmProviders.changeIdentityProviderState($scope.realm.slug, provider.provider, provider)
                .then(function (res) {
                    provider.enabled = res.enabled;
                    provider.registered = res.registered;
                    Utils.showSuccess();
                })
                .catch(function (err) {
                    Utils.showError(err.data.message);
                });

        }

        $scope.toggleProviderState = function (provider, state) {

            provider.enabled = state;

            RealmProviders.changeIdentityProviderState($scope.realm.slug, provider.provider, provider)
                .then(function (res) {
                    provider.enabled = res.enabled;
                    provider.registered = res.registered;
                    Utils.showSuccess();
                })
                .catch(function (err) {
                    Utils.showError(err.data.message);
                });

        }

        $scope.updateProviderType = function () {
            if ($scope.provider.clientName) {
                var ptIdx = $scope.oidcProviderTemplates.findIndex(function (pt) { return pt.name === $scope.provider.clientName });
                if (ptIdx >= 0) {
                    var pt = Object.assign({}, $scope.oidcProviderTemplates[ptIdx].configuration);
                    pt.name = $scope.provider.name;
                    pt.clientId = $scope.provider.clientId;
                    pt.clientSecret = $scope.provider.clientSecret;
                    pt.scope = toChips(pt.scope);
                    $scope.provider = pt;
                }
            }
            Utils.refreshFormBS();
        }

        $scope.updatePersistenceType = function () {
            Utils.refreshFormBS();
        }


        $scope.importProviderDlg = function () {
            $('#importProviderDlg').modal({ keyboard: false });
        }


        $scope.importProvider = function () {
            $('#importProviderDlg').modal('hide');
            var file = $scope.importFile;
            var mimeTypes = ['text/yaml', 'text/yml', 'application/x-yaml'];
            if (file == null || !mimeTypes.includes(file.type) || file.size == 0) {
                Utils.showError("invalid file");
            } else {
                RealmProviders.importIdentityProvider($scope.realm.slug, file)
                    .then(function (res) {
                        $scope.importFile = null;
                        $state.go('realm.provider', { realmId: res.realm, providerId: res.provider });
                        Utils.showSuccess();
                    })
                    .catch(function (err) {
                        Utils.showError(err.data.message);
                    });
            }
        }

        init();
    })
    .controller('RealmProviderController', function ($scope, $state, $stateParams, RealmData, RealmProviders, Utils) {
        var slug = $stateParams.realmId;
        var providerId = $stateParams.providerId;
        $scope.formView = 'overview';

        $scope.aceOption = {
            mode: 'javascript',
            theme: 'monokai',
            maxLines: 30,
            minLines: 6
        };


        $scope.activeView = function (view) {
            return view == $scope.formView ? 'active' : '';
        };

        $scope.switchView = function (view) {
            $scope.formView = view;
            Utils.refreshFormBS(300);
        }


        var init = function () {
            RealmProviders.getIdentityProviderTemplates(slug)
                .then(function (data) {
                    $scope.providerTemplates = data;

                    //extract 
                    $scope.oidcProviderTemplates = $scope.providerTemplates ? $scope.providerTemplates.filter(function (pt) { return pt.authority === 'oidc' }) : [];
                    $scope.internalProviderTemplates = $scope.providerTemplates ? $scope.providerTemplates.filter(function (pt) { return pt.authority === 'internal' }) : [];
                    $scope.samlProviderTemplates = $scope.providerTemplates ? $scope.providerTemplates.filter(function (pt) { return pt.authority === 'saml' }) : [];

                })
                .then(function () {
                    return RealmProviders.getIdentityProvider(slug, providerId)
                })
                .then(function (data) {
                    $scope.load(data);
                    $scope.formView = 'overview';
                    return data;
                })
                .catch(function (err) {
                    Utils.showError('Failed to load provider : ' + err.data.message);
                });
        };

        var initConfiguration = function (authority, config) {
            if (authority == 'oidc') {
                var scopes = [];
                toChips(config.scope).forEach(function (s) {
                    scopes.push({ 'text': s });
                });
                $scope.idpOidcScope = scopes;
            }

            if (authority == 'saml') {
                var authnContextClasses = [];
                if (config.authnContextClasses) {
                    config.authnContextClasses.forEach(function (s) {
                        authnContextClasses.push({ 'text': s });
                    });
                }

                $scope.samlAuthnContextClasses = authnContextClasses;
            }
        }

        var extractConfiguration = function (authority, config) {
            if (authority == 'oidc') {
                var scopes = $scope.idpOidcScope.map(function (s) {
                    if (s.hasOwnProperty('text')) {
                        return s.text;
                    }
                    return s;
                });
                config.scope = scopes.join(',');
            }
            if (authority == 'saml') {
                var authnContextClasses = $scope.samlAuthnContextClasses.map(function (s) {
                    if (s.hasOwnProperty('text')) {
                        return s.text;
                    }
                    return s;
                });
                if (authnContextClasses && authnContextClasses.length) {
                    config.authnContextClasses = authnContextClasses;
                } else {
                    config.authnContextClasses = null;
                }

            }

            return config;
        };

        var toChips = function (str) {
            return str.split(',').map(function (e) { return e.trim() }).filter(function (e) { return !!e });
        }

        $scope.load = function (data) {
            $scope.providerName = data.name != '' ? data.name : data.provider;
            $scope.idp = data;

            initConfiguration(data.authority, data.configuration);

            var attributeMapping = {
                enabled: false,
                code: "",
                result: null,
                error: null
            };
            if (data.hookFunctions.hasOwnProperty("attributeMapping")) {
                attributeMapping.enabled = true;
                attributeMapping.code = atob(data.hookFunctions["attributeMapping"]);
            }

            $scope.attributeMapping = attributeMapping;
        };


        $scope.deleteProviderDlg = function (provider) {
            $scope.modProvider = provider;
            $('#deleteProviderConfirm').modal({ keyboard: false });
        }

        $scope.deleteProvider = function () {
            $('#deleteProviderConfirm').modal('hide');
            RealmProviders.removeIdentityProvider($scope.realm.slug, $scope.modProvider.provider).then(function () {
                $state.go('realm.providers', { realmId: $scope.realm.slug });
            }).catch(function (err) {
                Utils.showError(err.data.message);
            });
        }

        $scope.saveProvider = function (provider) {

            var configuration = extractConfiguration(provider.authority, provider.configuration);

            var hookFunctions = provider.hookFunctions;
            if ($scope.attributeMapping.code != "") {
                hookFunctions["attributeMapping"] = btoa($scope.attributeMapping.code);
            } else {
                delete hookFunctions["attributeMapping"];
            }

            var data = {
                realm: provider.realm,
                provider: provider.provider,
                type: provider.type,
                authority: provider.authority,
                name: provider.name,
                description: provider.description,
                enabled: provider.enabled,
                persistence: provider.persistence,
                events: provider.events,
                configuration: configuration,
                hookFunctions: hookFunctions
            }

            RealmProviders.saveIdentityProvider($scope.realm.slug, data)
                .then(function (res) {
                    $scope.load(res);
                    Utils.showSuccess();
                })
                .catch(function (err) {
                    Utils.showError(err.data.message);
                });
        }

        $scope.toggleProviderState = function (provider) {
            provider.enabled = !provider.enabled;

            RealmProviders.changeIdentityProviderState($scope.realm.slug, provider.provider, provider)
                .then(function (res) {
                    provider.enabled = res.enabled;
                    provider.registered = res.registered;
                    Utils.showSuccess();
                })
                .catch(function (err) {
                    Utils.showError(err.data.message);
                });

        }

        $scope.exportProvider = function (provider) {
            window.open('console/dev/realms/' + provider.realm + '/providers/' + provider.provider + '/export');
        };

        $scope.applyProviderTemplate = function () {
            var configuration = $scope.idp.configuration;
            if (configuration.clientName) {
                var ptIdx = $scope.oidcProviderTemplates.findIndex(function (pt) { return pt.name === configuration.clientName });
                if (ptIdx >= 0) {
                    var pt = Object.assign({}, $scope.oidcProviderTemplates[ptIdx].configuration);
                    ptScopes = [];
                    toChips(pt.scope).forEach(function (s) {
                        ptScopes.push({ 'text': s });
                    });
                    $scope.idpOidcScope = ptScopes;
                    pt.clientId = configuration.clientId;
                    pt.clientSecret = configuration.clientSecret;
                    $scope.idp.configuration = pt;
                }
            }
            Utils.refreshFormBS();
        }

        $scope.toggleProviderAttributeMapping = function () {
            var attributeMapping = $scope.attributeMapping;

            if (attributeMapping.enabled && attributeMapping.code == '') {
                attributeMapping.code =
                    '/**\n * DEFINE YOUR OWN ATTRIBUTE MAPPING HERE\n' +
                    '**/\n' +
                    'function attributeMapping(attributes) {\n   return attributes;\n}';
            }

            attributeMapping.error = null;
            attributeMapping.result = null;

            $scope.attributeMapping = attributeMapping;

        }

        init();
    })

    ;