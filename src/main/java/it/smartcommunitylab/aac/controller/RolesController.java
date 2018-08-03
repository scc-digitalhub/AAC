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

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.google.common.base.Splitter;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.manager.UserManager;
import it.smartcommunitylab.aac.model.ClientDetailsEntity;
import it.smartcommunitylab.aac.model.ErrorInfo;
import it.smartcommunitylab.aac.model.Response;
import it.smartcommunitylab.aac.model.Role;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.repository.ClientDetailsRepository;
import it.smartcommunitylab.aac.repository.UserRepository;

@Controller
@Api(tags = {"AAC Roles"})
public class RolesController {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private UserManager userManager;
	@Autowired
	private ResourceServerTokenServices resourceServerTokenServices;
	@Autowired
	private ClientDetailsRepository clientDetailsRepository;

	@ApiOperation(value="Get roles of a current user")
	@RequestMapping(method = RequestMethod.GET, value = "/userroles/me")
	public @ResponseBody Set<Role> getRoles(HttpServletResponse response) throws Exception {
		Long userId = userManager.getUserId();
		if (userId == null) {
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			return null;
		}
		User user = userRepository.findOne(userId);

		return user.getRoles();
	}

	@ApiOperation(value="Add roles to a specific user")
	@RequestMapping(method = RequestMethod.PUT, value = "/userroles/user/{userId}")
	public @ResponseBody void addRoles(HttpServletRequest request, HttpServletResponse response,
			@PathVariable Long userId, @RequestParam String roles) throws Exception {
		User user = userRepository.findOne(userId);
		if (user == null) {
			response.sendError(HttpStatus.NOT_ACCEPTABLE.value(), "User " + userId + " not found.");
			return;
		}

		String parsedToken = it.smartcommunitylab.aac.common.Utils.parseHeaderToken(request);
		OAuth2Authentication auth = resourceServerTokenServices.loadAuthentication(parsedToken);
		String clientId = auth.getOAuth2Request().getClientId();
		ClientDetailsEntity client = clientDetailsRepository.findByClientId(clientId);
		Long developerId = client.getDeveloperId();

		User developer = userRepository.findOne(developerId);
		Set<Role> fullRoles = parseAndCheckRoles(roles);
		// role should be in the same space or in the same context if it is ROLE_PROVIDER
		Set<String> acceptedDomains = developer.contextRole(Config.R_PROVIDER).stream().map(Role::canonicalSpace).collect(Collectors.toSet());
		if (fullRoles.stream()
				.anyMatch(role -> !acceptedDomains.contains(role.canonicalSpace())  && !(acceptedDomains.contains(role.getContext()) && Config.R_PROVIDER.equals(role.getRole())))) {
			throw new IllegalArgumentException("Can add roles to the owned space or create new child space owners");
		}
		user.getRoles().addAll(fullRoles);
		userRepository.save(user);
	}

	protected Set<Role> parseAndCheckRoles(String roles) {
		Set<Role> fullRoles = new HashSet<>();
		List<String> input = Splitter.on(",").splitToList(roles);
		for (String roleString : input) {
			Role role = Role.parse(roleString);
			fullRoles.add(role);
		}
		if (fullRoles.isEmpty()) throw new IllegalArgumentException("Invalid input roles");
		return fullRoles;
	}

	@ApiOperation(value="Delete roles for a specific user")
	@RequestMapping(method = RequestMethod.DELETE, value = "/userroles/user/{userId}")
	public @ResponseBody void deleteRoles(HttpServletRequest request, HttpServletResponse response,
			@PathVariable Long userId, @RequestParam String roles) throws Exception {
		User user = userRepository.findOne(userId);
		if (user == null) {
			response.sendError(HttpStatus.NOT_ACCEPTABLE.value(), "User " + userId + " not found.");
			return;
		}

		String parsedToken = it.smartcommunitylab.aac.common.Utils.parseHeaderToken(request);
		OAuth2Authentication auth = resourceServerTokenServices.loadAuthentication(parsedToken);
		String clientId = auth.getOAuth2Request().getClientId();
		ClientDetailsEntity client = clientDetailsRepository.findByClientId(clientId);
		Long developerId = client.getDeveloperId();

		User developer = userRepository.findOne(developerId);
		Set<Role> fullRoles = parseAndCheckRoles(roles);
		// cannot remove ROLE_PROVIDER of the same user
		Set<String> acceptedDomains = developer.contextRole(Config.R_PROVIDER).stream().map(Role::canonicalSpace).collect(Collectors.toSet());
		if (developerId == userId && fullRoles.stream()
				.anyMatch(role -> Config.R_PROVIDER.equals(role.getRole()))) {
			throw new IllegalArgumentException("Cannot remove space ownership for the same user");
		}
		// can remove roles in the same space or ROLE_PROVIDERs of subspaces
		if (fullRoles.stream()
				.anyMatch(role -> !acceptedDomains.contains(role.canonicalSpace())  && !(acceptedDomains.contains(role.getContext()) && Config.R_PROVIDER.equals(role.getRole())))) {
			throw new IllegalArgumentException("Can delete roles only within owned spaces");
		}

		user.getRoles().removeAll(fullRoles);

		userRepository.save(user);
	}

	private Set<Role> getUserRoles(HttpServletRequest request, HttpServletResponse response,
			Long userId) throws IOException {
		User user = userRepository.findOne(userId);
		if (user == null) {
			response.sendError(HttpStatus.NOT_ACCEPTABLE.value(), "User " + userId + " not found.");
			return null;
		}

		String parsedToken = it.smartcommunitylab.aac.common.Utils.parseHeaderToken(request);
		OAuth2Authentication auth = resourceServerTokenServices.loadAuthentication(parsedToken);
		String clientId = auth.getOAuth2Request().getClientId();
		return userManager.getUserRolesByClient(user, clientId);
	}

	@ApiOperation(value="Get roles of a specific user in a domain")
	@RequestMapping(method = RequestMethod.GET, value = "/userroles/user/{userId}")
	public @ResponseBody Set<Role> getRolesByUserId(HttpServletRequest request, HttpServletResponse response,
			@PathVariable Long userId) throws Exception {
		return getUserRoles(request, response, userId);
	}
	
	@ApiOperation(value="Get roles of a client token owner")
	@RequestMapping(method = RequestMethod.GET, value = "/userroles/token/{token}")
	public @ResponseBody Set<Role> getRolesByToken(
			@PathVariable String token,
			HttpServletRequest request, 
			HttpServletResponse response) throws Exception {
		OAuth2Authentication auth = resourceServerTokenServices.loadAuthentication(token);
		String clientId = auth.getOAuth2Request().getClientId();
		ClientDetailsEntity client = clientDetailsRepository.findByClientId(clientId);
		// find the user - owner of the client app represented by the token
		Long developerId = client.getDeveloperId();
		return getUserRoles(request, response, developerId);
	}	

	@ApiOperation(value="Get roles of a client owner")
	@RequestMapping(method = RequestMethod.GET, value = "/userroles/client/{clientId}")
	public @ResponseBody Set<Role> getRolesByClientId(
			@PathVariable String clientId,
			HttpServletRequest request, 
			HttpServletResponse response) throws Exception {
		ClientDetailsEntity client = clientDetailsRepository.findByClientId(clientId);
		Long developerId = client.getDeveloperId();
		return getUserRoles(request, response, developerId);
	}
	
	@ApiOperation(value="Get roles of a client owner by token")
	@RequestMapping(method = RequestMethod.GET, value = "/userroles/client")
	public @ResponseBody Set<Role> getClientRoles(HttpServletRequest request, HttpServletResponse response)
			throws Exception {
			String parsedToken = it.smartcommunitylab.aac.common.Utils.parseHeaderToken(request);
			OAuth2Authentication auth = resourceServerTokenServices.loadAuthentication(parsedToken);
			String clientId = auth.getOAuth2Request().getClientId();
			ClientDetailsEntity client = clientDetailsRepository.findByClientId(clientId);
			Long developerId = client.getDeveloperId();

			User developer = userRepository.findOne(developerId);

			return developer.getRoles();
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
