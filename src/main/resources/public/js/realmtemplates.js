angular.module('aac.controllers.realmtemplates', ['ngSanitize'])

    .service('RealmTemplates', function ($http, $httpParamSerializer) {
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


        service.getAuthorities = function (realm) {
            return $http.get('console/dev/templates/' + realm).then(function (data) {
                return data.data;
            });
        }

        service.getTemplates = function (realm, authority) {
            return $http.get('console/dev/templates/' + realm + '/' + authority).then(function (data) {
                return data.data;
            });
        }
        service.getTemplate = function (realm, authority, template) {
            return $http.get('console/dev/templates/' + realm + '/' + authority + '/' + template).then(function (data) {
                return data.data;
            });
        }

        service.getTemplateModels = function (realm, params) {
            return $http.get('console/dev/templates/' + realm + '/models/search?' + buildQuery(params)).then(function (data) {
                return data.data;
            });
        }


        service.getTemplateModel = function (realm, templateId) {
            return $http.get('console/dev/templates/' + realm + '/models/' + templateId).then(function (data) {
                return data.data;
            });
        }

        service.addTemplateModel = function (realm, model) {
            return $http.post('console/dev/templates/' + realm + '/models', model).then(function (data) {
                return data.data;
            });
        }
        service.updateTemplateModel = function (realm, model) {
            return $http.put('console/dev/templates/' + realm + '/models/' + model.id, model).then(function (data) {
                return data.data;
            });
        }
        service.deleteTemplateModel = function (realm, templateId) {
            return $http.delete('console/dev/templates/' + realm + '/models/' + templateId).then(function (data) {
                return data.data;
            });
        }
        service.importTemplateModel = function (realm, file, yaml, reset) {
            var fd = new FormData();
            if (yaml) {
                fd.append('yaml', yaml);
            }
            if (file) {
                fd.append('file', file);
            }
            return $http({
                url: 'console/dev/templates/' + realm + '/models' + (reset ? "?reset=true" : ""),
                headers: { "Content-Type": undefined }, //set undefined to let $http manage multipart declaration with proper boundaries
                data: fd,
                method: "PUT"
            }).then(function (data) {
                return data.data;
            });
        }
        service.exportTemplateModel = function (realm, templateId) {
            window.open('console/dev/templates/' + realm + '/models/' + templateId + '/export');
        }
        service.previewTemplateModel = function (realm, id, model) {
            return $http.post('console/dev/templates/' + realm + '/models/' + id + '/preview', model).then(function (data) {
                return data.data;
            });
        }

        return service;
    })
    .controller('RealmTemplatesController', function ($scope, $state, $stateParams, RealmData, RealmTemplates, Utils) {
        var slug = $stateParams.realmId;
        $scope.query = {
            page: 0,
            size: 20,
            sort: { template: 1 },
            q: ''
        }
        $scope.keywords = '';

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

        $scope.load = function () {
            RealmTemplates.getTemplateModels(slug, $scope.query)
                .then(function (data) {
                    $scope.keywords = $scope.query.q;
                    $scope.models = data;
                })
                .catch(function (err) {
                    Utils.showError('Failed to load realm templates: ' + err.data.message);
                });

        };

        var init = function () {
            RealmData.getTemplatesConfig(slug)
                .then(function (data) {
                    $scope.availableLanguages = data.settings.languages;
                })
                .then(function () {
                    return RealmTemplates.getAuthorities(slug);
                })
                .then(function (data) {
                    $scope.authorities = data;
                    return data;
                })
                .then(function (authorities) {
                    if (authorities) {
                        return Promise.all(
                            authorities.map(a => {
                                return RealmTemplates.getTemplates(slug, a)
                                    .then(function (data) {
                                        return {
                                            authority: a,
                                            templates: data
                                        }
                                    });
                            })
                        );
                    }
                })
                .then(function (templates) {
                    if (templates) {
                        var aMap = new Map(templates.map(a => [a.authority, a.templates]));
                        $scope.templates = aMap;
                    }
                })
                .then(function () {
                    $scope.load();
                }).catch(function (err) {
                    Utils.showError('Failed to load realm templates: ' + err.data.message);
                });
        };


        $scope.createTemplateDlg = function (authority) {
            $scope.modTemplate = {
                realm: slug,
                authority: authority,
                language: null,
                template: null
            }

            //TODO filter on existing (per language)
            //var list = $scope.models.filter(m => m.authority == authority).map(m => m.template);
            $scope.modTemplates = $scope.templates.get(authority).map(t => t.template);

            $('#createTemplateModal').modal({ keyboard: false });
            Utils.refreshFormBS(300);

        }

        $scope.createTemplate = function () {
            $('#createTemplateModal').modal('hide');


            if ($scope.modTemplate) {
                var data = {
                    ...$scope.modTemplate,
                    realm: slug
                }
                RealmTemplates.addTemplateModel(slug, data)
                    .then(function () {
                        $scope.load();
                        Utils.showSuccess();
                    })
                    .catch(function (err) {
                        Utils.showError(err.data.message);
                    });
            }
        }


        $scope.importTemplateDlg = function () {
            if ($scope.importFile) {
                $scope.importFile.file = null;
            }

            $('#importTemplateModal').modal({ keyboard: false });
        }


        $scope.importTemplate = function () {
            $('#importTemplateModal').modal('hide');
            var file = $scope.importFile.file;
            var yaml = $scope.importFile.yaml;
            var resetID = !!$scope.importFile.resetID;
            var mimeTypes = ['text/yaml', 'text/yml', 'application/x-yaml'];
            if (!yaml && (file == null || !mimeTypes.includes(file.type) || file.size == 0)) {
                Utils.showError("invalid file");
            } else {
                RealmTemplates.importTemplateModel(slug, file, yaml, resetID)
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

        $scope.exportTemplate = function (item) {
            if (item) {
                RealmTemplates.exportTemplateModel(slug, item.id);
            }
        }

        $scope.deleteTemplateDlg = function (item) {
            $scope.modTemplate = item;
            //add confirm field
            $scope.modTemplate.confirmId = '';
            $('#deleteTemplateModal').modal({ keyboard: false });
        }

        $scope.deleteTemplate = function () {
            $('#deleteTemplateModal').modal('hide');
            if ($scope.modTemplate.id === $scope.modTemplate.confirmId) {
                RealmTemplates.deleteTemplateModel(slug, $scope.modTemplate.id).then(function () {
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

    .controller('RealmTemplateController', function ($scope, $state, $stateParams, $sanitize, $sce, RealmTemplates, RealmData, RealmRoles, Utils) {
        var slug = $stateParams.realmId;
        var templateId = $stateParams.templateId;


        $scope.aceOption = {
            mode: 'html',
            theme: 'monokai',
            maxLines: 35,
            minLines: 6
        };



        $scope.load = function () {
            var model;
            RealmTemplates.getTemplateModel(slug, templateId)
                .then(function (data) {
                    model = data;
                    return data;
                })
                .then(function (data) {
                    //load template   
                    return RealmTemplates.getTemplate(slug, data.authority, data.template)
                })
                .then(function (data) {
                    $scope.template = data;
                    $scope.keys = data.keys;
                })
                .then(function () {
                    $scope.reload(model);
                })
                .catch(function (err) {
                    Utils.showError('Failed to load realm template: ' + err.data.message);
                });

        };

        var init = function () {
            $scope.load();
        };

        $scope.reload = function (data) {
            $scope.templatemodelname = data.template + " " + data.language;
            $scope.templatemodeldescription = "";

            //make sure every key is available in content for edit
            //use map to avoid sink injection
            var cMap = data.content ? new Map(Object.entries(data.content)) : new Map();
            $scope.keys.forEach(key => {
                if (!cMap.has(key)) {
                    cMap.set(key, "");
                }
            });

            $scope.templatemodel = data;
            var contentMap = Array.from(cMap, ([key, value]) => {
                return {
                    'key': key,
                    'value': value
                };
            });

            // sort by name to keep UI consistent 
            contentMap.sort((a, b) => {
                var x = a.key.toUpperCase();
                var y = b.key.toUpperCase();
                if (x < y) {
                    return -1;
                }
                if (x > y) {
                    return 1;
                }

                // names must be equal
                return 0;
            });

            $scope.contentMap = contentMap;
        }

        $scope.exportTemplate = function () {
            RealmTemplates.exportTemplateModel(slug, templateId);
        }

        $scope.saveTemplate = function () {
            var model = $scope.templatemodel;

            //collect map
            var cMap = new Map($scope.contentMap.map(e => [e.key, e.value]));
            var content = Object.fromEntries(cMap);

            //save only editable 
            var data = {
                realm: slug,
                id: templateId,
                template: model.template,
                authority: model.authority,
                language: model.language,
                content: content
            };

            RealmTemplates.updateTemplateModel(slug, data)
                .then(function (res) {
                    Utils.showSuccess();
                    $scope.reload(res);
                })
                .catch(function (err) {
                    Utils.showError('Failed to save template: ' + err.data.message);
                });
        }


        $scope.deleteTemplate = function () {
            $scope.doDelete = function () {
                $('#deleteConfirm').modal('hide');
                RealmTemplates.deleteTemplateModel(slug, templateId)
                    .then(function () {
                        Utils.showSuccess();
                        $state.go('realm.templates', { realmId: $stateParams.realmId });
                    })
                    .catch(function (err) {
                        Utils.showError('Failed to delete template: ' + err.data.message);
                    });
            }
            $('#deleteConfirm').modal({ keyboard: false });
        };


        $scope.previewTemplate = function () {
            var model = $scope.templatemodel;

            //collect map
            var cMap = new Map($scope.contentMap.map(e => [e.key, e.value]));
            var content = Object.fromEntries(cMap);

            //preview only editable 
            var data = {
                realm: slug,
                id: templateId,
                template: model.template,
                authority: model.authority,
                language: model.language,
                content: content
            };

            RealmTemplates.previewTemplateModel(slug, templateId, data)
                .then(function (res) {
                    $scope.templatePreview = extractPreview(res);
                    $('#templatePreview').modal({ keyboard: false });
                })
                .catch(function (err) {
                    Utils.showError('Failed to process template: ' + err.data.message);
                });

        };

        var extractPreview = function (html) {
            var parser = new DOMParser();
            var doc = parser.parseFromString(html, "text/html");

            //extract also custom style if present
            var css = doc.getElementById('customStyle');

            if (css) {
                //append to body
                //DISABLED because this will impact console as well
                //doc.body.appendChild(css);
            }


            //skip sanitization, we trust output enough since it went though server sanitization
            //var content = $sanitize(doc.body.innerHTML);
            var content = doc.body.innerHTML;


            return $sce.trustAsHtml(content);
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