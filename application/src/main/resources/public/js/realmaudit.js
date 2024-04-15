angular.module('aac.controllers.realmaudit', [])
    /**
         * Realm Data Services
         */
    .service('RealmAudit', function ($q, $http) {
        var rService = {};

        rService.findEvents = function (slug, type, after, before) {
            var params = {}
            if (type) {
                params.type = type;
            }

            if (after) {
                params.after = after.toISOString();
            }

            if (before) {
                params.before = before.toISOString();
            }
            return $http.get('console/dev/audit/' + slug, {
                params: params
            }).then(function (data) {
                return data.data;
            });
        }

        rService.searchEvents = function (slug, type, after, before, page, size) {
            var params = {}
            if (type) {
                params.type = type;
            }

            if (after) {
                params.after = after.toISOString();
            }

            if (before) {
                params.before = before.toISOString();
            }

            if (page) {
                params.page = page;
            } else {
                params.page = 0;
            }

            if (size) {
                params.size = size;
            } else {
                params.size = 20;
            }

            return $http.get('console/dev/audit/' + slug + '/search', {
                params: params
            }).then(function (data) {
                return data.data;
            });
        }

        return rService;
    })
    /**
      * Realm providers controller
      */
    .controller('RealmAuditController', function ($scope, $state, $stateParams, RealmAudit, Utils) {
        var slug = $stateParams.realmId;

        $scope.load = function (after, before, type, page, size) {
            RealmAudit.searchEvents(slug, type, after, before, page, size)
                .then(function (data) {
                    $scope.events = data;
                })
                .catch(function (err) {
                    Utils.showError('Failed to load realm audit events: ' + err.data.message);
                });
        }


        var init = function () {
            $scope.eventTypes = [
                'USER_AUTHENTICATION_SUCCESS', 'USER_AUTHENTICATION_FAILURE',
                'CLIENT_AUTHENTICATION_SUCCESS', 'CLIENT_AUTHENTICATION_FAILURE',
                'OAUTH2_TOKEN_GRANT',
                'OIDC_MESSAGE',
                'SAML_REQUEST'
            ];

            $scope.filterType = null;
            //default filter lists today events
            var after = new Date();
            after.setHours(0);
            after.setMinutes(0);
            after.setSeconds(0);
            after.setMilliseconds(0);
            var before = new Date();

            $scope.filterAfter = after;
            $scope.filterBefore = before;

            $scope.page = 0;
            $scope.size = 20;

            $scope.load(after, before, null, $scope.page, $scope.size);
        }

        $scope.reload = function () {
            var after = $scope.filterAfter;
            var before = $scope.filterBefore;
            var type = ($scope.filterType ? $scope.filterType : null);

            $scope.page = 0;
            $scope.size = 20;

            $scope.load(after, before, type, $scope.page, $scope.size);
        }

        $scope.setPage = function (page) {
            var after = $scope.filterAfter;
            var before = $scope.filterBefore;
            var type = ($scope.filterType ? $scope.filterType : null);

            $scope.page = page;
            $scope.size = 20;

            $scope.load(after, before, type, $scope.page, $scope.size);
        }

        $scope.auditEventDlg = function (item) {
            $scope.modEvent = item;
            $('#auditEventModal').modal({ keyboard: false });

        }

        init();

    })

