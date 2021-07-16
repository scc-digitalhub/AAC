var app = angular.module('dev', [
    'pascalprecht.translate',
    //'ngRoute',
    'ui.router',
    'ngResource',
    'angularSpinner',
    'ngTagsInput',
    'aac.services',
    'aac.controllers.main',
    'aac.controllers.admin',
    'aac.controllers.realm',
    'aac.controllers.realmproviders',
    'aac.controllers.realmapps',
    'aac.controllers.realmservices',
    'aac.controllers.realmaudit',
    'aac.controllers.rolespaces',
    'aac.controllers.realmattributesets',
    'ui.ace',
    'ngSanitize'
]);

app.config(function ($httpProvider, $translateProvider) {
    $httpProvider.interceptors.push('loadingHttpInterceptor');
    $httpProvider.interceptors.push('accessDeniedInterceptor');
    $translateProvider.useStaticFilesLoader({
        prefix: 'i18n/locale-',
        suffix: '.json'
    });
    var lang = navigator.languages
        ? navigator.languages[0]
        : (navigator.language || navigator.userLanguage);
    lang = (lang.substring(0, lang.indexOf('-'))) || 'en';
    console.log('Detected language', lang);
    $translateProvider.preferredLanguage(lang);
});

app.config(function ($stateProvider) {
    $stateProvider
        .state('admin', {
            url: '/admin',
            templateUrl: 'html/admin.html',
            controller: 'AdminController',
        })
        .state('realm', {
            url: '/realm?realmId',
            templateUrl: 'html/realm.html',
            controller: 'RealmController',
            redirectTo: 'realm.dashboard'
        })
        .state('realm.dashboard', {
            url: '/dashboard',
            templateUrl: 'html/realm.dashboard.html',
            controller: 'RealmDashboardController',
        })
        .state('realm.users', {
            url: '/users',
            templateUrl: 'html/realm.users.html',
            controller: 'RealmUsersController',
        })
        .state('realm.providers', {
            url: '/providers',
            templateUrl: 'html/realm.providers.html',
            controller: 'RealmProvidersController',
        })
        .state('realm.provider', {
            url: '/provider?providerId',
            templateUrl: 'html/realm.provider.html',
            controller: 'RealmProviderController',
        })
        .state('realm.apps', {
            url: '/apps',
            templateUrl: 'html/realm.apps.html',
            controller: 'RealmAppsController',
        })
        .state('realm.app', {
            url: '/app?clientId',
            templateUrl: 'html/realm.app.html',
            controller: 'RealmAppController',
        })
        .state('realm.appstart', {
            url: '/app.start?clientId',
            templateUrl: 'html/realm.app.start.html',
            controller: 'RealmAppStartController',
        })
        .state('realm.services', {
            url: '/services',
            templateUrl: 'html/realm.services.html',
            controller: 'RealmServicesController',
        })
        .state('realm.service', {
            url: '/service?serviceId',
            templateUrl: 'html/realm.services.service.html',
            controller: 'RealmServiceController',
        })
        .state('realm.serviceapprovals', {
            url: '/serviceapprovals?serviceId',
            templateUrl: 'html/realm.services.approvals.html',
            controller: 'RealmServiceApprovalsController',
        })
        .state('realm.scopes', {
            url: '/scopes',
            templateUrl: 'html/realm.scopes.html',
            controller: 'RealmScopesController',
        })
        .state('realm.custom', {
            url: '/custom',
            templateUrl: 'html/realm.custom.html',
            controller: 'RealmCustomController',
        })
        .state('realm.audit', {
            url: '/audit',
            templateUrl: 'html/realm.audit.html',
            controller: 'RealmAuditController',
        })
        .state('realm.settings', {
            url: '/settings',
            templateUrl: 'html/realm.settings.html',
            controller: 'RealmSettingsController',
        })
        .state('realm.attributesets', {
            url: '/attributesets',
            templateUrl: 'html/realm.attributesets.html',
            controller: 'RealmAttributeSetsController',
        })
        .state('realm.attributeset', {
            url: '/attributeset?setId',
            templateUrl: 'html/realm.attributeset.html',
            controller: 'RealmAttributeSetController',
        })
        .state('rolespaces', {
            url: '/rolespaces',
            templateUrl: 'html/rolespaces.html',
            controller: 'RoleSpaceController',
        })
        .state('home', {
            url: '',
            controller: 'HomeController',
        });

});


/**
 * Interceptor adds loading spinner for REST calls
 */
app.factory('loadingHttpInterceptor', function ($q, $window, usSpinnerService) {
    return {
        request: function (config) {
            usSpinnerService.spin('spinner-1');
            return config;
        },
        response: function (response) {
            usSpinnerService.stop('spinner-1');
            return response;
        },
        responseError: function (error) {
            usSpinnerService.stop('spinner-1');
            return $q.reject(error);
        }
    };
});

app.factory('accessDeniedInterceptor', function ($q, $location) {
    return {
        request: function (config) {
            config.headers = config.headers || {};
            return config;
        },
        response: function (response) {
            if (response.data.code == 401) {
                window.location.href = "./logout";
            }
            return response || $q.when(response);
        },
        responseError: function (error) {
            if (error.status == 401) {
                window.location.href = "./logout";
            }
            return $q.reject(error);
        }
    };
});


