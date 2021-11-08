function buildQuery(params, serializer) {
	var q = Object.assign({}, params);
	if (q.sort) q.sort = Object.keys(q.sort).map(function (k) { return k + ',' + (q.sort[k] > 0 ? 'asc' : 'desc'); });
	var queryString = serializer(q);
	return queryString;
}


angular.module('aac.controllers.rolespaces', [])

	.service('RoleSpaceData', function ($q, $http, $rootScope, $httpParamSerializer) {
		var rsService = {};
		rsService.getMySpaces = function () {
			return $http.get('console/dev/rolespaces').then(function (data) {
				return data.data;
			});
		}

		rsService.getSpaceUsers = function (context, space, params) {
			return $http.get('console/dev/rolespaces/users?context=' + (context || '') + '&space=' + (space || '') + '&' + buildQuery(params, $httpParamSerializer)).then(function (data) {
				return data.data;
			});
		}

		rsService.saveRoles = function (subject, context, space, roles) {
			return $http.post('console/dev/rolespaces/users', { subject: subject, space: space, context: context, roles: roles }).then(function (data) {
				return data.data;
			});
		}

		return rsService;
	})

	/**
	 * List of RoleSpace space User Roles controller
	 */
	.controller('RoleSpaceController', function ($scope, RoleSpaceData, Data, Utils) {
		$scope.query = {
			page: 0,
			size: 20,
			sort: { subject: 1 },
			q: ''
		}
		$scope.currentSpaceRole = null;

		$scope.load = function () {
			RoleSpaceData.getSpaceUsers($scope.currentSpaceRole.context, $scope.currentSpaceRole.space, $scope.query)
				.then(function (data) {
					$scope.users = data;
				})
				.catch(function (err) {
					Utils.showError('Failed to load spaces and users: ' + err.data.message);
				});
		}

		/**
		 * Initialize the app: load list of the users
		 */
		var init = function () {
			Data.getProfileRoles()
				.then(function (data) {
					data = data.filter(function (r) {
						return r.role == 'ROLE_PROVIDER';
					});
					data.forEach(function (r) {
						r.strId = (r.context || '-') + '/' + (r.space || '-');
						r.label = (r.context || '');
						if (r.label) r.label += ' / ';
						r.label += (r.space || '');
						if (!r.label) r.label = '-- ROOT --';
					});
					data.sort(function (a, b) {
						var res = (a.context || '').localeCompare(b.context || '');
						if (res != 0) return res;
						return (a.space || '').localeCompare(b.space || '');
					});
					$scope.spaceRoles = data;
					if (!$scope.currentSpaceRole) {
						$scope.currentSpaceRole = $scope.spaceRoles[0];
						$scope.load();
					}
					Utils.refreshFormBS();
				})
				.catch(function (err) {
					Utils.showError('Failed to load spaces and users: ' + err.data.message);
				});
		};

		init();

		$scope.addSubject = function () {
			$scope.roles = {
				system_map: {}, map: {}, custom: '', withSubject: true, subject: '', space: ''
			}
			$('#rolesModal').modal({ backdrop: 'static', focus: true })
			Utils.refreshFormBS();
		}

		$scope.changeRoles = function (subj) {
			$scope.roles = {
				system_map: {}, map: {}, custom: '', withSubject: false, subject: subj.subject, space: ''
			}
			subj.roles.forEach(function (r) {
				if (r == 'ROLE_PROVIDER') $scope.roles.system_map[r] = true;
				else $scope.roles.map[r] = true;
			});
			$('#rolesModal').modal({ backdrop: 'static', focus: true })
			Utils.refreshFormBS();

		}

		$scope.hasRoles = function (m1, m2) {
			var res = false;
			for (var r1 in m1) res |= m1[r1];
			for (var r2 in m2) res |= m2[r2];
			return res;
		}

		// save roles
		$scope.updateRoles = function () {
			var roles = [];
			for (var k1 in $scope.roles.system_map) {
				if ($scope.roles.system_map[k1]) {
					roles.push(k1);
				}
			}
			for (var k2 in $scope.roles.map) {
				if ($scope.roles.map[k2]) {
					roles.push(k2);
				}
			}

			$('#rolesModal').modal('hide');
			var context = $scope.currentSpaceRole.context;
			var space = $scope.currentSpaceRole.space;
			if ($scope.roles.space) {
				context = (context ? (context + '/') : '') + (space || '');
				space = $scope.roles.space;
			}
			RoleSpaceData.saveRoles($scope.roles.subject, context, space, roles)
				.then(function () {
					if ($scope.roles.space) {
						init();
					}
					$scope.load();
					Utils.showSuccess();
				})
				.catch(function (err) {
					Utils.showError(err);
				});
		}
		$scope.dismiss = function () {
			$('#rolesModal').modal('hide');
		}

		$scope.addRole = function () {
			$scope.roles.map[$scope.roles.custom] = true;
			$scope.roles.custom = null;
		}

		$scope.invalidRole = function (role) {
			return !role || !(/^[a-zA-Z0-9_]{1,63}((\.[a-zA-Z0-9_]+)*\.[a-zA-Z]+)?$/g.test(role))
		}

		$scope.changeSpace = function (val) {
			$scope.currentSpaceRole = val;
			$scope.load();
		}


	})

	/**
	 * List of RoleSpace space User Roles controller
	 */
	.controller('RoleSpaceUsersController', function ($scope, Data, Utils) {
		$scope.contexts = { selected: null, all: null };

		var reset = function () {
			$scope.page = {
				offset: 0,
				limit: 10,
				totalItems: 0,
				currentPage: 1
			};
		}

		// page changed
		$scope.pageChanged = function () {
			$scope.page.offset = ($scope.page.currentPage - 1) * $scope.page.limit;
			loadData();
		};
		$scope.nextPage = function () {
			$scope.page.currentPage++;
			$scope.page.offset = ($scope.page.currentPage - 1) * $scope.page.limit;
			loadData();
		};
		$scope.prevPage = function () {
			$scope.page.currentPage--;
			$scope.page.offset = ($scope.page.currentPage - 1) * $scope.page.limit;
			loadData();
		};

		// load users
		var loadUsers = function () {
			Data.getContextUsers($scope.page.offset, $scope.page.limit, $scope.contexts.selected).then(function (data) {
				data.list.forEach(function (d) {
					d.roles = d.roles.map(function (r) {
						return r.role;
					});
				});
				$scope.users = data.list;
				if (!data.count) data.count = data.list.length;
				var count = (($scope.page.currentPage - 1) * $scope.page.limit + data.count);
				$scope.page.totalItems =
					data.count < $scope.page.limit ? count : (count + 1);
			}, Utils.showError);
		}

		// load data
		var loadData = function () {
			if (!$scope.contexts.all) {
				Data.getMyContexts().then(function (data) {
					$scope.contexts.all = data;
					$scope.contexts.selected = data && data.length > 0 ? data[0] : null;
					if ($scope.contexts.selected) loadUsers();
				}, Utils.showError);
			} else {
				loadUsers();
			}
		}
		$scope.loadData = loadData;

		reset();
		loadData();

		// toggle roles of the subscribed user
		$scope.changeRoles = function (sub) {
			if (!$scope.contexts.selected) return;

			var roleMap = {};
			if (sub.roles) {
				sub.roles.forEach(function (r) {
					roleMap[r] = true;
				});
			}
			$scope.roles = { map: roleMap, sub: sub };

			$('#rolesModal').modal({ backdrop: 'static', focus: true })
		}

		// add roles to a new user
		$scope.newUser = function () {
			var roleMap = {};
			$scope.roles = { map: roleMap, sub: { username: null, usernameRequired: true } };
			$('#rolesModal').modal({ backdrop: 'static', focus: true })
		}

		$scope.hasRoles = function (map) {
			var res = false;
			for (var r in map) res |= map[r];
			return res;
		}

		// save roles
		$scope.updateRoles = function () {
			// TODO fix the implementation and the error handling
			Data.updateUserRolesInContext($scope.roles.sub.username, $scope.roles.map, $scope.roles.sub.roles, $scope.contexts.selected).then(function (newRoles) {
				$scope.users.forEach(function (u) {
					if (newRoles == null || newRoles.length == 0) {
						loadData();
					}
					else if (u.userId == $scope.roles.sub.userId) {
						u.roles = newRoles;
					}
				});
				if ($scope.roles.sub.usernameRequired) {
					reset();
					loadData();
				}
				$('#rolesModal').modal('hide');
				Utils.showSuccess();
				$scope.roles = null;
			}, function (err) {
				$('#rolesModal').modal('hide');
				Utils.showError(err);
			});
		}
		$scope.dismiss = function () {
			$('#rolesModal').modal('hide');
		}

		$scope.addRole = function () {
			$scope.roles.map[$scope.roles.custom] = true;
			$scope.roles.custom = null;
		}
		$scope.changeRole = function () {
			console.log('changed');
		}
		$scope.invalidRole = function (role) {
			return !role || !(/^[a-zA-Z0-9_]{3,63}((\.[a-zA-Z0-9_]{2,63})*\.[a-zA-Z]{2,63})?$/g.test(role))
		}

	})

	/**
	 * List of RoleSpace space User Owners controller
	 */
	.controller('RoleSpaceOwnersController', function ($scope, $rootScope, $location, Data, Utils) {
		$scope.contexts = { selected: null, all: null };

		var reset = function () {
			$scope.page = {
				offset: 0,
				limit: 10,
				totalItems: 0,
				currentPage: 1
			};
		}

		// page changed
		$scope.pageChanged = function () {
			$scope.page.offset = ($scope.page.currentPage - 1) * $scope.page.limit;
			loadData();
		};
		$scope.nextPage = function () {
			$scope.page.currentPage++;
			$scope.page.offset = ($scope.page.currentPage - 1) * $scope.page.limit;
			loadData();
		};
		$scope.prevPage = function () {
			$scope.page.currentPage--;
			$scope.page.offset = ($scope.page.currentPage - 1) * $scope.page.limit;
			loadData();
		};

		var loadUsers = function () {
			Data.getContextOwners($scope.page.offset, $scope.page.limit, $scope.contexts.selected).then(function (data) {
				data.list.forEach(function (d) {
					d.roles = d.roles.map(function (r) {
						return r.space;
					});
				});
				$scope.users = data.list;
				if (!data.count) data.count = data.list.length;
				var count = (($scope.page.currentPage - 1) * $scope.page.limit + data.count);
				$scope.page.totalItems =
					data.count < $scope.page.limit ? count : (count + 1);
			}, Utils.showError);
		}

		// load data
		var loadData = function () {
			if (!$scope.contexts.all) {
				Data.getMyContexts().then(function (data) {
					$scope.contexts.all = data;
					$scope.contexts.selected = data && data.length > 0 ? data[0] : null;
					if ($scope.contexts.selected) loadUsers();
				}, Utils.showError);
			} else {
				loadUsers();
			}
		}
		var updateContexts = function () {
			Data.getMyContexts().then(function (data) {
				$scope.contexts.all = data;
			});

		}

		$scope.loadData = loadData;

		reset();
		loadData();

		// toggle roles of the subscribed user
		$scope.changeRoles = function (sub) {
			if (!$scope.contexts.selected) return;

			var roleMap = {};
			if (sub.roles) {
				sub.roles.forEach(function (r) {
					roleMap[r] = true;
				});
			}
			$scope.roles = { map: roleMap, sub: sub };

			$('#rolesModal').modal({ backdrop: 'static', focus: true })
		}

		// add roles to a new user
		$scope.newUser = function () {
			var roleMap = {};
			$scope.roles = { map: roleMap, sub: { username: null, usernameRequired: true } };
			$('#rolesModal').modal({ backdrop: 'static', focus: true })
		}

		$scope.hasRoles = function (map) {
			var res = false;
			for (var r in map) res |= map[r];
			return res;
		}

		// save roles
		$scope.updateRoles = function () {
			Data.updateOwnersInContext($scope.roles.sub.username, $scope.roles.map, $scope.roles.sub.roles, $scope.contexts.selected).then(function (newRoles) {
				$scope.users.forEach(function (u) {
					if (newRoles == null || newRoles.length == 0) {
						loadData();
					}
					else if (u.userId == $scope.roles.sub.userId) {
						u.roles = newRoles;
					}
				});
				if ($scope.roles.sub.usernameRequired) {
					reset();
					loadData();
				}
				$('#rolesModal').modal('hide');
				updateContexts();
				Utils.showSuccess();
				$scope.roles = null;
			}, function (err) {
				$('#rolesModal').modal('hide');
				Utils.showError(err);
			});
		}
		$scope.dismiss = function () {
			$('#rolesModal').modal('hide');
		}

		$scope.addRole = function () {
			$scope.roles.map[$scope.roles.custom] = true;
			$scope.roles.custom = null;
		}
		$scope.invalidRole = function (role) {
			return !role || !(/^[a-zA-Z0-9_]{3,63}((\.[a-zA-Z0-9_]{2,63})*\.[a-zA-Z]{2,63})?$/g.test(role))
		}

	})