var app = angular.module('dev', [
	'pascalprecht.translate',
	'ngRoute',
	'ngResource',
	'angularSpinner',
	'ngTagsInput',
	'aac.services', 
	'aac.controllers.main', 
	'aac.controllers.clients', 
	'aac.controllers.customservices', 
	'aac.controllers.apis', 
	'aac.controllers.admin',
	'ui.ace'
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
		lang = (lang.substring(0,lang.indexOf('-'))) || 'en';
		console.log('Detected language', lang);
		$translateProvider.preferredLanguage(lang);
	})

app.run(function($rootScope){
	$rootScope.$on("$routeChangeStart", function (event, next, current) {
		if (!next.$$route || !next.$$route.originalPath) return;
		if (next.$$route.originalPath.indexOf('/apps') == 0)  $rootScope.currentView = 'apps';
		if (next.$$route.originalPath.indexOf('/apis') == 0)  $rootScope.currentView = 'apis';
		if (next.$$route.originalPath.indexOf('/tenantusers') == 0)  $rootScope.currentView = 'tenantusers';
		if (next.$$route.originalPath.indexOf('/tenantowners') == 0)  $rootScope.currentView = 'tenantowners';
		if (next.$$route.originalPath.indexOf('/services') == 0)  $rootScope.currentView = 'services';
		if (next.$$route.originalPath.indexOf('/admin') == 0)  $rootScope.currentView = 'admin';
	});		
})

app.config(function($routeProvider) {
    $routeProvider
    .when("/apps", {
    	controller  : 'AppListController', 
        templateUrl : "html/apps.html"
    })
    .when("/apps/:clientId", {
    	controller  : 'AppController', 
        templateUrl : "html/app.html"
    })
    .when("/admin", {
    	controller  : 'AdminController', 
        templateUrl : "html/admin.html"
    })
    .when("/apis", {
    	controller  : 'APIListController', 
        templateUrl : "html/apis.html"
    })
    .when("/tenantusers", {
    	controller  : 'TenantUsersController', 
        templateUrl : "html/tenantusers.html"
    })
    .when("/tenantowners", {
    	controller  : 'TenantOwnersController', 
        templateUrl : "html/tenantowners.html"
    })
    .when("/services", {
    	controller  : 'ServicesController', 
        templateUrl : "html/services.html"
    })
    .when("/services/new", {
    	controller  : 'ServiceController', 
        templateUrl : "html/service-form.html"
    })
    .when("/services/:serviceId", {
    	controller  : 'ServiceController', 
        templateUrl : "html/service-form.html"
    })
    .otherwise("/apps");
})

/**
 * Interceptor adds loading spinner for REST calls
 */
app.factory('loadingHttpInterceptor', function ($q, $window, usSpinnerService) {
	return {
		request: function(config) {
			usSpinnerService.spin('spinner-1');
			return config;
		},
		response: function(response){
			usSpinnerService.stop('spinner-1');
		    return response;  
		},
		responseError: function(error) {
			usSpinnerService.stop('spinner-1');
			return $q.reject(error);
		}
	};
})

app.factory('accessDeniedInterceptor', function ($q, $location, $window) {
	return {
        request: function(config) {
            config.headers = config.headers || {};
            return config;
        },
        response: function(response) {
            if (response.data.code == 401) {
        	    window.document.location = "./logout";
            }
            return response || $q.when(response);
        },
		responseError: function(error) {
            if (error.status == 401) {
        	    window.document.location = "./logout";
            }
            return $q.reject(error);
		}
    };
})


;

/**
 * Parse authentication parameters obtained from implicit flow authorization request 
 * @param input
 * @returns
 */
function processAuthParams(input) {
	var params = {}, queryString = input;
	var regex = /([^&=]+)=([^&]*)/g;
	while (m = regex.exec(queryString)) {
	  params[m[1]] = m[2];
	}
	return params.access_token;
}