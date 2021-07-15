angular.module('aac.controllers.realmattributesets', [])
    /**
      * Realm Data Services
      */
    .service('RealmAttributeSets', function ($q, $http) {
        var rService = {};
        rService.getAttributeSet = function (slug, identifier) {
            return $http.get('console/dev/realms/' + slug + '/attributeset/' + identifier).then(function (data) {
                return data.data;
            });
        }

        rService.getAttributeSets = function (slug) {
            return $http.get('console/dev/realms/' + slug + '/attributeset').then(function (data) {
                return data.data;
            });
        }

        rService.removeAttributeSet = function (slug, identifier) {
            return $http.delete('console/dev/realms/' + slug + '/attributeset/' + identifier).then(function (data) {
                return data.data;
            });
        }

        rService.addAttributeSet = function (slug, attributeSet) {
            return $http.post('console/dev/realms/' + slug + '/attributeset', attributeSet).then(function (data) {
                return data.data;
            });
        }

        rService.updateAttributeSet = function (slug, identifier, attributeSet) {
            return $http.put('console/dev/realms/' + slug + '/attributeset/' + attributeSet.identifier, attributeSet).then(function (data) {
                return data.data;
            });
        }

        rService.importAttributeSet = function (slug, file) {
            var fd = new FormData();
            fd.append('file', file);
            return $http({
                url: 'console/dev/realms/' + slug + '/attributesets',
                headers: { "Content-Type": undefined }, //set undefined to let $http manage multipart declaration with proper boundaries
                data: fd,
                method: "PUT"
            }).then(function (data) {
                return data.data;
            });

        }

        return rService;

    })
    .controller('RealmAttributeSetsController', function ($scope, $state, $stateParams, RealmData, RealmAttributeSets, Utils) {
        var slug = $stateParams.realmId;

        var init = function () {
            $scope.load();
        };

        $scope.load = function () {
            RealmAttributeSets.getAttributeSets(slug)
                .then(function (data) {
                    $scope.attributeSets = data;
                    return data;
                })
                .catch(function (err) {
                    Utils.showError('Failed to load realm attributeSets: ' + err.data.message);
                });
        }

        $scope.createAttributeSetDlg = function () {
            $scope.modAttributeSet = {
                identifier: '',
                name: '',
                description: '',
                realm: slug

            };

            $('#createAttributeSetDlg').modal({ keyboard: false });
        }

        $scope.createAttributeSet = function () {
            $('#createAttributeSetDlg').modal('hide');

            RealmAttributeSets.addAttributeSet($scope.realm.slug, $scope.modAttributeSet)
                .then(function (res) {
                    $state.go('realm.attributeset', { realmId: res.realm, setId: res.identifier });
                    Utils.showSuccess();
                })
                .catch(function (err) {
                    Utils.showError(err.data.message);
                });

        }

        $scope.deleteAttributeSetDlg = function (attributeSet) {
            $scope.modAttributeSet = attributeSet;
            //add confirm field
            $scope.modAttributeSet.confirmId = '';
            $('#deleteAttributeSetConfirm').modal({ keyboard: false });
        }

        $scope.deleteAttributeSet = function () {
            $('#deleteAttributeSetConfirm').modal('hide');
            if ($scope.modAttributeSet.identifier === $scope.modAttributeSet.confirmId) {
                RealmAttributeSets.removeAttributeSet($scope.realm.slug, $scope.modAttributeSet.identifier).then(function () {
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

    })
    .controller('RealmAttributeSetController', function ($scope, $state, $stateParams, RealmData, RealmAttributeSets, Utils) {
        var slug = $stateParams.realmId;
        var setId = $stateParams.setId;

        $scope.aceOption = {
            mode: 'javascript',
            theme: 'monokai',
            maxLines: 30,
            minLines: 6
        };

        var init = function () {
            RealmAttributeSets.getAttributeSet(slug, setId)
                .then(function (data) {
                    $scope.load(data);
                    return data;
                })
                .catch(function (err) {
                    Utils.showError('Failed to load realm attribute set: ' + err.data.message);
                });
        };


        $scope.load = function (data) {
            $scope.attributeSet = data;
            $scope.attributeSetName = data.name;
        }

        $scope.exportAttributeSet = function (attributeSet) {
            window.open('console/dev/realms/' + slug + '/attributeset/' + attributeSet.identifier + '/yaml');
        };


        $scope.saveAttributeSet = function (attributeSet) {

            var data = {
                ...attributeSet,
                realm: slug
            };

            RealmAttributeSets.updateAttributeSet(slug, data.identifier, data)
                .then(function (res) {
                    $scope.load(res);
                    Utils.showSuccess();
                })
                .catch(function (err) {
                    Utils.showError(err.data.message);
                });

        }

        $scope.editAttributeSetDlg = function (attributeSet) {
            $scope.modAttributeSet = Object.assign({}, attributeSet);
            $('#editAttributeSetDlg').modal({ keyboard: false });
        }

        $scope.editAttributeSet = function () {
            $('#editAttributeSetDlg').modal('hide');
            var set = $scope.modAttributeSet;
            if (set) {
                var data = $scope.attributeSet;
                data.name = set.name;
                data.description = set.description;
                $scope.saveAttributeSet(data);
            }
        }


        $scope.deleteAttributeSetDlg = function (attributeSet) {
            $scope.modAttributeSet = attributeSet;
            //add confirm field
            $scope.modAttributeSet.confirmId = '';
            $('#deleteAttributeSetConfirm').modal({ keyboard: false });
        }

        $scope.deleteAttributeSet = function () {
            $('#deleteAttributeSetConfirm').modal('hide');
            if ($scope.modAttributeSet.identifier === $scope.modAttributeSet.confirmId) {
                RealmAttributeSets.removeAttributeSet(slug, $scope.modAttributeSet.identifier).then(function () {
                    $state.go('realm.attributesets', { realmId: $stateParams.realmId });
                    Utils.showSuccess();
                }).catch(function (err) {
                    Utils.showError(err.data.message);
                });
            } else {
                Utils.showError("confirmId not valid");
            }
        }

        $scope.removeAttribute = function (attr) {
            if (attr) {
                var attributes = $scope.attributeSet.attributes;
                $scope.attributeSet.attributes = attributes.filter(at => at.key !== attr.key);
            }
        };

        $scope.editAttribute = function (attr) {
            if (attr) {
                $scope.modAttr = Object.assign({}, attr);
            } else {
                $scope.modAttr = {};
            }
            $('#editAttrModal').modal({ keyboard: false });
            Utils.refreshFormBS();
        };


        $scope.saveAttribute = function () {
            $('#editAttrModal').modal('hide');
            var attr = $scope.modAttr;
            var attributes = $scope.attributeSet.attributes;
            if (attr) {
                //check if update or add, key is unique
                var a = attributes.find(at => at.key == attr.key);
                if (a) {
                    //update
                    a.type = attr.type;
                    a.multiple = attr.multiple;
                    a.name = attr.name;
                    a.description = attr.description;
                } else {
                    attributes.push(attr);
                }
            }

            $scope.attributeSet.attributes = attributes;
        };

        init();
    })

    ;