angular.module('aac.controllers.realm', [])

  /**
   * Main realm layout controller
   */
  .controller('RealmController', function ($scope, $rootScope, $state, $stateParams, RealmData, Utils) {
    var slug = $stateParams.realmId;
    var toDashboard = false;

    $scope.selectRealm = function (r) {
      localStorage.setItem('realm', r.slug);
      $scope.realm = r;
      $state.go('realm', { realmId: $scope.realm.slug, isGlobal: !$scope.realm.slug });
    }

    RealmData.getMyRealms()
      .then(function (data) {
        $scope.realms = data;
        if (!slug) {
          var stored = localStorage.getItem('realm') || null;
          var idx = stored ? data.findIndex(r => r.slug == slug) : 0;
          if (idx < 0) idx = 0;
          slug = data[idx].slug;
          toDashboard = true;
        }
        RealmData.getRealm(slug)
          .then(function (data) {
            $scope.realm = data;
            // global admin or realm admin
            $scope.realmAdmin = $rootScope.user.authorities.findIndex(function (a) { return a.realm == slug && a.role == 'ROLE_ADMIN' || a.authority == 'ROLE_ADMIN' }) >= 0;
            // realm admin or developer
            $scope.realmDeveloper = $rootScope.user.authorities.findIndex(function (a) { return a.realm == slug && a.role == 'ROLE_DEVELOPER' }) >= 0 || $scope.realmAdmin;
            $scope.realmImmutable = $scope.realm.slug == 'system' || data.slug == '';
            if (toDashboard) {
              $state.go('realm', { realmId: $scope.realm.slug, isGlobal: !$scope.realm.slug });
            }
          });
      })
      .catch(function (err) {
        Utils.showError('Failed to load realms: ' + err.data.message);
      });
  })

  .controller('RealmDashboardController', function ($scope, $rootScope, $state, $stateParams, RealmData, Utils) {
    var slug = $stateParams.realmId;
    if (slug) {
      RealmData.getRealmStats(slug).then(function (stats) {
        $scope.stats = stats;
      })
        .catch(function (err) {
          Utils.showError('Failed to load realm: ' + err.data.message);
        });
    }

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
            u._providers = u.identities.map(function (i) {
              return $scope.providers[i.provider] ? $scope.providers[i.provider].name : i.provider;
            });
            u._authorities = u.authorities
              .filter(function (a) { return a.realm === $scope.realm.slug })
              .map(function (a) { return a.role });
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

    $scope.setPage = function (page) {
      $scope.query.page = page;
      $scope.load();
    }

    init();

    $scope.editRoles = function (user) {
      $scope.modUser = user;
      var systemRoles = ['ROLE_ADMIN', 'ROLE_DEVELOPER'];
      $scope.roles = {
        system_map: {}, map: {}, custom: ''
      }
      user._authorities.forEach(function (a) {
        if (systemRoles.indexOf(a) >= 0) $scope.roles.system_map[a] = true;
        else $scope.roles.map[a] = true;
      });
      $('#rolesModal').modal({ backdrop: 'static', focus: true })

    }

    $scope.inviteUser = function () {
      $scope.invitation = {
        external: false
      }
      $scope.roles = {
        system_map: {}, map: {}, custom: ''
      }
      $('#inviteModal').modal({ backdrop: 'static', focus: true })
    }
    $scope.invite = function () {
      $('#inviteModal').modal('hide');
      var roles = [];
      for (var k in $scope.roles.system_map) if ($scope.roles.system_map[k]) roles.push(k);
      for (var k in $scope.roles.map) if ($scope.roles.map[k]) roles.push(k);
      RealmData.inviteUser($scope.realm.slug, $scope.invitation, roles).then(function () {
        $scope.load();
      }).catch(function (err) {
        Utils.showError(err.data.message);
      });
    }


    $scope.hasRoles = function (m1, m2) {
      var res = false;
      for (var r in m1) res |= m1[r];
      for (var r in m2) res |= m2[r];
      return res;
    }

    // save roles
    $scope.updateRoles = function () {
      var roles = [];
      for (var k in $scope.roles.system_map) if ($scope.roles.system_map[k]) roles.push(k);
      for (var k in $scope.roles.map) if ($scope.roles.map[k]) roles.push(k);

      $('#rolesModal').modal('hide');
      RealmData.updateRealmRoles($scope.realm.slug, $scope.modUser, roles)
        .then(function () {
          $scope.load();
          Utils.showSuccess();
        })
        .catch(function (err) {
          Utils.showError(err);
        });
    }
    $scope.dismiss = function () {
      $('#rolesModal').modal('hide');
    }

    $scope.addRole = function () {
      $scope.roles.map[$scope.roles.custom] = true;
      $scope.roles.custom = null;
    }

    $scope.invalidRole = function (role) {
      return !role || !(/^[a-zA-Z0-9_]{3,63}((\.[a-zA-Z0-9_]{2,63})*\.[a-zA-Z]{2,63})?$/g.test(role))
    }

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
        for (cb of data.customization) {
          var k = cb.identifier;
          for (r in cb.resources) {
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

      for (k in $scope.realmCustom) {
        var res = {};
        var rs = $scope.realmCustom[k]
          .filter(function (r) {
            return r.value != null;
          });

        for (r of rs) {
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
            'resources' : {}
         }
        var res = {};
        var rs = $scope.realmCustom[template]
          .filter(function (r) {
            return r.value != null;
          });

        for (r of rs) {
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
  });