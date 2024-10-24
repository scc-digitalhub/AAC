angular.module('aac.controllers.realmroles', [])

    .service('RealmRoles', function ($http) {
        var service = {};

        service.getRoles = function (realm) {
            return $http.get('console/dev/roles/' + realm + '?page=0&size=200').then(function (data) {
                return data.data.content;
            });
        }
        service.getRole = function (realm, roleId) {
            return $http.get('console/dev/roles/' + realm + '/' + roleId).then(function (data) {
                return data.data;
            });
        }

        service.addRole = function (realm, role) {
            return $http.post('console/dev/roles/' + realm, role).then(function (data) {
                return data.data;
            });
        }
        service.updateRole = function (realm, role) {
            return $http.put('console/dev/roles/' + realm + '/' + role.roleId, role).then(function (data) {
                return data.data;
            });
        }
        service.deleteRole = function (realm, roleId) {
            return $http.delete('console/dev/roles/' + realm + '/' + roleId).then(function (data) {
                return data.data;
            });
        }
        service.importRole = function (realm, file, yaml, reset) {
            var fd = new FormData();
            if (yaml) {
                fd.append('yaml', yaml);
            }
            if (file) {
                fd.append('file', file);
            }
            return $http({
                url: 'console/dev/roles/' + realm + (reset ? "?reset=true" : ""),
                headers: { "Content-Type": undefined }, //set undefined to let $http manage multipart declaration with proper boundaries
                data: fd,
                method: "PUT"
            }).then(function (data) {
                return data.data;
            });
        }
        service.exportRole = function (realm, roleId) {
            window.open('console/dev/roles/' + realm + '/' + roleId + '/export');
        }
        service.getApprovals = function (slug, roleId) {
            return $http.get('console/dev/roles/' + slug + '/' + roleId + '/approvals').then(function (data) {
                return data.data;
            });
        }
        service.setApprovals = function (slug, roleId, scopes) {
            return $http.put('console/dev/roles/' + slug + '/' + roleId + '/approvals', scopes).then(function (data) {
                return data.data;
            });
        }
        service.getRoleSubjects = function (realm, roleId) {
            return $http.get('console/dev/roles/' + realm + '/' + roleId + '/subjects').then(function (data) {
                return data.data;
            });
        }

        service.addRoleSubject = function (realm, roleId, subjectId) {
            return $http.put('console/dev/roles/' + realm + '/' + roleId + '/subjects/' + subjectId).then(function (data) {
                return data.data;
            });
        }
        service.removeRoleSubject = function (realm, roleId, subjectId) {
            return $http.delete('console/dev/roles/' + realm + '/' + roleId + '/subjects/' + subjectId).then(function (data) {
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
                    .then(function () {
                        $scope.load();
                        Utils.showSuccess();
                    })
                    .catch(function (err) {
                        Utils.showError(err.data.message);
                    });
            }
        }


        $scope.importRoleDlg = function () {
            if ($scope.importFile) {
                $scope.importFile.file = null;
            }

            $('#importRoleModal').modal({ keyboard: false });
        }


        $scope.importRole = function () {
            $('#importRoleModal').modal('hide');
            var file = $scope.importFile.file;
            var yaml = $scope.importFile.yaml;
            var resetID = !!$scope.importFile.resetID;
            var mimeTypes = ['text/yaml', 'text/yml', 'application/x-yaml'];
            if (!yaml && (file == null || !mimeTypes.includes(file.type) || file.size == 0)) {
                Utils.showError("invalid file");
            } else {
                RealmRoles.importRole(slug, file, yaml, resetID)
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

    .controller('RealmRoleController', function ($scope, $state, $stateParams, RealmRoles, RealmServices, RealmData, Utils) {
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
                .then(function () {
                    $scope.loadRoleSubjects();
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

            var approvals = Array.from(services.values()).map(s => {
                var { serviceId, namespace, realm, name, description } = s;

                //var sMap = new Map(scopes.map(e => [e.scope, e]));

                return {
                    serviceId, namespace, realm, name, description,
                    approvals: data.filter(a => a.userId == s.serviceId)
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
                scopes: scopes.map(s => {
                    return {
                        ...s,
                        value: service.approvals.some(a => s.scope == a.scope)
                    }
                }),
                clientId: roleId,

            }
            $('#permissionsModal').modal({ keyboard: false });
        }

        $scope.updatePermissions = function () {
            $('#permissionsModal').modal('hide');


            if ($scope.modApprovals) {
                var scopes = $scope.modApprovals.scopes.map(s => s.scope);
                var approved = $scope.modApprovals.scopes.filter(s => s.value).map(s => s.scope);

                var toKeep = $scope.permissions.filter(s => !scopes.includes(s));
                var toSet = toKeep.concat(approved);

                RealmRoles.setApprovals(slug, roleId, toSet)
                    .then(function (data) {
                        $scope.reloadApprovals(data);
                        Utils.showSuccess();
                    })
                    .catch(function (err) {
                        Utils.showError('Failed to update realm role: ' + err.data.message);
                    });
            }
        }

        $scope.searchSubjects = function () {
            var keywords = $scope.modRoleSubject.search;
            if (keywords && keywords.length > 2) {
                var params = {
                    t: "user,client,group",
                    q: keywords
                };

                RealmData.getSubjects(slug, params)
                    .then(function (data) {
                        $scope.modRoleSubject.results = data;
                    })
                    .catch(function (err) {
                        Utils.showError('Error while searching: ' + err.data.message);
                    });
            }
        }

        $scope.loadRoleSubjects = function () {
            RealmRoles.getRoleSubjects(slug, roleId)
                .then(function (data) {
                    $scope.reloadRoleSubjects(data);
                })
                .catch(function (err) {
                    Utils.showError('Failed to load realm role subjects: ' + err.data.message);
                });
        };

        $scope.reloadRoleSubjects = function (data) {
            //$scope.subjects = data;
            var ids = new Set(data);

            //resolve subjects
            Promise
                .all(
                    Array.from(ids).map(id => {
                        return RealmData.getSubject(slug, id);
                    })
                )
                .then(function (subjects) {
                    //add icon
                    var list = subjects.map(s => {
                        return {
                            icon: iconProvider(s.type),
                            ...s
                        };
                    });
                    $scope.subjects = list;
                })
                .catch(function (err) {
                    Utils.showError('Failed to load role subjects: ' + err.data.message);
                });

        };

        $scope.createRoleSubjectDlg = function () {
            $scope.modRoleSubject = {
                subjectId: null,
                search: null,
                results: null
            };
            $('#roleSubjectModal').modal({ keyboard: false });
            Utils.refreshFormBS();
        }

        $scope.saveRoleSubject = function () {
            if ($scope.modRoleSubject) {
                RealmRoles.addRoleSubject(slug, roleId, $scope.modRoleSubject.subjectId)
                    .then(function () {
                        Utils.showSuccess();
                        $scope.loadRoleSubjects();
                    })
                    .catch(function (err) {
                        Utils.showError('Failed to save role subject: ' + err.data.message);
                    });
            }

            $('#roleSubjectModal').modal('hide');
        }

        $scope.removeRoleSubject = function (subject) {
            $scope.doDelete = function () {
                $('#deleteConfirm').modal('hide');
                RealmRoles.removeRoleSubject(slug, roleId, subject.subjectId)
                    .then(function () {
                        Utils.showSuccess();
                        $scope.loadRoleSubjects();
                    })
                    .catch(function (err) {
                        Utils.showError('Failed to remove role subject: ' + err.data.message);
                    });
            }
            $('#deleteConfirm').modal({ keyboard: false });
        };

        var iconProvider = function (type) {
            var icon = './italia/svg/sprite.svg#it-user';
            if (type == 'client') {
                icon = './italia/svg/sprite.svg#it-piattaforme';
            }
            if (type == 'group') {
                icon = './italia/svg/sprite.svg#it-open-source';
            }
            return icon;
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

    ;