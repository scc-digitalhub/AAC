angular.module('aac.controllers.admin', [])
/**
 * Main layout controller
 * @param $scope
 */
.controller('AdminController', function ($scope, $resource, Utils) {
	// title
	$scope.title = 'App Approvals';
	
	$scope.adminView = 'approvals';

	// resource reference for the approval API
	var ClientApprovals = $resource('admin/approvals/:clientId', {}, {
		query : { method : 'GET' },
		approve : {method : 'POST'}
	});

	// resource reference for the IdP API
	var IdPApprovals = $resource('admin/idps/:clientId', {}, {
		query : { method : 'GET' },
		approve : {method : 'POST'}
	});

	/**
	 * Initialize the app: load list of the developer's apps and reset views
	 */
	var init = function() {
		ClientApprovals.query(function(response){
			if (response.responseCode == 'OK') {
				$scope.approvals = response.data;
			} else {
				Utils.showError('Failed to load approval requests: '+response.errorMessage);
			}	
		});
		IdPApprovals.query(function(response){
			if (response.responseCode == 'OK') {
				$scope.idps = response.data;
			} else {
				Utils.showError('Failed to load IdP requests: '+response.errorMessage);
			}	
		});
	};
	init();
	
	$scope.approve = function(clientId) {
		var newClient = new ClientApprovals();
		newClient.$approve({clientId:clientId},function(response){
			if (response.responseCode == 'OK') {
				$scope.approvals = response.data;
				Utils.showSuccess();
			} else {
				Utils.showError('Failed to approve scope access: '+response.errorMessage);
			}	
		});
	};
	
	$scope.approveIdP = function(clientId) {
		var newClient = new IdPApprovals();
		newClient.$approve({clientId:clientId},function(response){
			if (response.responseCode == 'OK') {
				$scope.idps = response.data;
				Utils.showSuccess();
			} else {
				Utils.showError('Failed to approve IdP: '+response.errorMessage);
			}	
		});
	};
})

