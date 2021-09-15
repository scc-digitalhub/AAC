angular.module('aac.controllers.dev', [])
  /**
   * Main layout controller
   * @param $scope
   */
  .controller('DevController', function ($scope, $location, Data, RealmData, Utils) {


    $scope.load = function () {
      RealmData.getMyRealms()
        .then(function (data) {
          $scope.realms = data;
        }).catch(function (err) {
          Utils.showError('Failed to load realms: ' + err.data.message);
        });
    }


    var init = function () {
      $scope.load();
    };



    init();
  })

