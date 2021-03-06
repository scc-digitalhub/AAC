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

package it.smartcommunitylab.aac.roles;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.EntityNotFoundException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.common.SecurityUtils;
import it.smartcommunitylab.aac.dto.UserDTO;
import it.smartcommunitylab.aac.manager.RoleManager;
import it.smartcommunitylab.aac.manager.UserManager;
import it.smartcommunitylab.aac.model.ClientDetailsEntity;
import it.smartcommunitylab.aac.model.ErrorInfo;
import it.smartcommunitylab.aac.model.Response;
import it.smartcommunitylab.aac.model.Role;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.repository.ClientDetailsRepository;

@Controller
@Api(tags = {"AAC Roles"})
public class RolesController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

//	@Autowired
//	private UserRepository userRepository;

	@Autowired
	private UserManager userManager;
	
	@Autowired
	private RoleManager roleManager;
	
	//TODO remove, used only in deprecated method
	@Autowired
	private ResourceServerTokenServices resourceServerTokenServices;
	
	//TODO remove, call manager
	@Autowired
	private ClientDetailsRepository clientDetailsRepository;

    @ApiOperation(value = "Get roles of the current user")
    @RequestMapping(method = RequestMethod.GET, value = "/userroles/me")
    public @ResponseBody Set<Role> getRoles(Authentication auth,
            HttpServletResponse response) throws Exception {
    	
        String clientId = SecurityUtils.getOAuthClientId(auth);
        String userId = SecurityUtils.getOAuthUserId(auth);
        
        logger.debug("get roles for user " + userId + " from client " + clientId);        
        
        // return all the user roles
        return roleManager.getRoles(Long.parseLong(userId));
    }

    @ApiOperation(value = "Add roles to a specific user")
    @RequestMapping(method = RequestMethod.PUT, value = "/userroles/user/{userId}")
    public @ResponseBody void addRoles(@PathVariable Long userId, @RequestParam String roles,
            Authentication auth,
            HttpServletRequest request, HttpServletResponse response) throws Exception {

        //will trigger exception if user does not exists
        User user = userManager.getOne(userId);
        String clientId = SecurityUtils.getOAuthClientId(auth);
        
        Collection<String> scopes = SecurityUtils.getOAuthScopes(auth);
        boolean asRoleManager = scopes.contains(Config.SCOPE_ROLEMANAGEMENT);
        
        roleManager.addRoles(user.getId(), clientId, roles, asRoleManager);
    }


    @ApiOperation(value = "Delete roles for a specific user")
    @RequestMapping(method = RequestMethod.DELETE, value = "/userroles/user/{userId}")
    public @ResponseBody void deleteRoles(@PathVariable Long userId, @RequestParam String roles,
            Authentication auth,
            HttpServletRequest request, HttpServletResponse response) throws Exception {

        //will trigger exception if user does not exists
        User user = userManager.getOne(userId);
        String clientId = SecurityUtils.getOAuthClientId(auth);

        Collection<String> scopes = SecurityUtils.getOAuthScopes(auth);
        boolean asRoleManager = scopes.contains(Config.SCOPE_ROLEMANAGEMENT);
        
        roleManager.deleteRoles(user.getId(), clientId, roles, asRoleManager);
    }

    @ApiOperation(value = "Get roles of a specific user in a domain")
    @RequestMapping(method = RequestMethod.GET, value = "/userroles/user/{userId}")
    public @ResponseBody Set<Role> getRolesByUserId(
            @PathVariable Long userId,
            Authentication auth,
            HttpServletRequest request, HttpServletResponse response) throws Exception {

        //will trigger exception if user does not exists
        User user = userManager.getOne(userId);
        String clientId = SecurityUtils.getOAuthClientId(auth);
        
        Collection<String> scopes = SecurityUtils.getOAuthScopes(auth);
        boolean asRoleManager = scopes.contains(Config.SCOPE_ROLEMANAGEMENT);

        return userManager.getUserRolesByClient(user, clientId, asRoleManager);
    }

    @Deprecated
    @ApiOperation(value = "Get roles of a client token owner")
    @RequestMapping(method = RequestMethod.GET, value = "/userroles/token/{token}")
    public @ResponseBody Set<Role> getRolesByToken(@PathVariable String token,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        OAuth2Authentication auth = resourceServerTokenServices.loadAuthentication(token);
        String clientId = SecurityUtils.getOAuthClientId(auth);
        ClientDetailsEntity client = clientDetailsRepository.findByClientId(clientId);
        
        // find the user - owner of the client app represented by the token
        Long developerId = client.getDeveloperId();
        //will trigger exception if user does not exists
        User user = userManager.getOne(developerId);
        
        Collection<String> scopes = SecurityUtils.getOAuthScopes(auth);
        boolean asRoleManager = scopes.contains(Config.SCOPE_ROLEMANAGEMENT);

        return userManager.getUserRolesByClient(user, clientId, asRoleManager);
        
    }

    @ApiOperation(value = "Get roles of a client owner")
    @RequestMapping(method = RequestMethod.GET, value = "/userroles/client/{clientId}")
    public @ResponseBody Set<Role> getRolesByClientId(@PathVariable String clientId,
            Authentication auth,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
    	
        ClientDetailsEntity client = clientDetailsRepository.findByClientId(clientId);        
        Long developerId = client.getDeveloperId();
        
        //will trigger exception if user does not exists
        User user = userManager.getOne(developerId);
        
        Collection<String> scopes = SecurityUtils.getOAuthScopes(auth);
        boolean asRoleManager = scopes.contains(Config.SCOPE_ROLEMANAGEMENT);

        return userManager.getUserRolesByClient(user, clientId, asRoleManager);
    }
	
    @ApiOperation(value = "Get roles of a client owner by token")
    @RequestMapping(method = RequestMethod.GET, value = "/userroles/client")
    public @ResponseBody Set<Role> getClientRoles(Authentication auth,
            HttpServletRequest request, HttpServletResponse response) throws Exception {

        String clientId = SecurityUtils.getOAuthClientId(auth);
        ClientDetailsEntity client = clientDetailsRepository.findByClientId(clientId);
        Long developerId = client.getDeveloperId();

        //will trigger exception if user does not exists
        User developer = userManager.getOne(developerId);
        
        return developer.getRoles();
    }

	@ApiOperation(value="Get users in a role space with specific role")
	@GetMapping("/userroles/role")
	public @ResponseBody List<UserDTO> spaceUsers(
			@RequestParam String context,
			@RequestParam(required=false, defaultValue="false") Boolean nested,
			@RequestParam(required=false) String role,
			@RequestParam(required=false, defaultValue="0") Integer offset, 
			@RequestParam(required=false, defaultValue="25") Integer limit,
			Authentication auth) {
		offset = offset / limit;
		// if nested, search by context/space matching input context union context prefix match input context
		// if not nested, search context/space matching input context
		Role r = Role.ownerOf(context);
		String extContext =  context + "/";
		List<User> users = null;
		List<UserDTO> dtos = null;
		if (nested) {
			users = roleManager.findUsersByContextNested(r.getContext(), r.getSpace(), role, offset, limit);
			dtos = users.stream().map(u -> {
				UserDTO dto = UserDTO.fromUser(u);
				dto.setRoles(u.getRoles().stream().filter(ur -> {
					String canonical = ur.canonicalSpace();
					return canonical.equals(context) || canonical.startsWith(extContext);
				}).collect(Collectors.toSet()));
				return dto;
			}).collect(Collectors.toList());
		} else {
			users = roleManager.findUsersByContextAndRole(r.getContext(), r.getSpace(), role, offset, limit);
			dtos = users.stream().map(u -> {
				UserDTO dto = UserDTO.fromUser(u);
				dto.setRoles(u.getRoles().stream().filter(ur -> ur.canonicalSpace().equals(context)).collect(Collectors.toSet()));
				return dto;
			}).collect(Collectors.toList());
		}
		return dtos;
	}
	
	   
    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
    @ResponseBody
    Response processNotFoundError(EntityNotFoundException ex) {
        return Response.error(ex.getMessage());
    }
    
	@ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ResponseBody
    Response processAccessError(AccessDeniedException ex) {
		return Response.error(ex.getMessage());
    }
	
	@ExceptionHandler(InsufficientAuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ResponseBody
    Response processAuthError(InsufficientAuthenticationException ex) {
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
	
	
//   private Set<Role> getUserRoles(HttpServletRequest request, HttpServletResponse response, Long userId) throws IOException {
//        User user = userManager.findOne(userId);
//        if (user == null) {
//            response.sendError(HttpStatus.NOT_ACCEPTABLE.value(), "User " + userId + " not found.");
//            return null;
//        }
//
//        String parsedToken = it.smartcommunitylab.aac.common.Utils.parseHeaderToken(request);
//        OAuth2Authentication auth = resourceServerTokenServices.loadAuthentication(parsedToken);
//        String clientId = auth.getOAuth2Request().getClientId();
//        return userManager.getUserRolesByClient(user, clientId, auth.getOAuth2Request().getScope().contains(Config.SCOPE_ROLEMANAGEMENT));
//    }

    
}
