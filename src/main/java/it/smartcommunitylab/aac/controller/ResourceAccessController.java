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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import it.smartcommunitylab.aac.dto.AACTokenValidation;
import it.smartcommunitylab.aac.manager.UserManager;
import it.smartcommunitylab.aac.model.ClientDetailsEntity;
import it.smartcommunitylab.aac.oauth.AutoJdbcTokenStore;
import it.smartcommunitylab.aac.repository.ClientDetailsRepository;

/**
 * Controller for remote check the access to the resource
 * 
 * @author raman
 *
 */
@Controller
@Api(tags = { "AAC Token Info" })
public class ResourceAccessController {

	private static Log logger = LogFactory.getLog(ResourceAccessController.class);
	@Autowired
	private ResourceServerTokenServices resourceServerTokenServices;
	@Autowired
	private ClientDetailsRepository clientDetailsRepository;
	
	@Autowired
	private AutoJdbcTokenStore tokenStore;
	@Autowired
	private UserManager userManager;	
	
	/**
	 * Check the access to the specified resource using the client app token header
	 * @param token
	 * @param resourceUri
	 * @param request
	 * @return
	 */
	@ApiOperation(value="Check scope access for token")
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
	
	@SuppressWarnings("unchecked")
	@ApiOperation(value="Get token info")
	@RequestMapping(method = RequestMethod.GET, value = "/resources/token")
	public @ResponseBody AACTokenValidation getTokenInfo(HttpServletRequest request, HttpServletResponse response) {
		AACTokenValidation result = new AACTokenValidation();
		
		try {
			String parsedToken = it.smartcommunitylab.aac.common.Utils.parseHeaderToken(request);
			
			OAuth2Authentication auth = resourceServerTokenServices.loadAuthentication(parsedToken);
			
			OAuth2AccessToken storedToken = tokenStore.getAccessToken(auth);
			long expiresIn = storedToken.getExpiresIn();
			
			String clientId = auth.getOAuth2Request().getClientId();
			
			ObjectMapper mapper = new ObjectMapper();
			mapper.setVisibility(mapper.getSerializationConfig().getDefaultVisibilityChecker()
	                .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
	                .withGetterVisibility(JsonAutoDetect.Visibility.ANY)
	                .withSetterVisibility(JsonAutoDetect.Visibility.ANY)
	                .withCreatorVisibility(JsonAutoDetect.Visibility.ANY));
			
			String userName = null;
			String userId = null;
			boolean applicationToken = false;
			
//			System.err.println(auth.getPrincipal());
			
			if (auth.getPrincipal() instanceof User) {
				User principal = (User)auth.getPrincipal();
				userId = principal.getUsername();
//			} if (auth.getPrincipal() instanceof it.smartcommunitylab.aac.model.User) { 
//				it.smartcommunitylab.aac.model.User principal = (it.smartcommunitylab.aac.model.User)auth.getPrincipal();
//				userId = principal.getId().toString();
//				userName = getWSO2Name(user);
			} else {
				ClientDetailsEntity client = clientDetailsRepository.findByClientId(clientId);
				applicationToken = true;
				userId = ""+client.getDeveloperId();				
//				if (client.getParameters() != null) {
//					Map<String,?> parameters = mapper.readValue(client.getParameters(), Map.class);
//					userName = (String)parameters.get("username");
//				} else {
////					it.smartcommunitylab.aac.model.User user = userRepository.findOne(Long.parseLong(userId));
//					userName = "admin";
//					userName = (String)auth.getPrincipal();
//				}
			}
			userName = userManager.getUserInternalName(Long.parseLong(userId));

			result.setUsername(userName);
			result.setUserId(userId);
			result.setClientId(clientId);
			result.setScope(Iterables.toArray(auth.getOAuth2Request().getScope(), String.class));
			result.setGrantType(auth.getOAuth2Request().getGrantType());
			
			long now = System.currentTimeMillis();
			result.setIssuedTime(now);
			result.setValidityPeriod(expiresIn);
			
			logger.info("Requested token " + parsedToken + " expires in " + result.getValidityPeriod());

			result.setValid(true);
			
			result.setApplicationToken(applicationToken);
			
//			System.err.println(mapper.writeValueAsString(response));			
		} catch (InvalidTokenException e) {
			logger.error("Invalid token: "+ e.getMessage());
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			return null;
		} catch (Exception e) {
			logger.error("Error getting info for token: "+ e.getMessage());
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return null;
		}
		
		return result;
	}	

	


}
