angular.module('aac.controllers.admin', [])
/**
 * Main layout controller
 * @param $scope
 */
.controller('AdminController', function ($scope, $resource) {
	// error message
	$scope.error = '';
	// info message
	$scope.info = '';
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
				$scope.error = '';
				$scope.approvals = response.data;
			} else {
				$scope.error = 'Failed to load approval requests: '+response.errorMessage;
			}	
		});
		IdPApprovals.query(function(response){
			if (response.responseCode == 'OK') {
				$scope.error = '';
				$scope.idps = response.data;
			} else {
				$scope.error = 'Failed to load IdP requests: '+response.errorMessage;
			}	
		});
	};
	init();
	
	$scope.approve = function(clientId) {
		var newClient = new ClientApprovals();
		newClient.$approve({clientId:clientId},function(response){
			if (response.responseCode == 'OK') {
				$scope.error = '';
				$scope.info = 'Approved successfully';
				$scope.approvals = response.data;
			} else {
				$scope.error = 'Failed to approve resource access: '+response.errorMessage;
			}	
		});
	};
	
	$scope.approveIdP = function(clientId) {
		var newClient = new IdPApprovals();
		newClient.$approve({clientId:clientId},function(response){
			if (response.responseCode == 'OK') {
				$scope.error = '';
				$scope.info = 'IdP approved successfully';
				$scope.idps = response.data;
			} else {
				$scope.error = 'Failed to approve IdP: '+response.errorMessage;
			}	
		});
	};
})

.controller('APIProviderController', function ($scope, APIProviders, Utils) {

	var resetPage = function(){
		$scope.page = {
		  offset: 0,
		  limit: 25,
		  totalItems: 0,
		  currentPage: 1
		};
	}
	resetPage();
	
	// page changed
	$scope.pageChanged = function() {
		$scope.page.offset = ($scope.page.currentPage - 1) * $scope.page.limit;
		loadData();
	};

	// load providers
	var loadData = function(){
		APIProviders.getProviders($scope.page.offset, $scope.page.limit).then(function(data){
	    	$scope.providers = data.list;
			var count = (($scope.page.currentPage-1) * $scope.page.limit + data.count);
			$scope.page.totalItems = 
				data.count < $scope.page.limit ? count : (count + 1);			
	    }, Utils.showError);
	}
	loadData();

	// create provider
	$scope.createProvider = function() {
		$scope.providerDlg.close();
		APIProviders.createProvider($scope.provider).then(function(){
			$('#providerModal').modal('hide');
			Utils.showSuccess();
			resetPage();
			loadData();
		},Utils.showError);
	}
	$scope.dismiss = function(){
		$('#providerModal').modal('hide');
	}
	
	$scope.newProvider = function(){
		$scope.provider = {};
		$('#providerModal').modal({backdrop: 'static', focus: true})
//		$scope.providerDlg = $uibModal.open({
//	      ariaLabelledBy: 'modal-title',
//	      ariaDescribedBy: 'modal-body',
//	      templateUrl: 'html/provider.modal.html',
//	      scope: $scope,
//	      size: 'lg'
//	    });
	}
})
