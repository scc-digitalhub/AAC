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
  } else if ($stateParams.isGlobal) {
    // TODO
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
.controller('RealmUsersController', function ($scope, Data, Utils) {  

})