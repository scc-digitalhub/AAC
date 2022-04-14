angular.module('aac.controllers.main', [])

   /**
    * Main layout controller
    * @param $scope
    */
   .controller('MainCtrl', function($scope, $rootScope, $location, Data, Utils) {
      $scope.go = function(v) {
         $location.path(v);
      }


      $scope.activeView = function(view) {
         return view == $rootScope.currentView ? 'active' : '';
      };
      $scope.signOut = function() {
         window.document.location = "./logout";
      };

      Data.getProfile().then(function(data) {
         $rootScope.user = data;
         $rootScope.isDev = data.authorities.findIndex(function(a) {
            return a.role === 'ROLE_DEVELOPER' || a.role === 'ROLE_ADMIN';
         }) >= 0;
      }).catch(function(err) {
         Utils.showError(err);
      });

      //Utils.initUI();
   })

   .controller('HomeController', function($scope, $rootScope, $location) {
   })
   .controller('AccountsController', function($scope, $rootScope, $location, Data, Utils) {

      $scope.load = function() {
         Data.getProfile()
            .then(function(profile) {
               $scope.profile = profile;
               return profile;
            })
            .then(function() {
               return Data.getProfiles()
            })
            .then(function(profiles) {
               $scope.profiles = profiles;
               return profiles;
            })
            .then(function() {
               return Data.getAccounts()
            })
            .then(function(accounts) {
               $scope.accounts = accounts;
               //         var providers = [];
               //         var accounts = {};
               //         data.forEach(function(a) {
               //            if (a.provider !== 'internal') providers.push(a.provider);
               //            accounts[a.provider] = Object.assign({}, a.attributes);
               //            accounts[a.provider].username = a.username;
               //         });
               //         $scope.providers = providers;
               //         $scope.accounts = accounts;
               return accounts;
            })
            .then(function() {
               return Data.getProviders()
            })
            .then(function(providers) {
               var idps = providers.filter(p => p.type === 'identity');
               idps.forEach(function(idp) {
                  idp.icon = iconProvider(idp);
               });
               $scope.providers = idps;
               return idps;
            })
            .then(function(providers) {
               var map = new Map(providers.map(e => [e.provider, e]));
               //merge with account details
               var accounts = $scope.accounts;
               accounts.forEach(function(ac) {
                  var provider = map.get(ac.provider);
                  ac.provider = provider;
               });
               $scope.accounts = accounts;

            })
            .catch(function(err) {
               Utils.showError(err);
            });
      }

      $scope.updateCredentials = function(authority, provider, accountId) {
         if(authority == 'internal') {
            //redirect to changepwd
            //TODO handle in service
            window.location.href = './changepwd/'+provider+'/' + accountId;
         }
//         //split userid and redirect
//         var path = userId.replaceAll("|", "/");
      }

      $scope.confirmDeleteAccount = function() {
         $('#deleteConfirm').modal({ keyboard: false });
      }

      $scope.deleteAccount = function() {
         $('#deleteConfirm').modal('hide');
         Data.deleteAccount().then(function() {
            window.location.href = './logout';
         }).catch(function(err) {
            Utils.showError(err);
         });
      }

      var iconProvider = function(idp) {
         var icons = ['facebook', 'google', 'microsoft', 'apple', 'instagram', 'github'];

         if (idp.authority === "oidc") {
            var logo = null;
            if ('clientName' in idp.configuration && icons.includes(idp.configuration.clientName.toLowerCase())) {
               logo = idp.configuration.clientName.toLowerCase();
            } else if (icons.includes(idp.name.toLowerCase())) {
               logo = idp.name.toLowerCase();
            }

            if (logo) {
               return './svg/sprite.svg#logo-' + logo;
            }
         }
         if (idp.authority === "spid") {
            return './spid/sprite.svg#spid-ico-circle-bb';
         }
         return './italia/svg/sprite.svg#it-unlocked';
      }

      $scope.load();
   })
   .controller('ConnectionsController', function($scope, $rootScope, $location, Data, Utils) {
      Data.getConnections().then(function(connections) {
         $scope.connections = connections;
      }).catch(function(err) {
         Utils.showError(err);
      });

      $scope.confirmDeleteApp = function(app) {
         $scope.clientId = app.clientId;
         $('#deleteConfirm').modal({ keyboard: false });
      }

      $scope.deleteApp = function() {
         $('#deleteConfirm').modal('hide');
         Data.removeConnection($scope.clientId).then(function(connections) {
            $scope.connections = connections;
            Utils.showSuccess();
         }).catch(function(err) {
            Utils.showError(err);
         });
      }

   })
   .controller('ProfileController', function($scope, $rootScope, $location, Data, Utils) {
      $scope.profile = { name: $rootScope.user.firstName, surname: $rootScope.user.lastName, username: $rootScope.user.username, email: $rootScope.user.emailAddress };

      $scope.cancel = function() {
         window.history.back();
      }

      $scope.save = function() {
         if (!$scope.profile.name ||
            !$scope.profile.surname ||
            !$scope.profile.username ||
            $scope.profile.password && $scope.profile.password != $scope.profile.password2) {
            return;
         }
         Data.saveAccount($scope.profile).then(function(data) {
            $rootScope.user = data;
            $scope.profile = Object.assign($rootScope.user);
            Utils.showSuccess();
         }).catch(function(err) {
            Utils.showError(err);
         });
      }
      Utils.initUI();
   })
   ;