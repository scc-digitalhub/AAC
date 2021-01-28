/*******************************************************************************
 * Copyright 2015 Fondazione Bruno Kessler
 * 
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 ******************************************************************************/

package it.smartcommunitylab.aac.controller;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.dto.DataList;
import it.smartcommunitylab.aac.dto.UserDTO;
import it.smartcommunitylab.aac.manager.RoleManager;
import it.smartcommunitylab.aac.manager.UserManager;
import it.smartcommunitylab.aac.model.ErrorInfo;
import it.smartcommunitylab.aac.model.Response;
import it.smartcommunitylab.aac.model.Role;
import it.smartcommunitylab.aac.roles.dto.RoleModel;

/**
 * @author raman
 *
 */
@Controller
public class UserManagementController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private RoleManager roleManager;
	@Autowired
	private UserManager userManager;	

	@GetMapping("/mgmt/users")
	public @ResponseBody DataList<UserDTO> spaceUsers(
			@RequestParam String context,
			@RequestParam(required=false, defaultValue="0") Integer offset, 
			@RequestParam(required=false, defaultValue="25") Integer limit) 
	{
		DataList<UserDTO> dataList = new DataList<>();
		
		Set<Role> ownedSpaces = userManager.getUser().contextRole(Config.R_PROVIDER);
		
		offset = offset / limit;

		Role role = Role.ownerOf(context);
		if (!ownedSpaces.isEmpty() && ownedSpaces.contains(role)) {
			dataList.setList(roleManager.findUsersByContext(role.getContext(), role.getSpace(), offset, limit) 
					.stream().map(u -> UserDTO.fromUser(u, role.getContext(), role.getSpace())).collect(Collectors.toList()));
		} else {
			throw new AccessDeniedException("Not an owner of any role space");
		}
		
		return dataList;
	}
	
	@PutMapping("/mgmt/userroles")
	public @ResponseBody List<String> updateRoles(@RequestParam String context, @RequestBody RoleModel roleModel) throws Exception 
	{
		Role role = Role.ownerOf(context);
		Set<Role> ownedSpaces = userManager.getUser().contextRole(Config.R_PROVIDER);
		if (!ownedSpaces.isEmpty() && ownedSpaces.contains(role)) {
			// cannot remove the ROLE_PROVIDER role from the same user
			if (userManager.getUser().getUsername().equals(roleModel.getUser()) && roleModel.getRemoveRoles() != null && roleModel.getRemoveRoles().contains(Config.R_PROVIDER)) {
				throw new IllegalArgumentException("Cannot remove space ownership for the same user");
			}
			return roleManager.updateLocalRoles(roleModel, role.getContext(), role.getSpace());
		} else {
			throw new AccessDeniedException("Not an owner of any role space");
		}
	} 
	
	@GetMapping("/mgmt/spaceowners")
	public @ResponseBody DataList<UserDTO> spaceOwners(
			@RequestParam String context,
			@RequestParam(required=false, defaultValue="0") Integer offset, 
			@RequestParam(required=false, defaultValue="25") Integer limit) 
	{
		DataList<UserDTO> dataList = new DataList<>();
		
		Set<Role> ownedSpaces = userManager.getUser().contextRole(Config.R_PROVIDER);
		
		offset = offset / limit;

		Role role = Role.ownerOf(context);
		String canonical = role.canonicalSpace();
		if (!ownedSpaces.isEmpty() && ownedSpaces.contains(role)) {
			dataList.setList(roleManager.findUsersByRole(Config.R_PROVIDER, role.canonicalSpace(), offset, limit) 
					.stream().map(user -> {
						UserDTO res = new UserDTO();
						res.setFullname(user.getFullName());
						res.setUserId(user.getId().toString());
						res.setUsername(user.getUsername());
						res.setRoles(user.contextRole(Config.R_PROVIDER, canonical));
						return res;
					}).collect(Collectors.toList()));
		} else {
			throw new AccessDeniedException("Not an owner of any role space");
		}
		
		return dataList;
	}
	
	@GetMapping("/mgmt/spaceowners/me")
	public @ResponseBody Set<String> getMySpaces() {
		Set<Role> userRoles = userManager.getUser().getRoles();
		return userRoles.stream().filter(r -> r.getRole().equals(Config.R_PROVIDER)).map(Role::canonicalSpace).collect(Collectors.toSet());
	} 
	
	@PutMapping("/mgmt/spaceowners")
	public @ResponseBody List<String> updateSpaces(@RequestParam String context, @RequestBody RoleModel roleModel) throws Exception 
	{
		Role role = Role.ownerOf(context);
		Set<Role> ownedSpaces = userManager.getUser().contextRole(Config.R_PROVIDER);
		
		// update object: the input role list is intended as a list of new spaces
		if (roleModel.getRemoveRoles() == null) roleModel.setRemoveRoles(new LinkedList<>());
		
		if (!ownedSpaces.isEmpty() && ownedSpaces.contains(role)) {
			// cannot remove the ROLE_PROVIDER role from the same user
			if (userManager.getUser().getUsername().equals(roleModel.getUser()) && !roleModel.getRemoveRoles().isEmpty()) {
				throw new IllegalArgumentException("Cannot remove space ownership for the same user");
			}
			List<String> owners = roleManager.updateLocalOwners(roleModel, role.getContext(), role.getSpace());
			return owners;
		} else {
			throw new AccessDeniedException("Not an owner of any role space");
		}
	} 

	@ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ResponseBody
    Response processAccessError(AccessDeniedException ex) {
		return Response.error(ex.getMessage());
    }

	@ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public Response processValidationError(IllegalArgumentException ex) {
		return Response.error(ex.getMessage());
    }

	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	@ExceptionHandler(Exception.class)
	@ResponseBody
	ErrorInfo handleBadRequest(HttpServletRequest req, Exception ex) {
		StackTraceElement ste = ex.getStackTrace()[0];
		return new ErrorInfo(req.getRequestURL().toString(), ex.getClass().getTypeName(), ste.getClassName(), ste.getLineNumber());
	}
}
