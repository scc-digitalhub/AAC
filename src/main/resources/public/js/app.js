var app = angular.module('dev', [
	'ngRoute',
	'ngResource',
	'angularSpinner',
	'ui.bootstrap',
	'aac.services', 
	'aac.controllers.main', 
	'aac.controllers.clients', 
	'aac.controllers.customservices', 
	'aac.controllers.apis', 
	'aac.controllers.admin' 
	]);

app.config(function ($httpProvider) {
	  $httpProvider.interceptors.push('loadingHttpInterceptor');
	})

app.run(function($rootScope){
	$rootScope.$on("$routeChangeStart", function (event, next, current) {
		if (!next.$$route || !next.$$route.originalPath) return;
		if (next.$$route.originalPath.indexOf('/apps') == 0)  $rootScope.currentView = 'apps';
		if (next.$$route.originalPath.indexOf('/apis') == 0)  $rootScope.currentView = 'apis';
		if (next.$$route.originalPath.indexOf('/admin') == 0)  $rootScope.currentView = 'admin';
	});	
})

app.config(function($routeProvider) {
    $routeProvider
    .when("/apps", {
    	controller  : 'AppController', 
        templateUrl : "html/apps.html"
    })
    .when("/admin", {
    	controller  : 'AdminController', 
        templateUrl : "html/admin.html"
    })
    .when("/apis", {
    	controller  : 'APIListController', 
        templateUrl : "html/apis.html"
    })
    .when("/apis/:apiId", {
    	controller  : 'APIController', 
        templateUrl : "html/apis.api.html"
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