angular.module('aac.controllers.realmroles', [])

    .service('RealmRoles', function ($q, $http) {
        var service = {};


        service.getRoles = function (realm) {
            return $http.get('console/dev/realms/' + realm + '/roles').then(function (data) {
                return data.data;
            });
        }
        service.getRole = function (realm, roleId) {
            return $http.get('console/dev/realms/' + realm + '/roles/' + roleId).then(function (data) {
                return data.data;
            });
        }

        service.addRole = function (realm, role) {
            return $http.post('console/dev/realms/' + realm + '/roles', role).then(function (data) {
                return data.data;
            });
        }
        service.updateRole = function (realm, role) {
            return $http.put('console/dev/realms/' + realm + '/roles/' + role.roleId, role).then(function (data) {
                return data.data;
            });
        }
        service.deleteRole = function (realm, roleId) {
            return $http.delete('console/dev/realms/' + realm + '/roles/' + roleId).then(function (data) {
                return data.data;
            });
        }
        service.importRole = function (realm, file) {
            var fd = new FormData();
            fd.append('file', file);
            return $http({
                url: 'console/dev/realms/' + realm + '/roles',
                headers: { "Content-Type": undefined }, //set undefined to let $http manage multipart declaration with proper boundaries
                data: fd,
                method: "PUT"
            }).then(function (data) {
                return data.data;
            });
        }
        service.exportRole = function (realm, roleId) {
            window.open('console/dev/realms/' + realm + '/roles/' + roleId + '/yaml');
        }
        service.getApprovals = function (slug, roleId) {
            return $http.get('console/dev/realms/' + slug + '/roles/' + roleId + '/approvals').then(function (data) {
                return data.data;
            });
        }
        return service;
    })


    .controller('RealmRolesController', function ($scope, $state, $stateParams, RealmRoles, Utils) {
        var slug = $stateParams.realmId;

        $scope.load = function () {

            RealmRoles.getRoles(slug)
                .then(function (data) {
                    $scope.roles = data;
                })
                .catch(function (err) {
                    Utils.showError('Failed to load realm roles: ' + err.data.message);
                });

        };

        var init = function () {
            $scope.load();
        };


        $scope.createRoleDlg = function () {
            $scope.modRole = {
                realm: slug,
                role: null,

            }
            $('#createRoleModal').modal({ keyboard: false });
        }

        $scope.createRole = function () {
            $('#createRoleModal').modal('hide');


            if ($scope.modRole) {
                var data = {
                    ...$scope.modRole,
                    realm: slug
                }
                RealmRoles.addRole(slug, data)
                    .then(function (res) {
                        $scope.load();
                        Utils.showSuccess();
                    })
                    .catch(function (err) {
                        Utils.showError(err.data.message);
                    });
            }
        }


        $scope.importRoleDlg = function () {
            $('#importRoleModal').modal({ keyboard: false });
        }


        $scope.importRole = function () {
            $('#importRoleModal').modal('hide');
            var file = $scope.importFile;
            var resetID = !!file.resetID;
            var mimeTypes = ['text/yaml', 'text/yml', 'application/x-yaml'];
            if (file == null || !mimeTypes.includes(file.type) || file.size == 0) {
                Utils.showError("invalid file");
            } else {
                RealmRoles.importRole(slug, file, resetID)
                    .then(function (res) {
                        $scope.importFile = null;
                        $scope.load();
                        Utils.showSuccess();
                    })
                    .catch(function (err) {
                        Utils.showError(err.data.message);
                    });
            }
        }

        $scope.exportRole = function (role) {
            if (role) {
                RealmRoles.exportRole(slug, role.roleId);
            }
        }

        $scope.deleteRoleDlg = function (role) {
            $scope.modRole = role;
            //add confirm field
            $scope.modRole.confirmId = '';
            $('#deleteRoleModal').modal({ keyboard: false });
        }

        $scope.deleteRole = function () {
            $('#deleteRoleModal').modal('hide');
            if ($scope.modRole.roleId === $scope.modRole.confirmId) {
                RealmRoles.deleteRole(slug, $scope.modRole.roleId).then(function () {
                    $scope.load();
                    Utils.showSuccess();
                }).catch(function (err) {
                    Utils.showError(err.data.message);
                });
            } else {
                Utils.showError("confirmId not valid");
            }
        }

        init();

        $scope.copyText = function (txt) {
            var textField = document.createElement('textarea');
            textField.innerText = txt;
            document.body.appendChild(textField);
            textField.select();
            document.execCommand('copy');
            textField.remove();
        }

    })

    .controller('RealmRoleController', function ($scope, $state, $stateParams, RealmRoles, RealmServices, Utils) {
        var slug = $stateParams.realmId;
        var roleId = $stateParams.roleId;

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



        $scope.load = function () {

            RealmRoles.getRole(slug, roleId)
                .then(function (data) {
                    $scope.reload(data);
                    return data;
                })
                .then(function () {
                    return RealmRoles.getApprovals(slug, roleId)
                })
                .then(function (data) {
                    $scope.reloadApprovals(data);
                })
                .catch(function (err) {
                    Utils.showError('Failed to load realm role: ' + err.data.message);
                });

        };

        var init = function () {
            RealmServices.getServices(slug)
                .then(function (services) {
                    var sMap = new Map(services.map(e => [e.serviceId, e]));
                    $scope.services = sMap;
                })
                .then(function () {
                    $scope.load();
                })
                .catch(function (err) {
                    Utils.showError('Failed to load realm role: ' + err.data.message);
                });

        };

        $scope.reload = function (data) {
            $scope.rolename = data.name;
            $scope.roledescription = data.description;
            $scope.role = data;
        }

        $scope.loadApprovals = function () {
            RealmRoles.getApprovals(slug, roleId)
                .then(function (data) {
                    $scope.reloadApprovals(data);
                })
                .catch(function (err) {
                    Utils.showError('Failed to load realm role: ' + err.data.message);
                });
        }

        $scope.reloadApprovals = function (data) {
            var services = $scope.services;

            var approvals = data
                .map(a => {
                    if (!services.has(a.userId)) {
                        return null;
                    }

                    return {
                        ...a,
                        service: services.get(a.userId)
                    }
                })
                .filter(a => !!a);

            $scope.approvals = approvals;

        }

        $scope.exportRole = function () {
            RealmRoles.exportRole(slug, roleId);
        }

        $scope.saveRole = function () {
            var role = $scope.role;

            //save only basic settings
            var data = {
                realm: slug,
                roleId: roleId,
                role: role.role,
                name: role.name,
                description: role.description,
                permissions: role.permissions
            };

            RealmRoles.updateRole(slug, data)
                .then(function (res) {
                    Utils.showSuccess();
                    $scope.reload(res);
                })
                .catch(function (err) {
                    Utils.showError('Failed to save service: ' + err.data.message);
                });
        }


        $scope.deleteRole = function () {
            $scope.doDelete = function () {
                $('#deleteConfirm').modal('hide');
                RealmRoles.deleteRole(slug, roleId)
                    .then(function () {
                        Utils.showSuccess();
                        $state.go('realm.roles', { realmId: $stateParams.realmId });
                    })
                    .catch(function (err) {
                        Utils.showError('Failed to delete role: ' + err.data.message);
                    });
            }
            $('#deleteConfirm').modal({ keyboard: false });
        };


        init();

        $scope.copyText = function (txt) {
            var textField = document.createElement('textarea');
            textField.innerText = txt;
            document.body.appendChild(textField);
            textField.select();
            document.execCommand('copy');
            textField.remove();
        }

    })

    ;