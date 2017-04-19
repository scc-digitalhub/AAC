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

import it.smartcommunitylab.aac.keymanager.model.AACTokenValidation;
import it.smartcommunitylab.aac.model.ClientDetailsEntity;
import it.smartcommunitylab.aac.oauth.AutoJdbcTokenStore;
import it.smartcommunitylab.aac.oauth.ResourceServices;
import it.smartcommunitylab.aac.repository.ClientDetailsRepository;
import it.smartcommunitylab.aac.repository.UserRepository;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Map;

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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterables;

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
	@RequestMapping("/resources/access")
	public @ResponseBody Boolean canAccessResource(@RequestHeader("Authorization") String token, @RequestParam String scope, HttpServletRequest request) {
		try {
			String parsedToken = parseHeaderToken(request);
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
	
	@RequestMapping("/resources/token")
	public @ResponseBody AACTokenValidation getTokenInfo(@RequestHeader("Authorization") String token, HttpServletRequest request) {
		AACTokenValidation response = new AACTokenValidation();
		
		try {
			String parsedToken = parseHeaderToken(request);
			
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
			
			if (auth.getPrincipal() instanceof User) {
				User principal = (User)auth.getPrincipal();
				String userId = principal.getUsername();
				it.smartcommunitylab.aac.model.User user = userRepository.findOne(Long.parseLong(userId));
				userName = user.getName();
			} else {
				ClientDetailsEntity client = clientDetailsRepository.findByClientId(clientId);
				Map parameters = mapper.readValue(client.getParameters(), Map.class);
				userName = (String)parameters.get("username");
			}
			
			response.setUser(userName);
			response.setClientId(clientId);
			response.setScope(Iterables.toArray(auth.getOAuth2Request().getScope(), String.class));
			
			long now = System.currentTimeMillis();
			response.setIssuedTime(now);
			response.setValidityPeriod(storedToken.getExpiresIn() * 1000L);

			response.setValid(true);
		} catch (Exception e) {
			logger.error("Error validating token: "+e.getMessage());
		}
		return response;
	}	
	
	private String parseHeaderToken(HttpServletRequest request) {
		@SuppressWarnings("unchecked")
		Enumeration<String> headers = request.getHeaders("Authorization");
		while (headers.hasMoreElements()) { // typically there is only one (most servers enforce that)
			String value = headers.nextElement();
			if ((value.toLowerCase().startsWith(OAuth2AccessToken.BEARER_TYPE.toLowerCase()))) {
				String authHeaderValue = value.substring(OAuth2AccessToken.BEARER_TYPE.length()).trim();
				int commaIndex = authHeaderValue.indexOf(',');
				if (commaIndex > 0) {
					authHeaderValue = authHeaderValue.substring(0, commaIndex);
				}
				return authHeaderValue;
			}
		}

		return null;
	}	

}
