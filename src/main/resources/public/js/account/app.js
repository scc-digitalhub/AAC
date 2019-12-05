var app = angular.module('account', [
	'pascalprecht.translate',
	'ngRoute',
	'ngResource',
	'angularSpinner',
	'ngTagsInput',
	'aac.controllers.main', 
	'aac.services',
	'ui.ace'
	]);

app.config(function ($httpProvider, $translateProvider) {
	  $httpProvider.interceptors.push('loadingHttpInterceptor');
	  $httpProvider.interceptors.push('accessDeniedInterceptor');
	  $translateProvider.useStaticFilesLoader({
		    prefix: 'i18n/account/locale-',
		    suffix: '.json'
		});
		$translateProvider.preferredLanguage('en');
	})

app.run(function($rootScope){
	$rootScope.$on("$routeChangeStart", function (event, next, current) {
		if (!next.$$route || !next.$$route.originalPath) return;
		if (next.$$route.originalPath.indexOf('/') == 0)  $rootScope.currentView = '';
		if (next.$$route.originalPath.indexOf('/accounts') == 0)  $rootScope.currentView = 'accounts';
		if (next.$$route.originalPath.indexOf('/connections') == 0)  $rootScope.currentView = 'connections';
	});		
})

app.config(function($routeProvider) {
    $routeProvider
    .when("/", {
    	controller  : 'HomeController', 
        templateUrl : "html/account/home.html"
    })
    .when("/accounts", {
    	controller  : 'AccountsController', 
        templateUrl : "html/account/accounts.html"
    })
    .when("/connections", {
    	controller  : 'ConnectionsController', 
        templateUrl : "html/account/connections.html"
    })
    .when("/profile", {
    	controller  : 'ProfileController', 
        templateUrl : "html/account/profile.html"
    })
    .otherwise("/");
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