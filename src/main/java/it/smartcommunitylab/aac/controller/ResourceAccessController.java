/**
 *    Copyright 2012-2013 Trento RISE
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
 */

package it.smartcommunitylab.aac.controller;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterables;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.Config.ROLE_SCOPE;
import it.smartcommunitylab.aac.keymanager.model.AACTokenValidation;
import it.smartcommunitylab.aac.model.ClientDetailsEntity;
import it.smartcommunitylab.aac.model.Role;
import it.smartcommunitylab.aac.oauth.AutoJdbcTokenStore;
import it.smartcommunitylab.aac.oauth.ResourceServices;
import it.smartcommunitylab.aac.repository.ClientDetailsRepository;
import it.smartcommunitylab.aac.repository.UserRepository;
import it.smartcommunitylab.aac.wso2.services.Utils;

/**
 * Controller for remote check the access to the resource
 * 
 * @author raman
 *
 */
@Controller
public class ResourceAccessController {

	private static Log logger = LogFactory.getLog(ResourceAccessController.class);
	@Autowired
	private ResourceServices resourceServices;
	@Autowired
	private ResourceServerTokenServices resourceServerTokenServices;
	@Autowired
	private ClientDetailsRepository clientDetailsRepository;
	
	@Autowired
	private AutoJdbcTokenStore tokenStore;
	@Autowired
	private UserRepository userRepository;	
	
	/**
	 * Check the access to the specified resource using the client app token header
	 * @param token
	 * @param resourceUri
	 * @param request
	 * @return
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/resources/access")
	public @ResponseBody Boolean canAccessResource(@RequestParam String scope, HttpServletRequest request) {
		try {
			String parsedToken = it.smartcommunitylab.aac.common.Utils.parseHeaderToken(request);
			OAuth2Authentication auth = resourceServerTokenServices.loadAuthentication(parsedToken);
			Collection<String> actualScope = auth.getOAuth2Request().getScope();
			Collection<String> scopeSet = StringUtils.commaDelimitedListToSet(scope);
			if (actualScope != null && !actualScope.isEmpty() && actualScope.containsAll(scopeSet)) {
				return true;
			}
		} catch (AuthenticationException e) {
			logger.error("Error validating token: "+e.getMessage());
		}
		return false;
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/resources/token")
	public @ResponseBody AACTokenValidation getTokenInfo(HttpServletRequest request) {
		AACTokenValidation response = new AACTokenValidation();
		
		try {
			String parsedToken = it.smartcommunitylab.aac.common.Utils.parseHeaderToken(request);
			
			OAuth2Authentication auth = resourceServerTokenServices.loadAuthentication(parsedToken);
			
			OAuth2AccessToken storedToken = tokenStore.getAccessToken(auth);
			
			String clientId = auth.getOAuth2Request().getClientId();
			
			ObjectMapper mapper = new ObjectMapper();
			mapper.setVisibility(mapper.getSerializationConfig().getDefaultVisibilityChecker()
	                .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
	                .withGetterVisibility(JsonAutoDetect.Visibility.ANY)
	                .withSetterVisibility(JsonAutoDetect.Visibility.ANY)
	                .withCreatorVisibility(JsonAutoDetect.Visibility.ANY));
			
			String userName = null;
			String userId = null;
			
			System.err.println(auth.getPrincipal());
			
			if (auth.getPrincipal() instanceof User) {
				User principal = (User)auth.getPrincipal();
				userId = principal.getUsername();
				it.smartcommunitylab.aac.model.User user = userRepository.findOne(Long.parseLong(userId));
				userName = getWSO2Name(user);
//			} if (auth.getPrincipal() instanceof it.smartcommunitylab.aac.model.User) { 
//				it.smartcommunitylab.aac.model.User principal = (it.smartcommunitylab.aac.model.User)auth.getPrincipal();
//				userId = principal.getId().toString();
//				userName = getWSO2Name(user);
			} else {
				ClientDetailsEntity client = clientDetailsRepository.findByClientId(clientId);
				if (client.getParameters() != null) {
					Map parameters = mapper.readValue(client.getParameters(), Map.class);
					userName = (String)parameters.get("username");
				} else {
//					it.smartcommunitylab.aac.model.User user = userRepository.findOne(Long.parseLong(userId));
					userName = "admin";
					userName = (String)auth.getPrincipal();
				}
			}
			
			response.setUsername(userName);
			response.setUserId(userId);
			response.setClientId(clientId);
			response.setScope(Iterables.toArray(auth.getOAuth2Request().getScope(), String.class));
			
			long now = System.currentTimeMillis();
			response.setIssuedTime(now);
			response.setValidityPeriod(storedToken.getExpiresIn() * 1000L);

			response.setValid(true);
			
			response.setApplicationToken(response.getUserId() == null);
			
//			System.err.println(mapper.writeValueAsString(response));			
		} catch (Exception e) {
			logger.error("Error validating token: "+e.getMessage());
		}
		
		return response;
	}	
	
	/**
	 * @param user
	 * @return
	 */
	private String getWSO2Name(it.smartcommunitylab.aac.model.User user) {
		Set<Role> providerRoles = user.role(ROLE_SCOPE.tenant, "ROLE_PROVIDER");

		String email = user.attributeValue(Config.IDP_INTERNAL, "email");
		if (email == null) return null;
		
		String domain = null;
		if (providerRoles.isEmpty()) domain = "carbon.super";
		else {
			Role role = providerRoles.iterator().next();
			domain = role.getContext();
		}
		
		return Utils.getUserNameAtTenant(email, domain);
	}	
	


}
