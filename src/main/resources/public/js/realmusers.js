angular.module('aac.controllers.realmusers', [])
    /**
      * Realm Data Services
      */
    .service('RealmUsers', function ($q, $http, $httpParamSerializer) {
        var rService = {};
        rService.getUsers = function (slug, params) {
            return $http.get('console/dev/realms/' + slug + '/users?' + buildQuery(params, $httpParamSerializer)).then(function (data) {
                return data.data;
            });
        }
        rService.getUser = function (slug, subject) {
            return $http.get('console/dev/realms/' + slug + '/users/' + subject).then(function (data) {
                return data.data;
            });
        }

        rService.removeUser = function (slug, user) {
            return $http.delete('console/dev/realms/' + slug + '/users/' + user.subjectId).then(function (data) {
                return data.data;
            });
        }
        rService.updateRealmRoles = function (slug, user, roles) {
            return $http.put('console/dev/realms/' + slug + '/users/' + user.subjectId + '/roles', { roles: roles }).then(function (data) {
                return data.data;
            });
        }

        rService.inviteUser = function (slug, invitation, roles) {
            var data = { roles: roles, username: invitation.external ? null : invitation.username, subjectId: invitation.external ? invitation.subjectId : null };
            return $http.post('console/dev/realms/' + slug + '/users/invite', data).then(function (data) {
                return data.data;
            });
        }
        rService.getUserAttributes = function (slug, subject) {
            return $http.get('console/dev/realms/' + slug + '/users/' + subject + '/attributes').then(function (data) {
                return data.data;
            });
        }
        return rService;

    })
    /**
     * Realm users controller
     */
    .controller('RealmUsersController', function ($scope, $stateParams, RealmData, RealmUsers, RealmProviders, Utils) {
        var slug = $stateParams.realmId;
        $scope.query = {
            page: 0,
            size: 20,
            sort: { username: 1 },
            q: ''
        }
        $scope.keywords = '';

        $scope.load = function () {
            RealmUsers.getUsers(slug, $scope.query)
                .then(function (data) {
                    $scope.keywords = $scope.query.q;
                    $scope.users = data;
                    $scope.users.content.forEach(function (u) {
                        if ('identities' in u) {
                            u._providers = u.identities.map(function (i) {
                                return $scope.providers[i.provider] ? $scope.providers[i.provider].name : i.provider;
                            });
                        }
                        if ('authorities' in u) {
                            u._authorities = u.authorities
                                .filter(function (a) { return a.realm === $scope.realm.slug })
                                .map(function (a) { return a.role });
                        }
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
            $scope.systemRoles = ['ROLE_ADMIN', 'ROLE_DEVELOPER'];

            RealmProviders.getIdentityProviders(slug)
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
            RealmUsers.removeUser($scope.realm.slug, $scope.modUser).then(function () {
                $scope.load();
                Utils.showSuccess();
            }).catch(function (err) {
                Utils.showError(err.data.message);
            });
        }

        $scope.jsonUserDlg = function (user) {
            $scope.modUser = user;
            $('#userJsonModal').modal({ keyboard: false });
        }

        $scope.jsonUserAttributesDlg = function (user) {
            $scope.modAttributes = null;

            RealmUsers.getUserAttributes($scope.realm.slug, user.subjectId).then(function (data) {
                $scope.modAttributes = data;
                Utils.showSuccess();
            }).catch(function (err) {
                Utils.showError(err.data.message);
            });
            $('#attributesJsonModal').modal({ keyboard: false });
        }

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

        $scope.copyText = function (txt) {
            var textField = document.createElement('textarea');
            textField.innerText = txt;
            document.body.appendChild(textField);
            textField.select();
            document.execCommand('copy');
            textField.remove();
        }

        init();

        $scope.editRoles = function (user) {
            var systemRoles = $scope.systemRoles.map(r => {
                return {
                    'text': r,
                    'value': user._authorities.includes(r)
                };
            });

            var customRoles = user._authorities
                .filter(a => !$scope.systemRoles.includes(a))
                .map(r => {
                    return {
                        'text': r
                    };
                });

            $scope.modUser = {
                ...user,
                'systemRoles': systemRoles,
                'customRoles': customRoles
            };

            $('#rolesModal').modal({ backdrop: 'static', focus: true })
        }

        // save roles
        $scope.updateRoles = function () {
            $('#rolesModal').modal('hide');

            if ($scope.modUser) {
                var systemRoles = $scope.modUser.systemRoles.filter(r => r.value).map(r => r.text);
                var customRoles = $scope.modUser.customRoles.map(r => r.text);

                var roles = systemRoles.concat(customRoles);

                RealmUsers.updateRealmRoles($scope.realm.slug, $scope.modUser, roles)
                    .then(function () {
                        $scope.load();
                        Utils.showSuccess();
                    })
                    .catch(function (err) {
                        Utils.showError(err);
                    });

                $scope.modUser = null;
            }
        }

        $scope.inviteUser = function () {
            var systemRoles = $scope.systemRoles.map(r => {
                return {
                    'text': r,
                    'value': false
                };
            });

            $scope.invitation = {
                'external': false,
                'username': null,
                'subjectId': null,
                'systemRoles': systemRoles,
                'customRoles': []
            }

            $('#inviteModal').modal({ backdrop: 'static', focus: true })
        }
        $scope.invite = function () {
            $('#inviteModal').modal('hide');

            if ($scope.invitation) {
                var systemRoles = $scope.invitation.systemRoles.filter(r => r.value).map(r => r.text);
                var customRoles = $scope.invitation.customRoles.map(r => r.text);

                var roles = systemRoles.concat(customRoles);

                RealmUsers.inviteUser($scope.realm.slug, $scope.invitation, roles).then(function () {
                    $scope.load();
                    Utils.showSuccess();
                }).catch(function (err) {
                    Utils.showError(err.data.message);
                });

                $scope.invitation = null;
            }



        }


        // $scope.hasRoles = function (m1, m2) {
        //   var res = false;
        //   for (var r1 in m1) res |= m1[r1];
        //   for (var r2 in m2) res |= m2[r2];
        //   return res;
        // }



        $scope.dismiss = function () {
            $('#rolesModal').modal('hide');
        }

        // $scope.addRole = function () {
        //   $scope.roles.map[$scope.roles.custom] = true;
        //   $scope.roles.custom = null;
        // }

        // $scope.invalidRole = function (role) {
        //   return !role || !(/^[a-zA-Z0-9_]{3,63}((\.[a-zA-Z0-9_]{2,63})*\.[a-zA-Z]{2,63})?$/g.test(role))
        // }

    })
    .controller('RealmUserController', function ($scope, $stateParams, RealmData, RealmUsers, RealmProviders, RealmAttributeSets, Utils) {
        var slug = $stateParams.realmId;
        var subjectId = $stateParams.subjectId;
        $scope.curView = 'overview';


        $scope.activeView = function (view) {
            return view == $scope.curView ? 'active' : '';
        };

        $scope.switchView = function (view) {
            $scope.curView = view;
            Utils.refreshFormBS(300);
        }

        $scope.load = function () {
            RealmUsers.getUser(slug, subjectId)
                .then(function (data) {
                    $scope.username = data.username;
                    $scope.user = data;
                    return data;
                })
                .then(function (data) {
                    var idps = $scope.idps;
                    var identities = data.identities;
                    if (identities) {
                        identities.forEach(i => {
                            i.providerId = i.provider;
                            i.provider = idps[i.providerId];
                            i.icon = iconIdp(i.provider);
                        });
                    }
                    $scope.identities = identities;
                    return data;
                })
                .then(function (data) {
                    $scope.authorities = data.authorities.filter(a => a.realm && slug == a.realm);
                })
                .catch(function (err) {
                    Utils.showError('Failed to load realm users: ' + err.data.message);
                });
        }

        /**
       * Initialize the app: load list of the users
       */
        var init = function () {
            $scope.systemRoles = ['ROLE_ADMIN', 'ROLE_DEVELOPER'];

            RealmProviders.getIdentityProviders(slug)
                .then(function (providers) {
                    var pMap = {};
                    providers.forEach(function (p) {
                        p.icon = iconIdp(p);
                        pMap[p.provider] = p
                    });
                    $scope.idps = pMap;
                    return;
                })
                .then(function () {
                    return RealmAttributeSets.getAttributeSets(slug);
                })
                .then(function (sets) {
                    var sMap = {};
                    sets.forEach(function (s) {
                        var ss = {
                            ...s,
                            map: new Map(s.attributes.map(e => [e.key, e]))
                        };
                        sMap[s.identifier] = ss

                    });
                    $scope.attributeSets = sMap;
                    return;
                })
                .then(function () {
                    $scope.load();
                })
                .catch(function (err) {
                    Utils.showError('Failed to load realm users: ' + err.data.message);
                });
        };


        init();


        $scope.editRoles = function (user) {
            var systemRoles = $scope.systemRoles.map(r => {
                return {
                    'text': r,
                    'value': user._authorities.includes(r)
                };
            });

            var customRoles = user._authorities
                .filter(a => !$scope.systemRoles.includes(a))
                .map(r => {
                    return {
                        'text': r
                    };
                });

            $scope.modUser = {
                ...user,
                'systemRoles': systemRoles,
                'customRoles': customRoles
            };

            $('#rolesModal').modal({ backdrop: 'static', focus: true })
        }

        // save roles
        $scope.updateRoles = function () {
            $('#rolesModal').modal('hide');

            if ($scope.modUser) {
                var systemRoles = $scope.modUser.systemRoles.filter(r => r.value).map(r => r.text);
                var customRoles = $scope.modUser.customRoles.map(r => r.text);

                var roles = systemRoles.concat(customRoles);

                RealmUsers.updateRealmRoles($scope.realm.slug, $scope.modUser, roles)
                    .then(function () {
                        $scope.load();
                        Utils.showSuccess();
                    })
                    .catch(function (err) {
                        Utils.showError(err);
                    });

                $scope.modUser = null;
            }
        }


        $scope.inspectDlg = function (obj) {
            $scope.modObj = obj;
            $('#inspectModal').modal({ keyboard: false });
        }

        $scope.viewAttributes = function (attributes) {
            $scope.modAttributes = {
                ...attributes,
                attributeSet: $scope.attributeSets[attributes.identifier]
            };
            $('#viewAttributesDlg').modal({ keyboard: false });
        }

        $scope.copyText = function (txt) {
            var textField = document.createElement('textarea');
            textField.innerText = txt;
            document.body.appendChild(textField);
            textField.select();
            document.execCommand('copy');
            textField.remove();
        }

        var iconIdp = function (idp) {
            var icons = ['facebook', 'google', 'microsoft', 'apple', 'instagram', 'github'];

            if (idp.authority === "oidc") {
                var logo = null;
                if ('clientName' in idp.configuration && icons.includes(idp.configuration.clientName.toLowerCase())) {
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
    })
    ;