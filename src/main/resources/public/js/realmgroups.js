angular.module('aac.controllers.realmgroups', [])

    .service('RealmGroups', function ($http) {
        var service = {};

        service.getGroups = function (realm) {
            return $http.get('console/dev/groups/' + realm + '?page=0&size=200').then(function (data) {
                return data.data.content;
            });
        }
        service.getGroup = function (realm, groupId) {
            return $http.get('console/dev/groups/' + realm + '/' + groupId).then(function (data) {
                return data.data;
            });
        }

        service.addGroup = function (realm, group) {
            return $http.post('console/dev/groups/' + realm, group).then(function (data) {
                return data.data;
            });
        }
        service.updateGroup = function (realm, group) {
            return $http.put('console/dev/groups/' + realm + '/' + group.groupId, group).then(function (data) {
                return data.data;
            });
        }
        service.deleteGroup = function (realm, groupId) {
            return $http.delete('console/dev/groups/' + realm + '/' + groupId).then(function (data) {
                return data.data;
            });
        }
        service.importGroup = function (realm, file, yaml, reset) {
            var fd = new FormData();
            if (yaml) {
                fd.append('yaml', yaml);
            }
            if (file) {
                fd.append('file', file);
            }
            return $http({
                url: 'console/dev/groups/' + realm + (reset ? "?reset=true" : ""),
                headers: { "Content-Type": undefined }, //set undefined to let $http manage multipart declaration with proper boundaries
                data: fd,
                method: "PUT"
            }).then(function (data) {
                return data.data;
            });
        }
        service.exportGroup = function (realm, groupId) {
            window.open('console/dev/groups/' + realm + '/' + groupId + '/export');
        }

        service.getGroupMembers = function (realm, groupId) {
            return $http.get('console/dev/groups/' + realm + '/' + groupId + '/members').then(function (data) {
                return data.data;
            });
        }

        service.addGroupMember = function (realm, groupId, subjectId) {
            return $http.put('console/dev/groups/' + realm + '/' + groupId + '/members/' + subjectId).then(function (data) {
                return data.data;
            });
        }
        service.removeGroupMember = function (realm, groupId, subjectId) {
            return $http.delete('console/dev/groups/' + realm + '/' + groupId + '/members/' + subjectId).then(function (data) {
                return data.data;
            });
        }

        service.getGroupRoles = function (realm, groupId) {
            return $http.get('console/dev/groups/' + realm + '/' + groupId + '/roles').then(function (data) {
                return data.data;
            });
        }
        service.updateGroupRoles = function (realm, groupId, roles) {
            return $http.put('console/dev/groups/' + realm + '/' + groupId + '/roles', roles).then(function (data) {
                return data.data;
            });
        }
        return service;
    })
    .controller('RealmGroupsController', function ($scope, $state, $stateParams, RealmGroups, Utils) {
        var slug = $stateParams.realmId;

        $scope.load = function () {

            RealmGroups.getGroups(slug)
                .then(function (data) {
                    $scope.groups = data;
                })
                .catch(function (err) {
                    Utils.showError('Failed to load realm groups: ' + err.data.message);
                });

        };

        var init = function () {
            $scope.load();
        };


        $scope.createGroupDlg = function () {
            $scope.modGroup = {
                realm: slug,
                group: null,

            }
            $('#createGroupModal').modal({ keyboard: false });
        }

        $scope.createGroup = function () {
            $('#createGroupModal').modal('hide');


            if ($scope.modGroup) {
                var data = {
                    ...$scope.modGroup,
                    realm: slug
                }
                RealmGroups.addGroup(slug, data)
                    .then(function () {
                        $scope.load();
                        Utils.showSuccess();
                    })
                    .catch(function (err) {
                        Utils.showError(err.data.message);
                    });
            }
        }


        $scope.importGroupDlg = function () {
            if ($scope.importFile) {
                $scope.importFile.file = null;
            }

            $('#importGroupModal').modal({ keyboard: false });
        }


        $scope.importGroup = function () {
            $('#importGroupModal').modal('hide');
            var file = $scope.importFile.file;
            var yaml = $scope.importFile.yaml;
            var resetID = !!$scope.importFile.resetID;
            var mimeTypes = ['text/yaml', 'text/yml', 'application/x-yaml'];
            if (!yaml && (file == null || !mimeTypes.includes(file.type) || file.size == 0)) {
                Utils.showError("invalid file");
            } else {
                RealmGroups.importGroup(slug, file, yaml, resetID)
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

        $scope.exportGroup = function (group) {
            if (group) {
                RealmGroups.exportGroup(slug, group.groupId);
            }
        }

        $scope.deleteGroupDlg = function (group) {
            $scope.modGroup = group;
            //add confirm field
            $scope.modGroup.confirmId = '';
            $('#deleteGroupModal').modal({ keyboard: false });
        }

        $scope.deleteGroup = function () {
            $('#deleteGroupModal').modal('hide');
            if ($scope.modGroup.groupId === $scope.modGroup.confirmId) {
                RealmGroups.deleteGroup(slug, $scope.modGroup.groupId).then(function () {
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

    .controller('RealmGroupController', function ($scope, $state, $stateParams, RealmGroups, RealmData, RealmRoles, Utils) {
        var slug = $stateParams.realmId;
        var groupId = $stateParams.groupId;

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

            RealmGroups.getGroup(slug, groupId)
                .then(function (data) {
                    $scope.reload(data);
                    return data;
                })
                .then(function () {
                    $scope.loadRoles();
                })
                .then(function () {
                    $scope.loadMembers();
                })
                .catch(function (err) {
                    Utils.showError('Failed to load realm group: ' + err.data.message);
                });

        };

        var init = function () {
            RealmRoles.getRoles(slug)
                .then(function (roles) {
                    var rMap = new Map(roles.map(e => [e.role, e]));
                    $scope.realmRoles = rMap;
                })
                .then(function () {
                    $scope.load();
                })
                .catch(function (err) {
                    Utils.showError('Failed to load realm group: ' + err.data.message);
                });
        };

        $scope.reload = function (data) {
            $scope.groupname = data.name;
            $scope.groupdescription = data.description;
            $scope.group = data;
        }

        $scope.exportGroup = function () {
            RealmGroups.exportGroup(slug, groupId);
        }

        $scope.saveGroup = function () {
            var group = $scope.group;

            //save only basic settings
            var data = {
                realm: slug,
                groupId: groupId,
                group: group.group,
                name: group.name,
                description: group.description,
                parentGroup: group.parentGroup
            };

            RealmGroups.updateGroup(slug, data)
                .then(function (res) {
                    Utils.showSuccess();
                    $scope.reload(res);
                })
                .catch(function (err) {
                    Utils.showError('Failed to save group: ' + err.data.message);
                });
        }


        $scope.deleteGroup = function () {
            $scope.doDelete = function () {
                $('#deleteConfirm').modal('hide');
                RealmGroups.deleteGroup(slug, groupId)
                    .then(function () {
                        Utils.showSuccess();
                        $state.go('realm.groups', { realmId: $stateParams.realmId });
                    })
                    .catch(function (err) {
                        Utils.showError('Failed to delete group: ' + err.data.message);
                    });
            }
            $('#deleteConfirm').modal({ keyboard: false });
        };

        $scope.searchSubjects = function () {
            var keywords = $scope.modMember.search;
            if (keywords && keywords.length > 2) {
                var params = {
                    t: "user,client",
                    q: keywords
                };

                RealmData.getSubjects(slug, params)
                    .then(function (data) {
                        $scope.modMember.results = data;
                    })
                    .catch(function (err) {
                        Utils.showError('Error while searching: ' + err.data.message);
                    });
            }
        }

        /*
        * Members
        */

        $scope.loadMembers = function () {
            RealmGroups.getGroupMembers(slug, groupId)
                .then(function (data) {
                    $scope.reloadMembers(data);
                })
                .catch(function (err) {
                    Utils.showError('Failed to load realm group members: ' + err.data.message);
                });
        };

        $scope.reloadMembers = function (data) {
            $scope.members = data;
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
                    var members = subjects.map(s => {
                        return {
                            icon: iconProvider(s.type),
                            ...s
                        };
                    });
                    $scope.members = members;
                })
                .catch(function (err) {
                    Utils.showError('Failed to load group members: ' + err.data.message);
                });

        };

        $scope.createMemberDlg = function () {
            $scope.modMember = {
                subjectId: null,
                search: "",
                results: null
            };
            $('#memberModal').modal({ keyboard: false });
            Utils.refreshFormBS();
        }

        $scope.saveMember = function () {
            if ($scope.modMember) {
                RealmGroups.addGroupMember(slug, groupId, $scope.modMember.subjectId)
                    .then(function () {
                        Utils.showSuccess();
                        $scope.loadMembers();
                    })
                    .catch(function (err) {
                        Utils.showError('Failed to save group member: ' + err.data.message);
                    });
            }

            $('#memberModal').modal('hide');
        }

        $scope.removeMember = function (member) {
            $scope.doDelete = function () {
                $('#deleteConfirm').modal('hide');
                RealmGroups.removeGroupMember(slug, groupId, member.subjectId)
                    .then(function () {
                        Utils.showSuccess();
                        $scope.loadMembers();
                    })
                    .catch(function (err) {
                        Utils.showError('Failed to remove group member: ' + err.data.message);
                    });
            }
            $('#deleteConfirm').modal({ keyboard: false });
        };

        /*
        * realm roles
        */
        $scope.loadRoles = function () {
            RealmGroups.getGroupRoles(slug, groupId)
                .then(function (data) {
                    $scope.reloadRoles(data);
                })
                .catch(function (err) {
                    Utils.showError('Failed to load group roles: ' + err.data.message);
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
            $scope.group.roles = data;
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


            RealmGroups.updateGroupRoles(slug, groupId, data)
                .then(function (data) {
                    $scope.reloadRoles(data);
                    Utils.showSuccess();
                })
                .catch(function (err) {
                    Utils.showError('Failed to update roles: ' + err.data.message);
                });


        }

        var iconProvider = function (type) {
            var icon = './italia/svg/sprite.svg#it-user';
            if (type == 'client') {
                icon = './italia/svg/sprite.svg#it-piattaforme';
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