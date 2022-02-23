angular.module('aac.controllers.realmusers', [])
    /**
      * Realm Data Services
      */
    .service('RealmUsers', function ($http, $httpParamSerializer) {
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

        service.getUsers = function (slug, params) {
            return $http.get('console/dev/realms/' + slug + '/users?' + buildQuery(params)).then(function (data) {
                return data.data;
            });
        }
        service.getUser = function (slug, subject) {
            return $http.get('console/dev/realms/' + slug + '/users/' + subject).then(function (data) {
                return data.data;
            });
        }

        service.removeUser = function (slug, subject) {
            return $http.delete('console/dev/realms/' + slug + '/users/' + subject).then(function (data) {
                return data.data;
            });
        }

        service.getAuthorities = function (slug, subject) {
            return $http.get('console/dev/realms/' + slug + '/users/' + subject + '/authorities').then(function (data) {
                return data.data;
            });
        }
        service.updateAuthorities = function (slug, subject, authorities) {
            return $http.put('console/dev/realms/' + slug + '/users/' + subject + '/authorities', authorities).then(function (data) {
                return data.data;
            });
        }

        service.getRealmGroups = function (slug, subject) {
            return $http.get('console/dev/realms/' + slug + '/users/' + subject + '/groups').then(function (data) {
                return data.data;
            });
        }
        service.updateRealmGroups = function (slug, subject, groups) {
            return $http.put('console/dev/realms/' + slug + '/users/' + subject + '/groups', groups).then(function (data) {
                return data.data;
            });
        }

        service.getRealmRoles = function (slug, subject) {
            return $http.get('console/dev/realms/' + slug + '/users/' + subject + '/roles').then(function (data) {
                return data.data;
            });
        }
        service.updateRealmRoles = function (slug, subject, roles) {
            return $http.put('console/dev/realms/' + slug + '/users/' + subject + '/roles', roles).then(function (data) {
                return data.data;
            });
        }

        service.getSpaceRoles = function (slug, subject) {
            return $http.get('console/dev/realms/' + slug + '/users/' + subject + '/spaceroles').then(function (data) {
                return data.data;
            });
        }
        service.updateSpaceRoles = function (slug, subject, roles) {
            return $http.put('console/dev/realms/' + slug + '/users/' + subject + '/spaceroles', roles).then(function (data) {
                return data.data;
            });
        }

        service.blockUser = function (slug, subject) {
            return $http.put('console/dev/realms/' + slug + '/users/' + subject + '/block').then(function (data) {
                return data.data;
            });
        }

        service.unblockUser = function (slug, subject) {
            return $http.delete('console/dev/realms/' + slug + '/users/' + subject + '/block').then(function (data) {
                return data.data;
            });
        }

        service.lockUser = function (slug, subject) {
            return $http.put('console/dev/realms/' + slug + '/users/' + subject + '/lock').then(function (data) {
                return data.data;
            });
        }

        service.unlockUser = function (slug, subject) {
            return $http.delete('console/dev/realms/' + slug + '/users/' + subject + '/lock').then(function (data) {
                return data.data;
            });
        }

        service.verifyUser = function (slug, subject) {
            return $http.put('console/dev/realms/' + slug + '/users/' + subject + '/verify').then(function (data) {
                return data.data;
            });
        }

        service.unverifyUser = function (slug, subject) {
            return $http.delete('console/dev/realms/' + slug + '/users/' + subject + '/unverify').then(function (data) {
                return data.data;
            });
        }

        service.inviteUser = function (slug, invitation, roles) {
            var data = { roles: roles, username: invitation.external ? null : invitation.username, subjectId: invitation.external ? invitation.subjectId : null };
            return $http.post('console/dev/realms/' + slug + '/users/invite', data).then(function (data) {
                return data.data;
            });
        }
        service.getUserAttributes = function (slug, subject) {
            return $http.get('console/dev/realms/' + slug + '/users/' + subject + '/attributes').then(function (data) {
                return data.data;
            });
        }
        service.getUserAttributesSet = function (slug, subject, provider, identifier) {
            return $http.get('console/dev/realms/' + slug + '/users/' + subject + '/attributes/' + provider + '/' + identifier).then(function (data) {
                return data.data;
            });
        }
        service.setUserAttributesSet = function (slug, subject, attributes) {
            return $http.put('console/dev/realms/' + slug + '/users/' + subject + '/attributes/' + attributes.provider + '/' + attributes.identifier, attributes.attributes).then(function (data) {
                return data.data;
            });
        }
        service.deleteUserAttributesSet = function (slug, subject, attributes) {
            return $http.delete('console/dev/realms/' + slug + '/users/' + subject + '/attributes/' + attributes.provider + '/' + attributes.identifier).then(function (data) {
                return data.data;
            });
        }
        service.getApps = function (slug, subject) {
            return $http.get('console/dev/realms/' + slug + '/users/' + subject + '/apps').then(function (data) {
                return data.data;
            });
        }
        service.revokeApp = function (slug, subject, clientId) {
            return $http.delete('console/dev/realms/' + slug + '/users/' + subject + '/apps/' + clientId).then(function (data) {
                return data.data;
            });
        }
        service.getAudit = function (slug, subject) {
            return $http.get('console/dev/realms/' + slug + '/users/' + subject + '/audit').then(function (data) {
                return data.data;
            });
        }
        service.getApprovals = function (slug, subject) {
            return $http.get('console/dev/realms/' + slug + '/users/' + subject + '/approvals').then(function (data) {
                return data.data;
            });
        }
        service.getTokens = function (slug, subject) {
            return $http.get('console/dev/realms/' + slug + '/users/' + subject + '/tokens').then(function (data) {
                return data.data;
            });
        }
        return service;

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

        $scope.aceOption = {
            mode: 'javascript',
            theme: 'monokai',
            maxLines: 40,
            minLines: 20
        };


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
                        if ('roles' in u) {
                            u._roles = u.roles
                                .filter(function (a) { return slug == a.realm })
                                .map(function (a) { return a.role });
                        }
                        if ('authorities' in u) {
                            u._authorities = u.authorities
                                .filter(function (a) { return slug == a.realm })
                                .map(function (a) { return a.role });
                        }
                        if ('groups' in u) {
                            u._groups = u.groups
                                .filter(function (a) { return slug == a.realm })
                                .map(function (a) { return a.group });
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
            RealmUsers.removeUser($scope.realm.slug, $scope.modUser.subjectId).then(function () {
                $scope.load();
                Utils.showSuccess();
            }).catch(function (err) {
                Utils.showError(err.data.message);
            });
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


        $scope.blockUser = function (user) {
            RealmUsers.blockUser(slug, user.subjectId)
                .then(function (data) {
                    user.blocked = data.blocked;
                    Utils.showSuccess();
                }).catch(function (err) {
                    Utils.showError(err.data.message);
                });
        }
        $scope.unblockUser = function (user) {
            RealmUsers.unblockUser(slug, user.subjectId)
                .then(function (data) {
                    user.blocked = data.blocked;
                    Utils.showSuccess();
                }).catch(function (err) {
                    Utils.showError(err.data.message);
                });
        }
        $scope.lockUser = function (user) {
            RealmUsers.lockUser(slug, user.subjectId)
                .then(function (data) {
                    user.locked = data.locked;
                    Utils.showSuccess();
                }).catch(function (err) {
                    Utils.showError(err.data.message);
                });
        }
        $scope.unlockUser = function (user) {
            RealmUsers.unlockUser(slug, user.subjectId)
                .then(function (data) {
                    user.locked = data.locked;
                    Utils.showSuccess();
                }).catch(function (err) {
                    Utils.showError(err.data.message);
                });
        }
        $scope.verifyUser = function (user) {
            RealmUsers.verifyUser(slug, user.subjectId)
                .then(function (data) {
                    user.emailVerified = data.emailVerified;
                    Utils.showSuccess();
                }).catch(function (err) {
                    Utils.showError(err.data.message);
                });
        }
        $scope.unverifyUser = function (user) {
            RealmUsers.unverifyUser(slug, user.subjectId)
                .then(function (data) {
                    user.emailVerified = data.emailVerified;
                    Utils.showSuccess();
                }).catch(function (err) {
                    Utils.showError(err.data.message);
                });
        }

        $scope.inspectDlg = function (obj) {
            $scope.modObj = obj;
            $scope.modObj.json = JSON.stringify(obj, null, 3);
            $('#inspectModal').modal({ keyboard: false });
        }

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

                var data = roles.map(r => {
                    return { 'realm': slug, 'role': r }
                });

                RealmUsers.updateRealmRoles($scope.realm.slug, $scope.modUser.subjectId, data)
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
    .controller('RealmUserController', function ($scope, $state, $stateParams, RealmData, RealmUsers, RealmProviders, RealmAttributeSets, RealmGroups, RealmRoles, RealmServices, RoleSpaceData, Utils) {
        var slug = $stateParams.realmId;
        var subjectId = $stateParams.subjectId;
        $scope.curView = 'overview';


        $scope.aceOption = {
            mode: 'javascript',
            theme: 'monokai',
            maxLines: 40,
            minLines: 30
        };

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
                    //identities
                    var idps = $scope.idps;
                    var identities = data.identities;
                    if (identities) {
                        identities.forEach(i => {
                            i.providerId = i.provider;
                            i.provider = idps.get(i.providerId);
                            i.icon = iconIdp(i.provider);
                        });
                    } else {
                        identities = [];
                    }
                    $scope.identities = identities;
                    return data;
                })
                .then(function (data) {
                    //authorities
                    $scope.reloadAuthorities(data.authorities);
                    return data;
                })
                .then(function (data) {
                    //space roles
                    $scope.reloadSpaceRoles(data.spaceRoles);
                    return data;
                })
                .then(function (data) {
                    //attributes
                    $scope.reloadAttributes(data.attributes);
                    return data;
                })
                .then(function (data) {
                    $scope.reloadGroups(data.groups);
                    return;
                })                
                .then(function (data) {
                    $scope.reloadRoles(data.roles);
                    return;
                })
                .then(function () {
                    return RealmUsers.getApps(slug, subjectId);
                })
                .then(function (data) {
                    $scope.reloadApps(data);
                    return;
                })
                .then(function () {
                    return RealmUsers.getAudit(slug, subjectId);
                })
                .then(function (events) {
                    $scope.audit = events;
                    return;
                })
                .then(function () {
                    return RealmUsers.getApprovals(slug, subjectId);
                })
                .then(function (approvals) {
                    $scope.reloadApprovals(approvals, $scope.user.roles.map(r => r.role));
                    return;
                })
                .catch(function (err) {
                    Utils.showError('Failed to load realm user: ' + err.data.message);
                });
        }

        $scope.reload = function (data) {
            $scope.username = data.username;
            $scope.user = data;
        }

        /**
        * Initialize the app: load list of the users
        */
        var init = function () {
            $scope.systemRoles = ['ROLE_ADMIN', 'ROLE_DEVELOPER'];

            RealmProviders.getIdentityProviders(slug)
                .then(function (providers) {
                    var map = new Map(providers.map(p => [p.provider, p]));
                    $scope.idps = map;
                    return;
                })
                .then(function () {
                    return RealmProviders.getAttributeProviders(slug);
                })
                .then(function (providers) {
                    var map = new Map(providers.map(p => [p.provider, p]));
                    $scope.aps = map;
                    return;
                })
                .then(function () {
                    return RealmAttributeSets.getAttributeSets(slug);
                })
                .then(function (sets) {
                    var sMap = new Map();
                    sets.forEach(function (s) {
                        var ss = {
                            ...s,
                            map: new Map(s.attributes.map(e => [e.key, e]))
                        };
                        sMap.set(ss.identifier, ss);

                    });
                    $scope.attributeSets = sMap;
                    return;
                })
                .then(function () {
                    return RealmServices.getServices(slug);
                })
                .then(function (services) {
                    var sMap = new Map(services.map(e => [e.serviceId, e]));
                    $scope.services = sMap;
                })
                .then(function () {
                    return RealmGroups.getGroups(slug);
                })
                .then(function (groups) {
                    var gMap = new Map(groups.map(e => [e.group, e]));
                    $scope.realmGroups = gMap;
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
                    Utils.showError('Failed to load realm users: ' + err.data.message);
                });
        };


        init();


        $scope.deleteUserDlg = function () {
            $scope.doDelete = function () {
                $('#deleteConfirm').modal('hide');
                RealmUsers.removeUser(slug, subjectId).then(function () {
                    $state.go('realm.users', { realmId: $scope.realm.slug });
                    Utils.showSuccess();
                }).catch(function (err) {
                    Utils.showError(err.data.message);
                });
            }

            $('#deleteConfirm').modal({ keyboard: false });
        };

        $scope.blockUserDlg = function () {
            $scope.blockUser = function () {
                $('#blockConfirm').modal('hide');
                RealmUsers.blockUser(slug, subjectId)
                    .then(function (data) {
                        $scope.reload(data);
                        Utils.showSuccess();
                    }).catch(function (err) {
                        Utils.showError(err.data.message);
                    });
            }

            $('#blockConfirm').modal({ keyboard: false });
        }

        $scope.unblockUser = function () {
            RealmUsers.unblockUser(slug, subjectId)
                .then(function (data) {
                    $scope.reload(data);
                    Utils.showSuccess();
                }).catch(function (err) {
                    Utils.showError(err.data.message);
                });
        }

        $scope.lockUserDlg = function () {
            $scope.lockUser = function () {
                $('#lockConfirm').modal('hide');
                RealmUsers.lockUser(slug, subjectId)
                    .then(function (data) {
                        $scope.reload(data);
                        Utils.showSuccess();
                    }).catch(function (err) {
                        Utils.showError(err.data.message);
                    });
            }
            $('#lockConfirm').modal({ keyboard: false });
        }

        $scope.unlockUser = function () {
            RealmUsers.unlockUser(slug, subjectId)
                .then(function (data) {
                    $scope.reload(data);
                    Utils.showSuccess();
                }).catch(function (err) {
                    Utils.showError(err.data.message);
                });
        }

        $scope.verifyUserDlg = function () {
            $scope.verifyUser = function () {
                $('#verifyConfirm').modal('hide');
                RealmUsers.verifyUser(slug, subjectId)
                    .then(function (data) {
                        $scope.reload(data);
                        Utils.showSuccess();
                    }).catch(function (err) {
                        Utils.showError(err.data.message);
                    });
            }
            $('#verifyConfirm').modal({ keyboard: false });
        }

        $scope.unverifyUser = function () {
            RealmUsers.unverifyUser(slug, subjectId)
                .then(function (data) {
                    $scope.reload(data);
                    Utils.showSuccess();
                }).catch(function (err) {
                    Utils.showError(err.data.message);
                });
        }

        $scope.inspectDlg = function (obj) {
            $scope.modObj = obj;
            $scope.modObj.json = JSON.stringify(obj, null, 3);
            $('#inspectModal').modal({ keyboard: false });
        }


        $scope.reloadIdentities = function (data) {
            var idps = $scope.idps;
            var identities = [];
            if (data) {
                identities = data.map(i => {
                    return {
                        ...i,
                        providerId: i.provider,
                        provider: idps.get(i.providerId),
                        icon: iconIdp(i.provider)
                    }
                });
            }

            $scope.identities = identities;
        }


        /*
        * authorities
        */
        $scope.loadAuthorities = function () {

            RealmUsers.getAuthorities(slug, subjectId)
                .then(function (data) {
                    $scope.reloadAuthorities(data);
                })
                .catch(function (err) {
                    Utils.showError('Failed to load user roles: ' + err.data.message);
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

            //also update user model
            $scope.user.authorities = data;
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


                RealmUsers.updateAuthorities(slug, subjectId, authorities)
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
        * connected apps
        */
        $scope.loadApps = function () {
            RealmUsers.getApps(slug, subjectId)
                .then(function (data) {
                    $scope.reloadApps(data);
                })
                .catch(function (err) {
                    Utils.showError('Failed to load apps: ' + err.data.message);
                });

        }

        $scope.reloadApps = function (data) {
            //TODO load client details
            $scope.apps = data;
        }

        $scope.revokeApp = function (app) {
            if (app && app.clientId) {
                RealmUsers.revokeApp(slug, subjectId, app.clientId)
                    .then(function () {
                        $scope.loadApps();
                        Utils.showSuccess();
                    })
                    .catch(function (err) {
                        Utils.showError('Failed to load apps: ' + err.data.message);
                    });
            }
        }

        /*
        * realm groups
        */
        $scope.loadGroups = function () {
            RealmUsers.getRealmGroups(slug, subjectId)
                .then(function (data) {
                    $scope.reloadGroups(data);
                })
                .catch(function (err) {
                    Utils.showError('Failed to load user groups: ' + err.data.message);
                });
        }

        $scope.reloadGroups = function (data) {
            var realmGroups = $scope.realmGroups;
            var groups = data.map(r => {

                if (!realmGroups.has(r.group)) {
                    return null;
                }

                var { group, name, description, groupId } = realmGroups.get(r.group);
                return {
                    ...r,
                    group, name, description, groupId
                }
            })
            $scope.groups = groups;

            //flatten for display
            $scope._groups = groups.map(r => r.group);

            //also update user model
            $scope.user.groups = data;
        }

        $scope.removeGroup = function (group) {
            if (group.realm && group.realm != slug) {
                Utils.showError('Failed to remove group');
                return;
            }

            updateGroups(null, [group]);
        }


        $scope.addGroupDlg = function () {
            $scope.modGroup = {
                realm: slug,
                group: '',
                groups: Array.from($scope.realmGroups.values())
            };
            $('#groupsModal').modal({ keyboard: false });
            Utils.refreshFormBS(300);
        }

        $scope.addGroup = function () {
            $('#groupsModal').modal('hide');
            if ($scope.modGroup && $scope.modGroup.group) {
                var group = $scope.modGroup.group;

                updateGroups([group], null);
                $scope.modGroup = null;
            }
        }

        // save groups
        var updateGroups = function (groupsAdd, groupsRemove) {
            //map cur realm
            var curGroups = $scope.groups
                .map(a => a.group);

            var realmGroups = Array.from($scope.realmGroups.values()).map(r => r.group);

            //handle only same realm
            var groupsToAdd = [];
            if (groupsAdd) {
                groupsToAdd = groupsAdd.map(r => {
                    if (r.group) {
                        return r.group;
                    }
                    return r;
                }).filter(r => !curGroups.includes(r));
            }

            if (groupsToAdd.some(r => !realmGroups.includes(r))) {
                Utils.showError('Invalid groups');
                return;
            }

            var groupsToRemove = [];
            if (groupsRemove) {
                groupsToRemove = groupsRemove.map(r => {
                    if (r.group) {
                        return r.group;
                    }
                    return r;
                }).filter(r => curGroups.includes(r));
            }

            var keepGroups = curGroups.filter(r => !groupsToRemove.includes(r));
            var groups = keepGroups.concat(groupsToAdd);

            var data = groups.map(r => {
                return { 'realm': slug, 'group': r }
            });


            RealmUsers.updateRealmGroups(slug, subjectId, data)
                .then(function (data) {
                    $scope.reloadGroups(data);
                    return;
                })
                .then(function () {
                    return RealmUsers.getApprovals(slug, subjectId);
                })
                .then(function (data) {
                    $scope.reloadApprovals(data, $scope._groups);
                    Utils.showSuccess();
                })
                .catch(function (err) {
                    Utils.showError('Failed to update groups: ' + err.data.message);
                });


        }

        /*
        * realm roles
        */
        $scope.loadRoles = function () {
            RealmUsers.getRealmRoles(slug, subjectId)
                .then(function (data) {
                    $scope.reloadRoles(data);
                })
                .catch(function (err) {
                    Utils.showError('Failed to load user roles: ' + err.data.message);
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

            //also update user model
            $scope.user.roles = data;
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


            RealmUsers.updateRealmRoles(slug, subjectId, data)
                .then(function (data) {
                    $scope.reloadRoles(data);
                    return;
                })
                .then(function () {
                    return RealmUsers.getApprovals(slug, subjectId);
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
            RealmUsers.getSpaceRoles(slug, subjectId)
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

            //also update user model
            $scope.user.spaceRoles = data;
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

            RealmUsers.updateSpaceRoles(slug, subjectId, data)
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
            RealmUsers.getApprovals(slug, subjectId)
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
                scopes: scopes.filter(s => s.type == 'user' || s.type == 'generic').map(s => {
                    return {
                        ...s,
                        value: service.approvals.some(a => s.scope == a.scope),
                        locked: service.approvals.some(a => (s.scope == a.scope && a.role))

                    }
                }),
                clientId: subjectId,

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
                        clientId: subjectId,
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
        /*
        * attributes
        */
        $scope.loadAttributes = function () {
            RealmUsers.getUserAttributes(slug, subjectId)
                .then(function (data) {
                    $scope.reloadAttributes(data);
                })
                .catch(function (err) {
                    Utils.showError('Failed to load approvals: ' + err.data.message);
                });
        }


        $scope.reloadAttributes = function (data) {
            //attributes
            var providers = $scope.aps;
            var attributeSets = $scope.attributeSets;
            var attributes = [];

            if (data) {
                attributes = data.map(a => {
                    var set = $scope.attributeSets.get(a.identifier);
                    var models = new Map(set.attributes.map(a => [a.key, a]));
                    return {
                        ...a,
                        set,
                        attributes: a.attributes.map(at => {
                            var m = models.get(at.key);
                            if (!m) {
                                return at;
                            }

                            return {
                                ...at,
                                name: m.name ? m.name : at.key,
                                description: m.description
                            }
                        })
                    }
                });
            }

            $scope.attributes = attributes;

            //also extract editable attributes
            var editable = [];
            //only internal for now
            //TODO handle different providers..
            providers
                .forEach(ap => {
                    var { authority, provider, realm } = ap;
                    if (ap.authority == 'internal' && ap.attributeSets) {
                        ap.attributeSets.forEach(s => {
                            //if not present add as editable via this provider
                            var af = attributes.filter(a =>
                                (a.provider == ap.provider && a.identifier == s));
                            if (af.length == 0) {
                                var set = attributeSets.get(s);
                                var models = new Map(set.attributes.map(a => [a.key, a]));

                                var { identifier, name, description } = set;

                                var ua = {
                                    authority, provider, realm, set,
                                    userId: subjectId,
                                    attributesId: null,
                                    identifier,
                                    attributes: set.attributes.map(at => {
                                        var m = models.get(at.key);

                                        return {
                                            ...at,
                                            name: m.name ? m.name : at.key,
                                            description: m.description,
                                            value: null
                                        }
                                    }),
                                    name, description
                                }
                                editable.push(ua);
                            }

                        });
                    }
                });
            $scope.editAttributes = editable;

            //also update user model
            $scope.user.attributes = data;

        }

        $scope.editAttributesDlg = function (attributes) {
            //build form content
            $scope.modAttributes = {
                ...attributes,
                attributes: attributes.attributes.map(at => {
                    if (at.value && at.type === 'object') at.value = JSON.stringify(at.value);
                    return {
                        ...at,
                        field: attributeField(at)
                    }
                }),
            }
            $('#editAttributesDlg').modal({ keyboard: false });
            Utils.refreshFormBS(300);
        }


        $scope.addOrUpdateAttributes = function () {
            $('#editAttributesDlg').modal('hide');
            if ($scope.modAttributes) {
                //build attribute dto
                var attributes = {};
                $scope.modAttributes.attributes.forEach(a => {
                    if (a.type === 'object') a.value = a.value ? JSON.parse(a.value) : null;
                    attributes[a.key] = a.value;
                });

                var data = {
                    identifier: $scope.modAttributes.identifier,
                    provider: $scope.modAttributes.provider,
                    attributes
                };

                RealmUsers.setUserAttributesSet(slug, subjectId, data)
                    .then(function () {
                        $scope.loadAttributes();
                        Utils.showSuccess();
                    })
                    .catch(function (err) {
                        Utils.showError('Failed to update attributes: ' + err.data.message);
                    });


                $scope.modAttributes = null;
            }
        }

        $scope.viewAttributes = function (attributes) {
            $scope.modAttributes = {
                ...attributes,
                attributeSet: $scope.attributeSets[attributes.identifier]
            };
            $('#viewAttributesDlg').modal({ keyboard: false });
        }

        $scope.deleteAttributesDlg = function (attributes) {
            $scope.doDelete = function () {
                $('#deleteConfirm').modal('hide');
                var attributes = $scope.modAttributes;
                RealmUsers.deleteUserAttributesSet(slug, subjectId, attributes)
                    .then(function () {
                        $scope.loadAttributes();
                        Utils.showSuccess();
                    })
                    .catch(function (err) {
                        Utils.showError('Failed to update attributes: ' + err.data.message);
                    });

                $scope.modAttributes = null;
            }
            $scope.modAttributes = attributes;
            $('#deleteConfirm').modal({ keyboard: false });
        };

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

        var attributeField = function (attribute) {

            if ("date" == attribute.type) {
                return "date";
            } else if ("datetime" == attribute.type) {
                return "datetime-local";
            } else if ("time" == attribute.type) {
                return "time";
            } else if ("number" == attribute.type) {
                return "number";
            } else if ("boolean" == attribute.type) {
                return "checkbox";
            }

            if ("string" == attribute.type && attribute.key.toLowerCase().startsWith("email")) {
                return "email";
            }


            return 'text';
        }
    })
    ;