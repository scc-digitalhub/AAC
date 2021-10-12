angular.module('aac.controllers.admin', [])
  /**
   * Main layout controller
   * @param $scope
   */
  .controller('AdminController', function ($scope, $location, AdminData, Utils) {
    $scope.adminView = 'realms';
    $scope.query = {
      page: 0,
      size: 20,
      sort: { slug: 1 },
      q: ''
    }

    $scope.load = function () {
      AdminData.getRealms($scope.query).then(function (data) {
        $scope.realms = data;
      }).catch(function (err) {
        Utils.showError('Failed to load realms: ' + err.data.message);
      });
    }

    /**
     * Initialize the app: load list of the developer's apps and reset views
     */
    var init = function () {
      $scope.load();
    };

    $scope.addRealm = function () {
      $scope.newRealm = true;
      $scope.realm = {};
      $('#realmModal').modal({ backdrop: 'static', focus: true })
    }
    $scope.editRealm = function (realm) {
      $scope.newRealm = false;
      $scope.realm = Object.assign({}, realm);
      $('#realmModal').modal({ backdrop: 'static', focus: true })
      Utils.refreshFormBS();
    }
    $scope.save = function () {
      $('#realmModal').modal('hide');
      var op = $scope.newRealm ? AdminData.addRealm : AdminData.updateRealm;
      op($scope.realm).then(function (data) {
        $scope.load();
      }).catch(function (err) {
        Utils.showError('Failed to save realm: ' + err.data.message);
      });
    }

    $scope.removeRealm = function (realm) {
      $scope.realm = realm;
      $('#deleteConfirm').modal({ keyboard: false });
    }

    $scope.deleteRealm = function () {
      $('#deleteConfirm').modal('hide');
      AdminData.removeRealm($scope.realm.slug).then(function () {
        $scope.load();
      }).catch(function (err) {
        Utils.showError(err.data.message);
      });
    }

    $scope.manageRealm = function (item) {
      $location.path('realms/' + item.slug);
    }

    $scope.dismiss = function () {
      $('#realmModal').modal('hide');
    }

    $scope.setPage = function (page) {
      $scope.query.page = page;
      $scope.load();
    }

    init();
  })

