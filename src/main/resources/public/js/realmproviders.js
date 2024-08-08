angular.module('aac.controllers.realmproviders', [])
    /**
      * Realm Data Services
      */
    .service('RealmProviders', function ($http, $httpParamSerializer, $window) {
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

        // authorities
        service.getIdentityProviderAuthorities = function (slug) {
            return $http.get('console/dev/idps/' + slug + '/authorities').then(function (data) {
                return data.data;
            });
        }

        // idps
        service.getIdentityProvider = function (slug, providerId) {
            return $http.get('console/dev/idps/' + slug + '/' + providerId).then(function (data) {
                return data.data;
            });
        }

        service.getIdentityProviders = function (slug) {
            return $http.get('console/dev/idps/' + slug+ '?page=0&size=200').then(function (data) {
                return data.data.content;
            });
        }

        service.searchIdentityProviders = function (slug, params) {
            return $http.get('console/dev/idps/' + slug + '/search?' + buildQuery(params)).then(function (data) {
                return data.data;
            });
        }

        // service.getIdentityProviderTemplates = function (slug) {
        //     return $http.get('console/dev/idptemplates/' + slug).then(function (data) {
        //         return data.data;
        //     });
        // }

        service.removeIdentityProvider = function (slug, providerId) {
            return $http.delete('console/dev/idps/' + slug + '/' + providerId).then(function (data) {
                return data.data;
            });
        }

        service.saveIdentityProvider = function (slug, provider) {
            if (provider.provider) {
                return $http.put('console/dev/idps/' + slug + '/' + provider.provider, provider).then(function (data) {
                    return data.data;
                });
            } else {
                return $http.post('console/dev/idps/' + slug, provider).then(function (data) {
                    return data.data;
                });
            }
        }

        service.changeIdentityProviderState = function (slug, providerId, provider) {
            if (provider.enabled) {
                return $http.put('console/dev/idps/' + slug + '/' + providerId + '/status', provider).then(function (data) {
                    return data.data;
                });
            } else {
                return $http.delete('console/dev/idps/' + slug + '/' + providerId + '/status', provider).then(function (data) {
                    return data.data;
                });
            }
        }

        service.getIdentityProviderConfig = function (slug, providerId) {
            return $http.get('console/dev/idps/' + slug + '/' + providerId + '/config').then(function (data) {
                return data.data;
            });
        }

        service.importIdentityProvider = function (slug, file, yaml, reset) {
            var fd = new FormData();
            if (yaml) {
                fd.append('yaml', yaml);
            }
            if (file) {
                fd.append('file', file);
            }
            return $http({
                url: 'console/dev/idps/' + slug + (reset ? "?reset=true" : ""),
                headers: { "Content-Type": undefined }, //set undefined to let $http manage multipart declaration with proper boundaries
                data: fd,
                method: "PUT"
            }).then(function (data) {
                return data.data;
            });
        }

        service.exportIdentityProvider = function (slug, providerId) {
            $window.open('console/dev/idps/' + slug + '/' + providerId + '/export');
        }

        service.changeIdentityProviderClientApp = function (slug, providerId, client) {
            return $http.put('console/dev/idps/' + slug + '/' + providerId + '/apps/' + client.clientId, client).then(function (data) {
                return data.data;
            });
        }

        service.testIdentityProviderClaimMapping = function (slug, providerId, functionCode) {
            return $http.post('console/dev/idps/' + slug + '/' + providerId + "/claims", functionCode).then(function (data) {
                return data.data;
            });
        }

        service.testIdentityProviderAuthFunction = function (slug, providerId, functionCode) {
            return $http.post('console/dev/idps/' + slug + '/' + providerId + "/authz", functionCode).then(function (data) {
                return data.data;
            });
        }


        //aps
        service.getAttributeProvider = function (slug, providerId) {
            return $http.get('console/dev/aps/' + slug + '/' + providerId).then(function (data) {
                return data.data;
            });
        }

        service.getAttributeProviders = function (slug) {
            return $http.get('console/dev/aps/' + slug).then(function (data) {
                return data.data;
            });
        }

        service.searchAttributeProviders = function (slug, params) {
            return $http.get('console/dev/aps/' + slug + '/search?' + buildQuery(params)).then(function (data) {
                return data.data;
            });
        }

        service.removeAttributeProvider = function (slug, providerId) {
            return $http.delete('console/dev/aps/' + slug + '/' + providerId).then(function (data) {
                return data.data;
            });
        }

        service.saveAttributeProvider = function (slug, provider) {
            if (provider.provider) {
                return $http.put('console/dev/aps/' + slug + '/' + provider.provider, provider).then(function (data) {
                    return data.data;
                });
            } else {
                return $http.post('console/dev/aps/' + slug, provider).then(function (data) {
                    return data.data;
                });
            }
        }

        service.changeAttributeProviderState = function (slug, providerId, provider) {
            if (provider.enabled) {
                return $http.put('console/dev/aps/' + slug + '/' + providerId + '/status', provider).then(function (data) {
                    return data.data;
                });
            } else {
                return $http.delete('console/dev/aps/' + slug + '/' + providerId + '/status', provider).then(function (data) {
                    return data.data;
                });
            }
        }

        service.importAttributeProvider = function (slug, file, yaml, reset) {
            var fd = new FormData();
            if (yaml) {
                fd.append('yaml', yaml);
            }
            if (file) {
                fd.append('file', file);
            }
            return $http({
                url: 'console/dev/aps/' + slug + (reset ? "?reset=true" : ""),
                headers: { "Content-Type": undefined }, //set undefined to let $http manage multipart declaration with proper boundaries
                data: fd,
                method: "PUT"
            }).then(function (data) {
                return data.data;
            });
        }

        service.exportAttributeProvider = function (slug, providerId) {
            $window.open('console/dev/aps/' + slug + '/' + providerId + '/export');
        }

        service.testAttributeProvider = function (slug, providerId) {
            return $http.get('console/dev/aps/' + slug + '/' + providerId + '/test').then(function (data) {
                return data.data;
            });
        }

        return service;

    })
    /**
      * Realm identity providers controller
      */
    .controller('RealmIdentityProvidersController', function ($scope, $state, $stateParams, $window, RealmData, RealmProviders, RealmAppsData, Utils) {
        var slug = $stateParams.realmId;
        $scope.query = {
            page: 0,
            size: 20,
            sort: { name: 1 },
            q: ''
        }
        $scope.keywords = '';

        $scope.load = function () {
            RealmProviders.searchIdentityProviders(slug, $scope.query)
                .then(function (data) {
                    data.content.forEach(function (idp) {
                        //add icon
                        idp.icon = iconProvider(idp);
                    });
                    $scope.providers = data;
                })
                .then(function () {
                    return RealmAppsData.getClientApps(slug);
                })
                .then(function (data) {
                    $scope.apps = data;
                    var providers = $scope.providers.content;
                    //count num of active apps per provider
                    var cc = new Map(providers.map(p => [p.provider, 0]));
                    data.forEach(function (app) {
                        app.providers.forEach(function (p) {
                            var c = cc.get(p);
                            cc.set(p, c + 1);
                        });
                    });
                    providers.forEach(function (idp) {
                        idp.apps = cc.get(idp.provider);
                    });
                    $scope.providers.content = providers;

                })
                .catch(function (err) {
                    Utils.showError('Failed to load realm providers: ' + err.data.message);
                });
        }

        /**
         * Initialize the app: load list of the providers
         */
        var init = function () {
            RealmProviders.getIdentityProviderAuthorities(slug)
                .then(function (data) {
                    $scope.authorities = data;
                    $scope.load();
                })
        };

        $scope.setPage = function (page) {
            $scope.query.page = page;
            $scope.load();
        }

        $scope.setQuery = function (query) {
            $scope.query.q = query;
            $scope.query.page = 0;
            $scope.load();
        }

        $scope.runQuery = function () {
            $scope.setQuery($scope.keywords);
        }

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
            var provider = {
                type: 'identity',
                name: '',
                authority: authority,
                realm: slug,
                configuration: {}
            };
            if (name) {
                var template = $scope.providerTemplates.find(pt => pt.name === name);
                if (template) {
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

        $scope.exportProvider = function (provider) {
            RealmProviders.exportIdentityProvider(slug, provider.provider);
        };

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

        $scope.webAuthnProviderDlg = function (provider) {
            $scope.providerId = provider ? provider.provider : null;
            $scope.providerAuthority = 'webauthn';
            $scope.provider = provider ? Object.assign({}, provider.configuration) :
                {
                    rpid: 'localhost',
                    rpName: 'AAC',
                    enableRegistration: true,
                    trustUnverifiedAuthenticatorResponses: false,
                    enableUpdate: false,
                    maxSessionDuration: 86400,
                };
            $scope.provider.name = provider ? provider.name : null;
            $('#webAuthnModal').modal({ backdrop: 'static', focus: true })
            Utils.refreshFormBS();
        }

        $scope.saveProvider = function () {
            $('#' + $scope.providerAuthority + 'Modal').modal('hide');

            // HOOK: OIDC contains scopes to be converted to string
            if ($scope.providerAuthority === 'oidc' || $scope.providerAuthority === 'openidfed'  && $scope.provider.scope) {
                $scope.provider.scope = $scope.provider.scope.map(function (s) { return s.text }).join(',');
            }
            var name = $scope.provider.name;
            delete $scope.provider.name;
            var persistence = $scope.provider.persistence;
            delete $scope.provider.persistence;

            var data = {
                realm: $scope.realm.slug,
                name: name,
                persistence: persistence,
                linkable: true,
                configuration: $scope.provider,
                authority: $scope.providerAuthority,
                type: 'identity',
                provider: $scope.providerId
            };
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

        //        $scope.toggleProviderState = function (provider) {
        //            RealmProviders.changeIdentityProviderState($scope.realm.slug, provider.provider, provider)
        //                .then(function (res) {
        //                    provider.enabled = res.enabled;
        //                    provider.registered = res.registered;
        //                    Utils.showSuccess();
        //                })
        //                .catch(function (err) {
        //                    Utils.showError(err.data.message);
        //                });
        //
        //        }

        $scope.toggleProviderState = function (provider, state) {
            var data = {
                'authority': provider.authority,
                'realm': provider.realm,
                'provider': provider.provider,
                'enabled': state
            }

            RealmProviders.changeIdentityProviderState($scope.realm.slug, provider.provider, data)
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
                var template = $scope.providerTemplates.find(pt => pt.name === $scope.provider.clientName);
                if (template) {
                    var pt = Object.assign({}, template.configuration);
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
            if ($scope.importFile) {
                $scope.importFile.file = null;
            }

            $('#importProviderDlg').modal({ keyboard: false });
        }


        $scope.importProvider = function () {
            $('#importProviderDlg').modal('hide');
            var file = $scope.importFile.file;
            var yaml = $scope.importFile.yaml;
            var resetID = !!$scope.importFile.resetID;
            var mimeTypes = ['text/yaml', 'text/yml', 'application/x-yaml'];
            if (!yaml && (file == null || !mimeTypes.includes(file.type) || file.size == 0)) {
                Utils.showError("invalid file");
            } else {
                RealmProviders.importIdentityProvider(slug, file, yaml, resetID)
                    .then(function () {
                        $scope.importFile = null;
                        $scope.load();
                        Utils.showSuccess();
                    })
                    .catch(function (err) {
                        $scope.importFile.file = null;
                        Utils.showError(err.data.message);
                    });
            }
        }

        var iconProvider = function (idp) {
            var icons = ['facebook', 'google', 'microsoft', 'apple', 'instagram', 'github'];

            if (icons.includes(idp.authority.toLowerCase())) {
                return './svg/sprite.svg#logo-' + idp.authority.toLowerCase();
            }

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

            if (idp.authority === "apple") {
                return './svg/sprite.svg#logo-apple';
            }
            return './italia/svg/sprite.svg#it-unlocked';
        }

        $scope.copyText = function (txt) {
            var textField = document.createElement('textarea');
            textField.innerText = txt;
            document.body.appendChild(textField);
            textField.select();
            document.execCommand('copy');
            textField.remove();
        }

        init();
    })
    .controller('RealmIdentityProviderController', function ($scope, $state, $stateParams, RealmData, RealmProviders, RealmAppsData, Utils) {
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
            RealmData.getUrl(slug)
                .then(function (data) {
                    $scope.realmUrls = data;
                })
                .then(function () {
                    return RealmData.getRealm(slug);
                })
                .then(function (realm) {
                    var languages = [];
                    if(realm.localizationConfiguration.languages) {
                        languages = realm.localizationConfiguration.languages;
                    }
                    $scope.availableLanguages = languages;
                    $scope.selectedLanguage = languages[0];
                })
                .then(function () {
                    return RealmProviders.getIdentityProvider(slug, providerId)
                })
                .then(function (data) {
                    $scope.load(data);
                    $scope.formView = 'overview';
                    return data;
                })
                .then(function () {
                    return RealmAppsData.getClientApps(slug);
                })
                .then(function (apps) {
                    $scope.apps = apps;
                })
                .catch(function (err) {
                    Utils.showError('Failed to load provider : ' + err.data.message);
                });
        };

        var initConfiguration = function (authority, config, schema) {
            if (authority == 'oidc' || (schema && schema.id == 'urn:jsonschema:it:smartcommunitylab:aac:oidc:provider:OIDCIdentityProviderConfigMap')) {
                var scopes = [];
                if(config.scope) {
                    toChips(config.scope).forEach(function (s) {
                        scopes.push({ 'text': s });
                    });
                }
                $scope.idpOidcScope = scopes;
            }

            if (authority == 'openidfed' || (schema && schema.id == 'urn:jsonschema:it:smartcommunitylab:aac:openidfed:provider:OpenIdFedIdentityProviderConfigMap')) {
                var scopes = [];
                if(config.scope) {
                    toChips(config.scope).forEach(function (s) {
                        scopes.push({ 'text': s });
                    });
                }
                $scope.openidfedScope = scopes;

                var claims = [];
                if(config.claims) {
                    config.claims.forEach(function (s) {
                        claims.push({ 'text': s });
                    });
                }
                $scope.openidfedClaims = claims;

                var authorityHints = [];
                if(config.authorityHints) {
                    config.authorityHints.forEach(function (t) {
                        authorityHints.push({ 'text': t });
                    });
                }
                $scope.openidfedAuthorityHints = authorityHints;

                var contacts = [];
                if(config.contacts) {
                    config.contacts.forEach(function (t) {
                        contacts.push({ 'text': t });
                    });
                }
                $scope.openidfedContacts = contacts;

                var acrValues = [];
                if(config.acrValues) {
                    config.acrValues.forEach(function (t) {
                        acrValues.push({ 'text': t });
                    });
                }
                $scope.openidfedAcrValues = acrValues;
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

            if (authority == 'spid') {
                var idps = [];
                if (config.idps) {
                    config.idps.forEach(function (s) {
                        idps.push({ 'text': s });
                    });
                }

                $scope.spidIdps = idps;
            }

        }

        var extractConfiguration = function (authority, config, schema) {
            if (authority == 'oidc' || (schema && schema.id == 'urn:jsonschema:it:smartcommunitylab:aac:oidc:provider:OIDCIdentityProviderConfigMap')) {
                var scopes = $scope.idpOidcScope.map(function (s) {
                    if ('text' in s) {
                        return s.text;
                    }
                    return s;
                });
                config.scope = scopes.join(',');
            }
            if (authority == 'openidfed' || (schema && schema.id == 'urn:jsonschema:it:smartcommunitylab:aac:openidfed:provider:OpenIdFedIdentityProviderConfigMap')) {
                console.log('dump',$scope);
                var scopes = $scope.openidfedScope.map(function (s) {
                    if ('text' in s) {
                        return s.text;
                    }
                    return s;
                });
                config.scope = scopes.join(',');

                var claims = $scope.openidfedClaims.map(function (c) {
                    if ('text' in c) {
                        return c.text;
                    }
                    return c;
                });
                config.claims = claims;

                var authorityHints = $scope.openidfedAuthorityHints.map(function (t) {
                    if ('text' in t) {
                        return t.text;
                    }
                    return t;
                });
                config.authorityHints = authorityHints;

                var contacts = $scope.openidfedContacts.map(function (t) {
                    if ('text' in t) {
                        return t.text;
                    }
                    return t;
                });
                config.contacts = contacts;

                var acrValues = $scope.openidfedAcrValues.map(function (t) {
                    if ('text' in t) {
                        return t.text;
                    }
                    return t;
                });
                config.acrValues = acrValues;
            }
            if (authority == 'saml') {
                var authnContextClasses = $scope.samlAuthnContextClasses.map(function (s) {
                    if ('text' in s) {
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

            if (authority == 'spid') {
                var idps = $scope.spidIdps.map(function (s) {
                    if ('text' in s) {
                        return s.text;
                    }
                    return s;
                });
                if (idps && idps.length) {
                    config.idps = idps;
                } else {
                    config.idps = null;
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
            $scope.providerIcon = iconProvider(data);

            initConfiguration(data.authority, data.configuration, data.schema);

            var attributeMapping = {
                enabled: false,
                code: "",
                result: null,
                error: null
            };
            if ("hookFunctions" in data.settings && "attributeMapping" in data.settings.hookFunctions) {
                attributeMapping.enabled = true;
                attributeMapping.code = atob(data.settings.hookFunctions["attributeMapping"]);
            }

            $scope.attributeMapping = attributeMapping;

            var authFunction = {
                enabled: false,
                code: "",
                result: null,
                error: null
            };
            if ("hookFunctions" in data.settings && "authorize" in data.settings.hookFunctions) {
                authFunction.enabled = true;
                authFunction.code = atob(data.settings.hookFunctions["authorize"]);
            }

            $scope.authFunction = authFunction;

            if (data.authority == 'saml') {
                var metadataUrl = $scope.realmUrls.applicationUrl + "/auth/" + data.authority + "/metadata/" + data.provider;
                $scope.samlMetadataUrl = metadataUrl;
            }
            if (data.authority == 'spid') {
                // var metadataUrl = $scope.realmUrls.applicationUrl + "/auth/" + data.authority + "/metadata/" + data.provider;
                const regId = btoa(data.provider).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
                var metadataUrl = $scope.realmUrls.applicationUrl + "/auth/" + data.authority + "/metadata/" + regId;
                $scope.samlMetadataUrl = metadataUrl;
            }
            if (data.authority == 'oidc' || data.authority == 'apple' || data.schema.id == 'urn:jsonschema:it:smartcommunitylab:aac:oidc:provider:OIDCIdentityProviderConfigMap') {
                var loginUrl = $scope.realmUrls.applicationUrl + "/auth/" + data.authority + "/login/" + data.provider;
                $scope.oidcRedirectUrl = loginUrl;
            }
            if (data.authority == 'openidfed' || data.schema.id == 'urn:jsonschema:it:smartcommunitylab:aac:openidfed:provider:OpenIdFedIdentityProviderConfigMap') {
                var metadataUrl = $scope.realmUrls.applicationUrl + "/auth/" + data.authority + "/metadata/" + data.provider;
                $scope.openidfedMetadataUrl = metadataUrl;
            }

        };


        $scope.deleteProviderDlg = function (provider) {
            $scope.modProvider = provider;
            $('#deleteProviderConfirm').modal({ keyboard: false });
        }

        $scope.deleteProvider = function () {
            $('#deleteProviderConfirm').modal('hide');
            RealmProviders.removeIdentityProvider($scope.realm.slug, $scope.modProvider.provider).then(function () {
                $state.go('realm.idps', { realmId: $scope.realm.slug });
            }).catch(function (err) {
                Utils.showError(err.data.message);
            });
        }

        $scope.saveProvider = function (provider) {

            var configuration = extractConfiguration(provider.authority, provider.configuration, provider.schema);

            var hookFunctions = {};
            if ($scope.attributeMapping.code != "") {
                hookFunctions["attributeMapping"] = btoa($scope.attributeMapping.code);
            }
            if ($scope.authFunction.code != "") {
                hookFunctions["authorize"] = btoa($scope.authFunction.code);
            }
            var settings = {
                persistence: provider.settings.persistence,
                linkable: provider.settings.linkable,
                events: provider.settings.events,
                position: provider.settings.position,
                template: provider.settings.template,
                notes: provider.settings.notes,
                hookFunctions: hookFunctions
            }

            var data = {
                realm: provider.realm,
                provider: provider.provider,
                type: provider.type,
                authority: provider.authority,
                name: provider.name,
                titleMap: provider.titleMap,
                descriptionMap: provider.descriptionMap,
                displayMode: provider.displayMode,
                enabled: provider.enabled,
                settings: settings,
                configuration: configuration,
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

        $scope.removeTitleMapLanguage = function (language) {
            delete $scope.idp.titleMap[language];
        }

        $scope.removeDescriptionMapLanguage = function (language) {
            delete $scope.idp.descriptionMap[language];
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
            RealmProviders.exportIdentityProvider(slug, provider.provider);
        };

        $scope.inspectProvider = function (provider) {
            if (provider && provider.enabled) {
                RealmProviders.getIdentityProviderConfig(slug, provider.provider)
                    .then(function (res) {
                        $scope.inspectDlg(res);
                    })
                    .catch(function (err) {
                        Utils.showError(err.data.message);
                    });
            }
        }

        $scope.inspectDlg = function (obj) {
            $scope.modObj = obj;
            $scope.modObj.json = JSON.stringify(obj, null, 3);
            $('#inspectModal').modal({ keyboard: false });
        }

        $scope.applyProviderTemplate = function () {
            var configuration = $scope.idp.configuration;
            if (configuration.clientName) {
                var template = $scope.providerTemplates.find(pt => pt.name === configuration.clientName);
                if (template) {
                    var pt = Object.assign({}, template.configuration);
                    var ptScopes = [];
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

        $scope.testProviderAttributeMapping = function () {
            var attributeMapping = $scope.attributeMapping;
            var functionCode = attributeMapping.code;

            if (functionCode == '') {
                Utils.showError("empty function code");
                return;
            }

            var data = {
                name: 'attributeMapping',
                code: btoa(functionCode),
            }

            RealmProviders.testIdentityProviderClaimMapping(slug, providerId, data)
                .then(function (res) {
                    $scope.attributeMapping.result = res.result;
                    $scope.attributeMapping.errors = res.errors;
                    $scope.attributeMapping.context = (res.context ? JSON.stringify(res.context, null, 4) : '{}');
                }).catch(function (err) {
                    $scope.attributeMapping.result = null;
                    $scope.attributeMapping.context = (err.context ? JSON.stringify(err.context, null, 4) : '{}');
                    $scope.attributeMapping.errors = [err.data.message];
                });
        }

        $scope.toggleProviderAuthFunction = function () {
            var authFunction = $scope.authFunction;

            if (authFunction.enabled && authFunction.code == '') {
                authFunction.code =
                    '/**\n * DEFINE YOUR OWN AUTHORIZATION FUNCTION HERE\n' +
                    '**/\n' +
                    'function authorize(principal, context) {\n   return true;\n}';
            }

            authFunction.error = null;
            authFunction.result = null;

            $scope.authFunction = authFunction;

        }

        $scope.testProviderAuthFunction = function () {
            var authFunction = $scope.authFunction;
            console.log(authFunction);
            var functionCode = authFunction.code;

            if (functionCode == '') {
                Utils.showError("empty function code");
                return;
            }

            var data = {
                name: 'authorize',
                code: btoa(functionCode),
            }

            RealmProviders.testIdentityProviderAuthFunction(slug, providerId, data)
                .then(function (res) {
                    $scope.authFunction.result = res.result;
                    $scope.authFunction.errors = res.errors;
                    $scope.authFunction.context = (res.context ? JSON.stringify(res.context, null, 4) : '{}');
                }).catch(function (err) {
                    $scope.authFunction.result = null;
                    $scope.authFunction.context = (err.context ? JSON.stringify(err.context, null, 4) : '{}');
                    $scope.authFunction.errors = [err.data.message];
                });
        }

        $scope.toggleProviderClientApp = function (provider, client) {
            if (client.clientId) {
                var clientId = client.clientId;
                var providers = client.providers;
                if (providers.includes(provider)) {
                    providers = providers.filter(function (p) { return p != provider });
                } else {
                    providers.push(provider);
                }

                var data = {
                    'clientId': clientId,
                    'type': client.type,
                    'name': client.name,
                    'providers': providers
                };

                RealmProviders.changeIdentityProviderClientApp($scope.realm.slug, provider, data)
                    .then(function (res) {
                        client.providers = res.providers;
                        Utils.showSuccess();
                    })
                    .catch(function (err) {
                        Utils.showError(err.data.message);
                    });
            }
        }

        // $scope.toggleLang = function () {
        //     if (lang && $scope.selectedLanguage !== lang && $scope.availableLanguages.includes(lang)) {
        //         // switch lang
        //         $scope.selectedLanguage = lang;

        //         //switch fields content and map keys
        //         fields.forEach(f => {

        //         })
        //     }
        // }

        var iconProvider = function (ap) {
            var icons = ['facebook', 'google', 'microsoft', 'apple', 'instagram', 'github'];

            if (icons.includes(ap.authority.toLowerCase())) {
                return './svg/sprite.svg#logo-' + ap.authority.toLowerCase();
            }

            if (ap.authority === "oidc" && 'clientName' in ap.configuration) {
                var logo = null;
                if (icons.includes(ap.configuration.clientName.toLowerCase())) {
                    logo = ap.configuration.clientName.toLowerCase();
                } else if (icons.includes(ap.name.toLowerCase())) {
                    logo = ap.name.toLowerCase();
                }

                if (logo) {
                    return './svg/sprite.svg#logo-' + logo;
                }
            }

            if (ap.authority === "apple") {
                return './svg/sprite.svg#logo-apple';
            }
            return './italia/svg/sprite.svg#it-unlocked';
        }


        $scope.copyText = function (txt) {
            var textField = document.createElement('textarea');
            textField.innerText = txt;
            document.body.appendChild(textField);
            textField.select();
            document.execCommand('copy');
            textField.remove();
        }

        init();
    })
    /**
      * Realm attribute providers controller
      */
    .controller('RealmAttributeProvidersController', function ($scope, $state, $stateParams, $window, RealmData, RealmProviders, RealmAppsData, Utils) {
        var slug = $stateParams.realmId;
        $scope.query = {
            page: 0,
            size: 20,
            sort: { name: 1 },
            q: ''
        }
        $scope.keywords = '';

        $scope.load = function () {
            RealmProviders.searchAttributeProviders(slug, $scope.query)
                .then(function (data) {
                    data.content.forEach(function (ap) {
                        //add icon
                        ap.icon = iconProvider(ap);
                    });
                    $scope.providers = data;
                })
                .catch(function (err) {
                    Utils.showError('Failed to load realm providers: ' + err.data.message);
                });
        }

        /**
         * Initialize the app: load list of the providers
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
            $scope.query.page = 0;
            $scope.load();
        }

        $scope.runQuery = function () {
            $scope.setQuery($scope.keywords);
        }

        $scope.deleteProviderDlg = function (provider) {
            $scope.modProvider = provider;
            //add confirm field
            $scope.modProvider.confirmId = '';
            $('#deleteProviderConfirm').modal({ keyboard: false });
        }

        $scope.deleteProvider = function () {
            $('#deleteProviderConfirm').modal('hide');
            if ($scope.modProvider.provider === $scope.modProvider.confirmId) {
                RealmProviders.removeAttributeProvider($scope.realm.slug, $scope.modProvider.provider).then(function () {
                    $scope.load();
                    Utils.showSuccess();
                }).catch(function (err) {
                    Utils.showError(err.data.message);
                });
            } else {
                Utils.showError("confirmId not valid");
            }
        }

        $scope.createProviderDlg = function (authority) {
            var provider = {
                type: 'attributes',
                name: '',
                authority: authority,
                realm: slug,
                configuration: {}
            };

            $scope.modProvider = provider;

            $('#createProviderDlg').modal({ keyboard: false });
            Utils.refreshFormBS();
        }

        $scope.createProvider = function () {
            $('#createProviderDlg').modal('hide');
            var data = $scope.modProvider
            data.realm = slug;

            RealmProviders.saveAttributeProvider(slug, data)
                .then(function () {
                    $scope.load();
                    Utils.showSuccess();
                })
                .catch(function (err) {
                    Utils.showError(err.data.message);
                });


        }





        $scope.saveProvider = function () {
            var name = $scope.provider.name;
            delete $scope.provider.name;
            var persistence = $scope.provider.persistence;
            delete $scope.provider.persistence;

            var data = {
                realm: $scope.realm.slug,
                name: name,
                persistence: persistence,
                configuration: $scope.provider,
                authority: $scope.providerAuthority,
                type: 'attributes',
                provider: $scope.providerId
            };
            RealmProviders.saveAttributeProvider($scope.realm.slug, data)
                .then(function () {
                    $scope.load();
                    Utils.showSuccess();
                })
                .catch(function (err) {
                    Utils.showError(err.data.message);
                });
        }

        $scope.toggleProviderState = function (provider, state) {
            var data = {
                'authority': provider.authority,
                'realm': provider.realm,
                'provider': provider.provider,
                'enabled': state
            }

            RealmProviders.changeAttributeProviderState($scope.realm.slug, provider.provider, data)
                .then(function (res) {
                    provider.enabled = res.enabled;
                    provider.registered = res.registered;
                    Utils.showSuccess();
                })
                .catch(function (err) {
                    Utils.showError(err.data.message);
                });

        }

        $scope.updatePersistenceType = function () {
            Utils.refreshFormBS();
        }


        $scope.importProviderDlg = function () {
            if ($scope.importFile) {
                $scope.importFile.file = null;
            }

            $('#importProviderDlg').modal({ keyboard: false });
        }


        $scope.importProvider = function () {
            $('#importProviderDlg').modal('hide');
            var file = $scope.importFile.file;
            var yaml = $scope.importFile.yaml;
            var resetID = !!$scope.importFile.resetID;
            var mimeTypes = ['text/yaml', 'text/yml', 'application/x-yaml'];
            if (!yaml && (file == null || !mimeTypes.includes(file.type) || file.size == 0)) {
                Utils.showError("invalid file");
            } else {
                RealmProviders.importAttributeProvider(slug, file, yaml, resetID)
                    .then(function () {
                        $scope.importFile = null;
                        $scope.load();
                        Utils.showSuccess();
                    })
                    .catch(function (err) {
                        $scope.importFile.file = null;
                        Utils.showError(err.data.message);
                    });
            }
        }

        $scope.exportProvider = function (provider) {
            RealmProviders.exportAttributeProvider(slug, provider.provider);
        };

        var iconProvider = function (idp) {
            if (idp.authority === "mapper") {
                return './italia/svg/sprite.svg#it-exchange-circle';
            }
            if (idp.authority === "internal") {
                return './italia/svg/sprite.svg#it-upload';
            }
            if (idp.authority === "webhook") {
                return './italia/svg/sprite.svg#it-download';
            }
            if (idp.authority === "script") {
                return './italia/svg/sprite.svg#it-software';
            }
            return './italia/svg/sprite.svg#it-file';
        }

        $scope.copyText = function (txt) {
            var textField = document.createElement('textarea');
            textField.innerText = txt;
            document.body.appendChild(textField);
            textField.select();
            document.execCommand('copy');
            textField.remove();
        }

        init();
    })
    .controller('RealmAttributeProviderController', function ($scope, $state, $stateParams, RealmData, RealmProviders, RealmAppsData, RealmAttributeSets, Utils) {
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
            RealmAttributeSets.getAttributeSets(slug)
                .then(function (data) {
                    $scope.attributeSets = data.filter(s => s.realm == slug);
                    return data;
                })
                .then(function () {
                    return RealmProviders.getAttributeProvider(slug, providerId)
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
            if (authority == 'script') {
                $scope.script = {
                    code: config.code ? atob(config.code) : 'function attributeMapping(principal) {\n return {}; \n}'
                }
            }
        }

        var extractConfiguration = function (authority, config) {
            if (authority == 'script' && $scope.script) {
                var code = $scope.script.code;
                config.code = code !== null ? btoa(code) : null;
            }
            return config;
        };


        $scope.load = function (data) {
            $scope.providerName = data.name != '' ? data.name : data.provider;
            $scope.ap = data;
            $scope.providerIcon = iconProvider(data);
            $scope.test = {
                context: null,
                result: null,
                errors: null
            }
            initConfiguration(data.authority, data.configuration);
        };


        $scope.deleteProviderDlg = function (provider) {
            $scope.modProvider = provider;
            $('#deleteProviderConfirm').modal({ keyboard: false });
        }

        $scope.deleteProvider = function () {
            $('#deleteProviderConfirm').modal('hide');
            RealmProviders.removeAttributeProvider($scope.realm.slug, $scope.modProvider.provider).then(function () {
                $state.go('realm.aps', { realmId: $scope.realm.slug });
            }).catch(function (err) {
                Utils.showError(err.data.message);
            });
        }

        $scope.saveProvider = function (provider) {
            var configuration = extractConfiguration(provider.authority, provider.configuration);

            var data = {
                realm: provider.realm,
                provider: provider.provider,
                type: provider.type,
                authority: provider.authority,
                name: provider.name,
                description: provider.description,
                enabled: provider.enabled,
                settings: provider.settings,
                configuration: configuration

            }

            RealmProviders.saveAttributeProvider($scope.realm.slug, data)
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

            RealmProviders.changeAttributeProviderState($scope.realm.slug, provider.provider, provider)
                .then(function (res) {
                    provider.enabled = res.enabled;
                    provider.registered = res.registered;
                    Utils.showSuccess();
                })
                .catch(function (err) {
                    Utils.showError(err.data.message);
                });

        }

        $scope.toggleProviderAttributeSet = function (attributeSet) {
            if(!$scope.ap.settings.attributeSets){
                $scope.ap.settings.attributeSets = [];
            }
            if (attributeSet.identifier) {
                var id = attributeSet.identifier;
                var setIds = $scope.ap.settings.attributeSets;
                if ($scope.ap.settings.attributeSets.includes(id)) {
                    setIds = setIds.filter(i => id != i);
                } else {
                    setIds.push(id);
                }

                $scope.ap.settings.attributeSets = setIds;

            }
        }

        $scope.testProvider = function (provider) {
            if (!provider.enabled) {
                Utils.showError('provider must be enabled');
            }

            RealmProviders.testAttributeProvider($scope.realm.slug, provider.provider)
                .then(function (res) {
                    $scope.test.context = res.context;
                    $scope.test.result = res.result;
                    $scope.test.errors = res.errors;

                    Utils.showSuccess();
                })
                .catch(function (err) {
                    $scope.test.context = null;
                    $scope.test.result = null;
                    $scope.test.errors = null;

                    Utils.showError(err.data.message);
                });

        }

        $scope.exportProvider = function (provider) {
            RealmProviders.exportAttributeProvider(slug, provider.provider);
        };

        var iconProvider = function (idp) {
            if (idp.authority === "mapper") {
                return './italia/svg/sprite.svg#it-exchange-circle';
            }
            if (idp.authority === "internal") {
                return './italia/svg/sprite.svg#it-upload';
            }
            if (idp.authority === "webhook") {
                return './italia/svg/sprite.svg#it-download';
            }
            if (idp.authority === "script") {
                return './italia/svg/sprite.svg#it-software';
            }
            return './italia/svg/sprite.svg#it-file';
        }

        $scope.copyText = function (txt) {
            var textField = document.createElement('textarea');
            textField.innerText = txt;
            document.body.appendChild(textField);
            textField.select();
            document.execCommand('copy');
            textField.remove();
        }

        init();
    })
    ;