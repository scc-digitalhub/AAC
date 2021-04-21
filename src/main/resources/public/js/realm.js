angular.module('aac.controllers.realm', [])

/**
 * Main realm layout controller
 */
.controller('RealmController', function ($scope, $state, $stateParams, RealmData, Utils) {
  var slug = $stateParams.realmId;
  if (slug) {
    RealmData.getRealm(slug)
    .then(function(data){
      $scope.realm = data;
    })
    .catch(function(err) {
       Utils.showError('Failed to load realm: '+err.data.message);
    });
  } else {
    RealmData.getMyRealms()
    .then(function(data){
      $scope.realm = data[0];
      $state.go('realm', {realmId: $scope.realm.slug, isGlobal: !$scope.realm.slug});
    })
    .catch(function(err) {
       Utils.showError('Failed to load realms: '+err.data.message);
    });
  } 
})

/**
 * Realm users controller
 */
.controller('RealmUsersController', function ($scope, $stateParams, RealmData, Utils) {  
  var slug = $stateParams.realmId;
  $scope.query = {
    page: 0,
    size: 20,
    sort: {username: 1},
    q: ''
  }

  $scope.load = function() {
    RealmData.getRealmUsers(slug, $scope.query)
    .then(function(data) {
      $scope.users = data;
      $scope.users.content.forEach(function(u) {
        u._providers = u.identities.map(function(i) {
          return i.provider;
        });
        u._authorities = u.authorities
        .filter(function(a) { return a.realm === $scope.realm.slug })
        .map(function(a) { return a.role });
      }); 
    })
    .catch(function(err) {
       Utils.showError('Failed to load realm users: '+err.data.message);
    });
  }  
  
  /**
   * Initialize the app: load list of the developer's apps and reset views
   */
  var init = function() {
    $scope.load();
  };
  
  $scope.deleteUserDlg = function(user) {
    $scope.user = user;
    $('#deleteConfirm').modal({keyboard: false});
  }
  
  $scope.deleteUser = function() {
    $('#deleteConfirm').modal('hide');
    RealmData.removeUser($scope.realm.slug, $scope.user).then(function() {
      $scope.load();
    }).catch(function(err) {
      Utils.showError(err.data.message);
    });
  }
  
  $scope.setPage = function(page) {
   $scope.query.page = page;
   $scope.load();
  }
  
  init();
  
  $scope.editRoles = function(user) {
     var systemRoles = ['ROLE_ADMIN', 'ROLE_DEVELOPER'];
     $scope.roles = {
        system_map: {}, map: {}, custom: ''
     }
     user._authorities.forEach(function(a) {
       if (systemRoles.indexOf(a) >= 0) $scope.roles.system_map[a] = true;
       else $scope.roles.map[a] = true;
     });
     $('#rolesModal').modal({backdrop: 'static', focus: true})
  
  }
  
  $scope.hasRoles = function(m1, m2){
    var res = false;
    for (var r in m1) res |= m1[r];
    for (var r in m2) res |= m2[r];
    return res;
  }
  
  // save roles
  $scope.updateRoles = function() {
    var roles = [];
    for (var k in $scope.roles.system_map) if ($scope.roles.system_map[k]) roles.push(k); 
    for (var k in $scope.roles.map) if ($scope.roles.map[k]) roles.push(k);
     
    $('#rolesModal').modal('hide');
    RealmData.updateRealmRoles($scope.realm.slug, $scope.user, roles)
    .then(function() {
      $scope.load();
      Utils.showSuccess();
    })
    .catch(function(err) {
      Utils.showError(err);
    });
  }
  $scope.dismiss = function(){
    $('#rolesModal').modal('hide');
  }
  
  $scope.addRole = function(){
    $scope.roles.map[$scope.roles.custom] = true;
    $scope.roles.custom = null;
  }

  $scope.invalidRole = function(role) {
    return !role || !(/^[a-zA-Z0-9_]{3,63}((\.[a-zA-Z0-9_]{2,63})*\.[a-zA-Z]{2,63})?$/g.test(role))
  }

})