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

import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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

import it.smartcommunitylab.aac.Config.ROLE_SCOPE;
import it.smartcommunitylab.aac.manager.RoleManager;
import it.smartcommunitylab.aac.manager.UserManager;
import it.smartcommunitylab.aac.model.ClientDetailsEntity;
import it.smartcommunitylab.aac.model.ErrorInfo;
import it.smartcommunitylab.aac.model.Role;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.repository.ClientDetailsRepository;
import it.smartcommunitylab.aac.repository.UserRepository;
import it.smartcommunitylab.aac.wso2.services.UserManagementService;

@Controller
public class RolesController {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RoleManager roleManager;
	@Autowired
	private UserManager userManager;
	@Autowired
	UserManagementService userManagementService;
	@Autowired
	private ResourceServerTokenServices resourceServerTokenServices;
	@Autowired
	private ClientDetailsRepository clientDetailsRepository;

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
		Role provider = developer.getRoles().stream().filter(x -> "ROLE_PROVIDER".equals(x.getRole())).findFirst()
				.get();
		String tenant = provider.getContext();

		Set<Role> fullRoles = Splitter.on(",").splitToList(roles).stream()
				.map(x -> new Role(ROLE_SCOPE.application, x, tenant)).collect(Collectors.toSet());
		user.getRoles().addAll(fullRoles);

		userRepository.save(user);
	}

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
		Role provider = developer.getRoles().stream().filter(x -> "ROLE_PROVIDER".equals(x.getRole())).findFirst()
				.get();
		String tenant = provider.getContext();

		Set<Role> fullRoles = Splitter.on(",").splitToList(roles).stream()
				.map(x -> new Role(ROLE_SCOPE.application, x, tenant)).collect(Collectors.toSet());
		user.getRoles().removeAll(fullRoles);

		userRepository.save(user);
	}

	@RequestMapping(method = RequestMethod.GET, value = "/userroles/all/user/{userId}")
	public @ResponseBody Set<Role> getAllRoles(HttpServletResponse response, @PathVariable Long userId)
			throws Exception {
		User user = userRepository.findOne(userId);
		if (user == null) {
			response.sendError(HttpStatus.NOT_ACCEPTABLE.value(), "User " + userId + " not found.");
			return null;
		}
		return user.getRoles();
	}

	@RequestMapping(method = RequestMethod.GET, value = "/userroles/user/{userId}")
	public @ResponseBody Set<Role> getRoles(HttpServletRequest request, HttpServletResponse response,
			@PathVariable Long userId) throws Exception {

		User user = userRepository.findOne(userId);
		if (user == null) {
			response.sendError(HttpStatus.NOT_ACCEPTABLE.value(), "User " + userId + " not found.");
			return null;
		}

		String parsedToken = it.smartcommunitylab.aac.common.Utils.parseHeaderToken(request);
		OAuth2Authentication auth = resourceServerTokenServices.loadAuthentication(parsedToken);
		String clientId = auth.getOAuth2Request().getClientId();
		ClientDetailsEntity client = clientDetailsRepository.findByClientId(clientId);
		Long developerId = client.getDeveloperId();

		User developer = userRepository.findOne(developerId);
		Role provider = developer.getRoles().stream().filter(x -> "ROLE_PROVIDER".equals(x.getRole())).findFirst()
				.get();
		String tenant = provider.getContext();

		Set<Role> roles = user.getRoles().stream().filter(x -> tenant.equals(x.getContext()))
				.collect(Collectors.toSet());

		return roles;
	}

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

	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	@ExceptionHandler(Exception.class)
	@ResponseBody
	ErrorInfo handleBadRequest(HttpServletRequest req, Exception ex) {
		StackTraceElement ste = ex.getStackTrace()[0];
		return new ErrorInfo(req.getRequestURL().toString(), ex.getClass().getTypeName(), ste.getClassName(), ste.getLineNumber());
	}

}
