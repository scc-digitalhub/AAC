angular.module('aac.controllers.realm', [])
   /**
    * Realm Data Services
    */
   .service('RealmData', function ($http, $httpParamSerializer) {
      var rService = {};

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

      rService.getRealm = function (slug) {
         return $http.get('console/dev/realms/' + slug).then(function (data) {
            return data.data;
         });
      }

      rService.updateRealm = function (slug, r) {
         return $http.put('console/dev/realms/' + slug, r).then(function (data) {
            return data.data;
         });
      }

      rService.removeRealm = function (slug) {
         return $http.delete('console/dev/realms/' + slug).then(function (data) {
            return data.data;
         });
      }

      rService.previewRealm = function (slug, template, cb) {
         return $http.post('console/dev/realms/' + slug + '/custom?template=' + template, cb).then(function (data) {
            return data.data;
         });
      }

      rService.getRealmStats = function (slug) {
         return $http.get('console/dev/realms/' + slug + '/stats').then(function (data) {
            return data.data;
         });
      }
      rService.getMyRealms = function () {
         return $http.get('console/dev/realms').then(function (data) {
            return data.data;
         });
      }
      rService.getResources = function (slug) {
         return $http.get('console/dev/realms/' + slug + '/resources').then(function (data) {
            return data.data;
         });
      }

      rService.getUrl = function (slug) {
         return $http.get('console/dev/realms/' + slug + '/well-known/url').then(function (data) {
            return data.data;
         });
      }

      rService.getOAuth2Metadata = function (slug) {
         return $http.get('console/dev/realms/' + slug + '/well-known/oauth2').then(function (data) {
            return data.data;
         });
      }

      rService.getSubject = function (slug, subject) {
         return $http.get('console/dev/realms/' + slug + '/subjects/' + subject).then(function (data) {
            return data.data;
         });
      }
      rService.getSubjects = function (slug, params) {
         return $http.get('console/dev/realms/' + slug + '/subjects?' + buildQuery(params)).then(function (data) {
            return data.data;
         });
      }

      rService.getDevelopers = function (slug) {
         return $http.get('console/dev/realms/' + slug + '/developers').then(function (data) {
            return data.data;
         });
      }
      rService.updateDeveloper = function (slug, subject, authorities) {
         return $http.put('console/dev/realms/' + slug + '/developers/' + subject, authorities).then(function (data) {
            return data.data;
         });
      }
      rService.removeDeveloper = function (slug, subject) {
         return $http.delete('console/dev/realms/' + slug + '/developers/' + subject).then(function (data) {
            return data.data;
         });
      }
      rService.inviteDeveloper = function (slug, invite) {
         return $http.post('console/dev/realms/' + slug + '/developers', invite).then(function (data) {
            return data.data;
         });
      }
      return rService;

   })
   /**
    * Main realm layout controller
    */
   .controller('RealmController', function ($scope, $rootScope, $state, $stateParams, RealmData, Utils) {
      var slug = $stateParams.realmId;
      var toDashboard = false;


      var init = function () {
         RealmData.getMyRealms()
            .then(function (data) {
               $scope.realms = data;
               if (!slug) {
                  var stored = localStorage.getItem('realm');
                  if (stored) {
                     slug = data.find(r => r.slug == stored.slug);
                  }
                  if (!slug && data.length > 0) {
                     slug = data[0].slug;
                  }
                  toDashboard = true;
               }

               return data;
            })
            .then(function () {
               RealmData.getRealm(slug)
                  .then(function (data) {
                     $scope.load(data);
                  });
            })
            .catch(function (err) {
               Utils.showError('Failed to load realms: ' + err.data.message);
            });
      }

      $scope.selectRealm = function (r) {
         localStorage.setItem('realm', r.slug);
         $scope.realm = r;
         $state.go('realm', { realmId: $scope.realm.slug, isGlobal: !$scope.realm.slug });
      }

      $scope.load = function (data) {
         $scope.realm = data;
         // global admin or realm admin
         $scope.realmAdmin = $rootScope.user.authorities.findIndex(function (a) { return a.realm == slug && a.role == 'ROLE_ADMIN' || a.authority == 'ROLE_ADMIN' }) >= 0;
         // realm admin or developer
         $scope.realmDeveloper = $rootScope.user.authorities.findIndex(function (a) { return a.realm == slug && a.role == 'ROLE_DEVELOPER' }) >= 0 || $scope.realmAdmin;
         $scope.realmImmutable = $scope.realm.slug == 'system' || data.slug == '';
         if (toDashboard) {
            $state.go('realm', { realmId: $scope.realm.slug, isGlobal: !$scope.realm.slug });
         }
      }

      $scope.refresh = function () {
         RealmData.getMyRealms()
            .then(function (data) {
               $scope.realms = data;
               return data;
            })
            .then(function () {
               RealmData.getRealm(slug)
                  .then(function (data) {
                     $scope.load(data);
                  });
            })
            .catch(function (err) {
               Utils.showError('Failed to load realms: ' + err.data.message);
            });
      }

      $scope.isAnyState = function (states) {
         var ret = false;
         if (states) {
            var sm = states.filter(s => $state.includes(s));
            ret = sm && sm.length > 0;
         }

         return ret;
      }

      init();

      // RealmData.getMyRealms()
      //   .then(function (data) {
      //     $scope.realms = data;
      //     if (!slug) {
      //       var stored = localStorage.getItem('realm') || null;
      //       var idx = stored ? data.findIndex(r => r.slug == slug) : 0;
      //       if (idx < 0) idx = 0;
      //       slug = data[idx].slug;
      //       toDashboard = true;
      //     }
      //     RealmData.getRealm(slug)
      //       .then(function (data) {
      //         $scope.realm = data;
      //         // global admin or realm admin
      //         $scope.realmAdmin = $rootScope.user.authorities.findIndex(function (a) { return a.realm == slug && a.role == 'ROLE_ADMIN' || a.authority == 'ROLE_ADMIN' }) >= 0;
      //         // realm admin or developer
      //         $scope.realmDeveloper = $rootScope.user.authorities.findIndex(function (a) { return a.realm == slug && a.role == 'ROLE_DEVELOPER' }) >= 0 || $scope.realmAdmin;
      //         $scope.realmImmutable = $scope.realm.slug == 'system' || data.slug == '';
      //         if (toDashboard) {
      //           $state.go('realm', { realmId: $scope.realm.slug, isGlobal: !$scope.realm.slug });
      //         }
      //       });
      //   })
      //   .catch(function (err) {
      //     Utils.showError('Failed to load realms: ' + err.data.message);
      //   });
   })

   .controller('RealmDashboardController', function ($scope, $rootScope, $state, $stateParams, RealmData, Utils) {
      var slug = $stateParams.realmId;
      $scope.curStep = 1
      var init = function () {
         if (slug) {
            RealmData.getRealmStats(slug)
               .then(function (stats) {
                  $scope.load(stats);
               })
               .catch(function (err) {
                  Utils.showError('Failed to load realm: ' + err.data.message);
               });
         }
      };

      $scope.load = function (stats) {
         $scope.stats = stats;
         $scope.showAddIdp = !(stats.providers);
         $scope.showAddApp = !(stats.apps);
         $scope.showAddService = !(stats.services);
         $scope.showAddUser = !(stats.users);
         $scope.showStart = $scope.showAddIdp || $scope.showAddApp || $scope.showAddService || $scope.showAddUser;
         $scope.showAudit = (stats.loginCount > 0 || stats.registrationCount > 0);
         $scope.curStep = 0;
         if ($scope.showAddIdp) {
            $scope.curStep = 1;
         } else if ($scope.showAddApp) {
            $scope.curStep = 2;
         } else if ($scope.showAddService) {
            $scope.curStep = 3;
         } else if ($scope.showAddUser) {
            $scope.curStep = 4;
         }
      };

      $scope.prevStep = function () {
         $scope.curStep--;
      }
      $scope.nextStep = function () {
         $scope.curStep++;
      }
      $scope.setStep = function (s) {
         $scope.curStep = s;
      }

      init();

   })
   .controller('RealmCustomController', function ($scope, $stateParams, RealmData, Utils) {
      var slug = $stateParams.realmId;

      $scope.formView = 'login';

      $scope.aceOption = {
         mode: 'html',
         theme: 'monokai',
         maxLines: 30,
         minLines: 12,
      };

      $scope.activeView = function (view) {
         return view == $scope.formView ? 'active' : '';
      };

      $scope.switchView = function (view) {
         $scope.formView = view;
         Utils.refreshFormBS(300);
      }

      var init = function () {
         RealmData.getRealm(slug)
            .then(function (data) {
               $scope.load(data);
               return data;
            })
            .catch(function (err) {
               Utils.showError('Failed to load realm : ' + err.data.message);
            });
      };

      $scope.load = function (data) {
         $scope.realmName = data.name;
         // TODO multilanguage
         //TODO handle templates registration in controllers...
         var customization = {
            "global": {
               'headerText': null,
               'footerText': null
            },
            "login": {
               'loginText': null,
               'resetPasswordText': null
            },
            "registration": {
               'registrationText': null,
               'registrationSuccessText': null,
            },
            "approval": {
               'approvalText': null
            },
            "endsession": {
               'logoutText': null
            }



         };

         if (data.customization != null) {
            for (var cb of data.customization) {
               var k = cb.identifier;
               for (var r in cb.resources) {
                  customization[k][r] = cb.resources[r];
               }
            }
         }
         var custom = [];
         for (k in customization) {
            var arr = [];
            for (r in customization[k]) {
               arr.push({ 'key': r, 'value': customization[k][r] });
            }

            custom[k] = arr;
         }
         $scope.realmCustom = custom;
      }

      $scope.saveRealmCustom = function () {
         var data = $scope.realm;
         var customization = [];

         for (var k in $scope.realmCustom) {
            var res = {};
            var rs = $scope.realmCustom[k]
               .filter(function (r) {
                  return r.value != null;
               });

            for (var r of rs) {
               res[r.key] = r.value;
            }

            if (Object.keys(res).length > 0) {
               customization.push({ 'identifier': k, 'resources': res });
            }
         }

         data.customization = customization;



         RealmData.updateRealm($scope.realm.slug, data)
            .then(function (res) {
               $scope.load(res);
               Utils.showSuccess();
            })
            .catch(function (err) {
               Utils.showError(err.data.message);
            });

      }

      $scope.exportRealmCustom = function () {
         window.open('console/dev/realms/' + $scope.realm.slug + '/export?custom=1');
      };

      $scope.previewRealmCustom = function (template) {
         if ($scope.realmCustom[template] == null) {
            Utils.showError("invalid template or missing data");
         } else {
            var cb = {
               'identifier': template,
               'resources': {}
            }
            var res = {};
            var rs = $scope.realmCustom[template]
               .filter(function (r) {
                  return r.value != null;
               });

            for (var r of rs) {
               res[r.key] = r.value;
            }

            if (Object.keys(res).length > 0) {
               cb.resources = res;
            }


            RealmData.previewRealm($scope.realm.slug, template, cb)
               .then(function (res) {
                  $scope.customPreview = res;
                  $('#customPreview').modal({ keyboard: false });
               })
               .catch(function (err) {
                  Utils.showError(err.data.message);
               });
         }
      };

      init();
   })

   .controller('RealmSettingsController', function ($scope, $state, $stateParams, RealmData, Utils) {
      var slug = $stateParams.realmId;


      $scope.formView = 'basic';

      $scope.aceOption = {
         mode: 'html',
         theme: 'monokai',
         maxLines: 30,
         minLines: 12,
      };

      $scope.activeView = function (view) {
         return view == $scope.formView ? 'active' : '';
      };

      $scope.switchView = function (view) {
         $scope.formView = view;
         Utils.refreshFormBS(300);
      }


      var init = function () {
         $scope.load();
      };

      $scope.load = function () {
         RealmData.getRealm(slug)
            .then(function (data) {
               $scope.reload(data);
               return data;
            })
            .then(function () {
               $scope.loadDevelopers();
            })
            .catch(function (err) {
               Utils.showError('Failed to load realm : ' + err.data.message);
            });
      }

      $scope.reload = function (data) {
         $scope.realmSettings = data;
         Utils.refreshFormBS(300);
      };

      $scope.saveRealmSettings = function () {
         var data = $scope.realmSettings;

         RealmData.updateRealm($scope.realm.slug, data)
            .then(function (res) {
               $scope.reload(res);
               $scope['$parent'].refresh();
               Utils.showSuccess();
            })
            .catch(function (err) {
               Utils.showError(err.data.message);
            });

      }

      $scope.exportRealmSettings = function () {
         window.open('console/dev/realms/' + $scope.realm.slug + '/export');
      };

      $scope.exportRealm = function () {
         window.open('console/dev/realms/' + $scope.realm.slug + '/export?full=1');
      };

      $scope.deleteRealmDlg = function () {
         $scope.modRealm = $scope.realm;
         //add confirm field
         $scope.modRealm.confirmSlug = '';
         $('#deleteRealmConfirm').modal({ keyboard: false });
      }

      $scope.deleteRealm = function () {
         $('#deleteRealmConfirm').modal('hide');
         if ($scope.modRealm.slug === $scope.modRealm.confirmSlug) {
            RealmData.removeRealm($scope.modRealm.slug).then(function () {
               $state.go('realm', { realmId: null });
               Utils.showSuccess();
            }).catch(function (err) {
               Utils.showError(err.data.message);
            });
         } else {
            Utils.showError("confirmId not valid");
         }
      }

      $scope.loadDevelopers = function () {
         RealmData.getDevelopers(slug)
            .then(function (data) {
               $scope.users = data;
               return data;
            })
            .catch(function (err) {
               Utils.showError('Failed to load realm : ' + err.data.message);
            });
      }


      $scope.inviteDeveloperDlg = function () {

         $scope.modInvite = {
            external: false,
            username: null,
            subjectId: null,
            email: null,
         };
         $('#inviteModal').modal({ keyboard: false });
      }

      $scope.inviteDeveloper = function () {
         $('#inviteModal').modal('hide');
         if ($scope.modInvite) {
            var { email, subjectId } = $scope.modInvite;
            var data = {
               email, subjectId
            };

            RealmData.inviteDeveloper(slug, data)
               .then(function () {
                  $scope.loadDevelopers();
                  Utils.showSuccess();
               })
               .catch(function (err) {
                  Utils.showError('Failed to invite user: ' + err.data.message);
               });


         }
      }

      $scope.manageAuthoritiesDlg = function (user) {
         var authorities = user.authorities;
         var roles = authorities.map(a => a.role);

         $scope.modAuthorities = {
            realm: slug,
            subject: user.subjectId,
            admin: roles.includes('ROLE_ADMIN'),
            developer: roles.includes('ROLE_DEVELOPER')
         };
         $('#authoritiesModal').modal({ keyboard: false });
      }

      $scope.updateAuthorities = function () {
         $('#authoritiesModal').modal('hide');
         if ($scope.modAuthorities) {
            var subjectId = $scope.modAuthorities.subject;
            var roles = $scope.modAuthorities;
            var authorities = [];


            if (roles.admin === true) {
               authorities.push('ROLE_ADMIN');
            }
            if (roles.developer === true) {
               authorities.push('ROLE_DEVELOPER');
            }


            RealmData.updateDeveloper(slug, subjectId, authorities)
               .then(function () {
                  $scope.loadDevelopers();
                  Utils.showSuccess();
               })
               .catch(function (err) {
                  Utils.showError('Failed to update authorities: ' + err.data.message);
               });

         }
      }



      init();
   })

   .controller('RealmScopesController', function ($scope, $stateParams, RealmData, Utils) {
      var slug = $stateParams.realmId;


      var init = function () {
         RealmData.getResources(slug)
            .then(function (data) {
               $scope.load(data);
               return data;
            })
            .catch(function (err) {
               Utils.showError('Failed to load realm : ' + err.data.message);
            });
      };

      $scope.load = function (data) {
         $scope.resources = data;
         var scopes = [];
         data.forEach(r => {
            var ss = r.scopes.map(s => { return { ...s, 'resource': r } });
            scopes.push(...ss)
         });
         $scope.scopes = scopes;
         $scope.results = null;
         $scope.search = null;
         Utils.refreshFormBS(300);
      };


      $scope.scopesDlg = function (resource) {
         $scope.scopeResource = resource;
         $('#scopesModal').modal({ backdrop: 'static', focus: true })
         Utils.refreshFormBS();
      }

      $scope.doSearch = function () {
         var keywords = $scope.search;
         var results = null;
         if (keywords) {
            results = $scope.scopes
               .filter(s => {
                  return s.scope.includes(keywords.toLowerCase())
                     || s.name.toLowerCase().includes(keywords.toLowerCase())
                     || s.resource.name.toLowerCase().includes(keywords.toLowerCase());
               });
         }
         $scope.results = results;
      }


      init();
   })
   ;