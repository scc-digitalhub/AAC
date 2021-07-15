angular.module('aac.controllers.realm', [])

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
      RealmData.getRealmStats(slug)
        .then(function (stats) {
          $scope.load(stats);
        })
        .catch(function (err) {
          Utils.showError('Failed to load realm: ' + err.data.message);
        });
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

  /**
   * Realm users controller
   */
  .controller('RealmUsersController', function ($scope, $stateParams, RealmData, RealmProviders, Utils) {
    var slug = $stateParams.realmId;
    $scope.query = {
      page: 0,
      size: 20,
      sort: { username: 1 },
      q: ''
    }

    $scope.load = function () {
      RealmData.getRealmUsers(slug, $scope.query)
        .then(function (data) {
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
      RealmData.removeUser($scope.realm.slug, $scope.modUser).then(function () {
        $scope.load();
      }).catch(function (err) {
        Utils.showError(err.data.message);
      });
    }

    $scope.jsonUserDlg = function (user) {
      $scope.modUser = user;
      $('#userJsonModal').modal({ keyboard: false });

    }

    $scope.setPage = function (page) {
      $scope.query.page = page;
      $scope.load();
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

        RealmData.updateRealmRoles($scope.realm.slug, $scope.modUser, roles)
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

        RealmData.inviteUser($scope.realm.slug, $scope.invitation, roles).then(function () {
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
        "login": {
          'headerText': null,
          'footerText': null,
          'loginText': null,
          'resetPasswordText': null
        },
        "registration": {
          'headerText': null,
          'footerText': null,
          'registrationText': null,
          'registrationSuccessText': null,
        },
        "approval": {
          'headerText': null,
          'footerText': null,
          'approvalText': null
        },
        "endsession": {
          'headerText': null,
          'footerText': null,
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

    $scope.exportRealm = function () {
      window.open('console/dev/realms/' + $scope.realm.slug + '/export');
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
            console.log(res);
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

  .controller('RealmSettingsController', function ($scope, $stateParams, RealmData, Utils) {
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
      $scope.realmSettings = data;
      Utils.refreshFormBS(300);
    };

    $scope.saveRealmSettings = function () {
      var data = $scope.realmSettings;

      RealmData.updateRealm($scope.realm.slug, data)
        .then(function (res) {
          $scope.load(res);
          $scope['$parent'].refresh();
          Utils.showSuccess();
        })
        .catch(function (err) {
          Utils.showError(err.data.message);
        });

    }

    $scope.exportRealm = function () {
      window.open('console/dev/realms/' + $scope.realm.slug + '/export');
    };

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