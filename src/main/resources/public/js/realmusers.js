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

        rService.getRoles = function (slug, subject) {
            return $http.get('console/dev/realms/' + slug + '/users/' + subject + '/roles').then(function (data) {
                return data.data;
            });
        }
        rService.updateRoles = function (slug, subject, roles) {
            return $http.put('console/dev/realms/' + slug + '/users/' + subject + '/roles', { roles: roles }).then(function (data) {
                return data.data;
            });
        }

        rService.blockUser = function (slug, subject) {
            return $http.put('console/dev/realms/' + slug + '/users/' + subject + '/block').then(function (data) {
                return data.data;
            });
        }

        rService.unblockUser = function (slug, subject) {
            return $http.delete('console/dev/realms/' + slug + '/users/' + subject + '/block').then(function (data) {
                return data.data;
            });
        }

        rService.lockUser = function (slug, subject) {
            return $http.put('console/dev/realms/' + slug + '/users/' + subject + '/lock').then(function (data) {
                return data.data;
            });
        }

        rService.unlockUser = function (slug, subject) {
            return $http.delete('console/dev/realms/' + slug + '/users/' + subject + '/lock').then(function (data) {
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
        rService.setUserAttributes = function (slug, user, attributes) {
            return $http.post('console/dev/realms/' + slug + '/users/' + user.subjectId + '/attributes', attributes).then(function (data) {
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

                RealmUsers.updateRoles($scope.realm.slug, $scope.modUser.subjectId, data)
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
    .controller('RealmUserController', function ($scope, $state, $stateParams, RealmData, RealmUsers, RealmProviders, RealmAttributeSets, Utils) {
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
                    $scope.authorities = data.authorities;
                    return data;
                })
                .then(function (data) {
                    //attributes
                    var aps = $scope.aps;
                    var attributes = data.attributes;
                    if (attributes) {
                        attributes.forEach(a => {
                            a.providerId = a.provider;
                            a.provider = aps.get(a.providerId);
                            a.attributes = a.attributes.map(at => {
                                return {
                                    ...at,
                                    field: attributeField(at),
                                    name: at.name ? at.name : at.key,
                                }
                            });
                        });
                    } else {
                        attributes = [];
                    }
                    $scope.attributes = attributes;

                    //also extract editable attributes
                    var attributeSets = $scope.attributeSets;
                    var editable = [];
                    //only internal for now
                    //TODO handle different providers..
                    aps
                        .forEach(ap => {
                            if (ap.authority == 'internal' && ap.attributeSets) {
                                ap.attributeSets.forEach(s => {
                                    //if not present add as editable via this provider
                                    var af = attributes.filter(a =>
                                        (a.providerId == ap.provider && a.identifier == s));
                                    if (af.length == 0) {
                                        var set = attributeSets.get(s);

                                        var ua = {
                                            authority: ap.authority,
                                            providerId: ap.provider,
                                            provider: ap,
                                            userId: subjectId,
                                            setId: null,
                                            identifier: set.identifier,
                                            attributes: set.attributes.map(at => {
                                                return {
                                                    ...at,
                                                    field: attributeField(at),
                                                    name: at.name ? at.name : at.key,
                                                }
                                            }),
                                            name: set.name ? set.name : set.identifier,
                                            description: set.description
                                        }
                                        editable.push(ua);
                                    }

                                });
                            }
                        });

                    $scope.editAttributes = editable;

                    return data;
                })
                .then(function () {
                    return RealmUsers.getRoles(slug, subjectId);
                })
                .then(function (roles) {
                    $scope.roles = roles;
                    return;
                })
                .catch(function (err) {
                    Utils.showError('Failed to load realm user: ' + err.data.message);
                });
        }

        $scope.reload = function (data) {
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
                    $scope.load();
                })
                .catch(function (err) {
                    Utils.showError('Failed to load realm users: ' + err.data.message);
                });
        };


        init();

        $scope.deleteUserDlg = function (user) {
            $scope.modUser = user;
            $('#deleteConfirm').modal({ keyboard: false });
        }

        $scope.deleteUser = function () {
            $('#deleteConfirm').modal('hide');
            RealmUsers.removeUser($scope.realm.slug, $scope.modUser).then(function () {
                $state.go('realm.users', { realmId: $scope.realm.slug });
                Utils.showSuccess();
            }).catch(function (err) {
                Utils.showError(err.data.message);
            });
        }


        $scope.blockUserDlg = function (user) {
            $scope.modUser = user;
            $('#blockConfirm').modal({ keyboard: false });
        }

        $scope.blockUser = function () {
            $('#blockConfirm').modal('hide');
            RealmUsers.blockUser($scope.realm.slug, $scope.modUser.subjectId)
                .then(function (data) {
                    $scope.reload(data);
                    Utils.showSuccess();
                }).catch(function (err) {
                    Utils.showError(err.data.message);
                });
        }

        $scope.unblockUser = function (user) {
            RealmUsers.unblockUser($scope.realm.slug, user.subjectId)
                .then(function (data) {
                    $scope.reload(data);
                    Utils.showSuccess();
                }).catch(function (err) {
                    Utils.showError(err.data.message);
                });
        }

        $scope.lockUserDlg = function (user) {
            $scope.modUser = user;
            $('#lockConfirm').modal({ keyboard: false });
        }

        $scope.lockUser = function () {
            $('#lockConfirm').modal('hide');
            RealmUsers.lockUser($scope.realm.slug, $scope.modUser.subjectId)
                .then(function (data) {
                    $scope.reload(data);
                    Utils.showSuccess();
                }).catch(function (err) {
                    Utils.showError(err.data.message);
                });
        }

        $scope.unlockUser = function (user) {
            RealmUsers.unlockUser($scope.realm.slug, user.subjectId)
                .then(function (data) {
                    $scope.reload(data);
                    Utils.showSuccess();
                }).catch(function (err) {
                    Utils.showError(err.data.message);
                });
        }


        $scope.loadRoles = function () {
            RealmUsers.getRoles(slug, subjectId)
                .then(function (data) {
                    $scope.roles = data;
                })
                .catch(function (err) {
                    Utils.showError('Failed to load user roles: ' + err.data.message);
                });
        }


        $scope.removeRole = function (r) {
            if (r.realm && r.realm != slug) {
                Utils.showError('Failed to remove role');
                return;
            }

            updateRoles(null, [r]);
        }

        $scope.manageSystemRolesDlg = function () {
            var authorities = $scope.user.authorities
                .filter(a => a.realm && slug == a.realm)
                .map(a => a.role);

            $scope.modSystemRoles = {
                realm: slug,
                admin: authorities.includes('ROLE_ADMIN'),
                developer: authorities.includes('ROLE_DEVELOPER')
            };
            $('#systemRolesModal').modal({ keyboard: false });
        }

        $scope.updateSystemRoles = function () {
            $('#systemRolesModal').modal('hide');
            if ($scope.modSystemRoles) {
                var systemRoles = $scope.modSystemRoles;
                var rolesAdd = [];
                var rolesRemove = [];

                if (systemRoles.admin === true) {
                    rolesAdd.push('ROLE_ADMIN');
                } else {
                    rolesRemove.push('ROLE_ADMIN');
                }
                if (systemRoles.developer === true) {
                    rolesAdd.push('ROLE_DEVELOPER');
                } else {
                    rolesRemove.push('ROLE_DEVELOPER');
                }

                updateRoles(rolesAdd, rolesRemove);
            }
        }

        $scope.addRoleDlg = function () {
            $scope.modRole = {
                realm: slug,
                role: ''
            };
            $('#rolesModal').modal({ keyboard: false });
        }

        $scope.addRole = function () {
            $('#rolesModal').modal('hide');
            console.log($scope.modRole);
            if ($scope.modRole && $scope.modRole.role) {
                var r = $scope.modRole.role;

                updateRoles([r], null);
                $scope.modRole = null;
            }
        }

        // save roles
        var updateRoles = function (rolesAdd, rolesRemove) {
            console.log("update roles", rolesAdd, rolesRemove);
            //map cur realm
            var curAuthorities = $scope.roles
                .map(a => a.role);

            //handle only same realm
            var rolesToAdd = [];
            if (rolesAdd) {
                rolesToAdd = rolesAdd.map(r => {
                    if (r.role) {
                        return r.role;
                    }
                    return r;
                }).filter(r => !curAuthorities.includes(r));
            }

            var rolesToRemove = [];
            if (rolesRemove) {
                rolesToRemove = rolesRemove.map(r => {
                    if (r.role) {
                        return r.role;
                    }
                    return r;
                }).filter(r => curAuthorities.includes(r));
            }

            var keepRoles = curAuthorities.filter(r => !rolesToRemove.includes(r));
            var roles = keepRoles.concat(rolesToAdd);

            var data = roles.map(r => {
                return { 'realm': slug, 'role': r }
            });


            RealmUsers.updateRoles($scope.realm.slug, $scope.user.subjectId, data)
                .then(function () {
                    $scope.loadRoles();
                    Utils.showSuccess();
                })
                .catch(function (err) {
                    Utils.showError('Failed to update roles: ' + err.data.message);
                });


        }


        $scope.inspectDlg = function (obj) {
            $scope.modObj = obj;
            $scope.modObj.json = JSON.stringify(obj, null, 3);
            $('#inspectModal').modal({ keyboard: false });
        }





        $scope.editAttributesDlg = function (attributes) {
            //build form content
            $scope.modAttributes = {
                ...attributes,
            }
            console.log(attributes);
            $('#editAttributesDlg').modal({ keyboard: false });
        }


        $scope.addOrUpdateAttributes = function () {
            $('#editAttributesDlg').modal('hide');
            if ($scope.modAttributes) {
                console.log($scope.modAttributes);
                //build attribute dto
                var attributes = {
                    identifier: $scope.modAttributes.identifier,
                    provider: $scope.modAttributes.providerId,
                    attributes: $scope.modAttributes.attributes.map(a => {
                        return {
                            key: a.key,
                            value: a.value
                        }
                    })
                };

                console.log(attributes);
                RealmUsers.setUserAttributes($scope.realm.slug, $scope.user, attributes)
                    .then(function () {
                        $scope.load();
                        Utils.showSuccess();
                    })
                    .catch(function (err) {
                        Utils.showError('Failed to update roles: ' + err.data.message);
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