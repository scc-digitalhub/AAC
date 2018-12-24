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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import it.smartcommunitylab.aac.dto.AACTokenIntrospection;
import it.smartcommunitylab.aac.manager.UserManager;
import it.smartcommunitylab.aac.model.ClientDetailsEntity;
import it.smartcommunitylab.aac.oauth.AutoJdbcTokenStore;
import it.smartcommunitylab.aac.repository.ClientDetailsRepository;

/**
 * OAuth2.0 Token introspection controller as of RFC7662: https://tools.ietf.org/html/rfc7662.
 * @author raman
 *
 */
@Controller
@Api(tags = { "AAC Token Introspection (ITEF RFC7662)" })
public class TokenIntrospectionController {

	private static Log logger = LogFactory.getLog(TokenIntrospectionController.class);
	@Autowired
	private ResourceServerTokenServices resourceServerTokenServices;
	@Autowired
	private ClientDetailsRepository clientDetailsRepository;
	
	@Autowired
	private AutoJdbcTokenStore tokenStore;
	@Autowired
	private UserManager userManager;	

	@Value("${openid.issuer}")
	private String issuer;

	
	@ApiOperation(value="Get token metadata")
	@RequestMapping(method = RequestMethod.POST, value = "/token_introspection")
	public ResponseEntity<AACTokenIntrospection> getTokenInfo(@RequestParam String token) {
		AACTokenIntrospection result = new AACTokenIntrospection();
		
		try {
			OAuth2Authentication auth = resourceServerTokenServices.loadAuthentication(token);
			
			OAuth2AccessToken storedToken = tokenStore.getAccessToken(auth);
			
			String clientId = auth.getOAuth2Request().getClientId();
			
			String userName = null;
			String userId = null;
			boolean applicationToken = false;
			
			if (auth.getPrincipal() instanceof User) {
				User principal = (User)auth.getPrincipal();
				userId = principal.getUsername();
			} else {
				ClientDetailsEntity client = clientDetailsRepository.findByClientId(clientId);
				applicationToken = true;
				userId = ""+client.getDeveloperId();				
			}
			userName = userManager.getUserInternalName(Long.parseLong(userId));
			String localName = userName.substring(0, userName.lastIndexOf('@'));
			String tenant = userName.substring(userName.lastIndexOf('@') + 1);

			result.setUsername(localName);
			result.setClient_id(clientId);
			result.setScope(StringUtils.collectionToDelimitedString(auth.getOAuth2Request().getScope(), " "));
			result.setExp((int)(storedToken.getExpiration().getTime() / 1000));
			result.setIat(result.getExp() - storedToken.getExpiresIn());
			result.setIss(issuer);
			result.setNbf(result.getIat());
			result.setSub(userId);
			result.setAud(clientId);
			// jti is not supported in this form
			
			// only bearer tokens supported
			result.setToken_type(OAuth2AccessToken.BEARER_TYPE);
			result.setActive(true);

			result.setAac_user_id(userId);
			result.setAac_grantType(auth.getOAuth2Request().getGrantType());
			result.setAac_applicationToken(applicationToken);
			result.setAac_am_tenant(tenant);
		} catch (Exception e) {
			logger.error("Error getting info for token: "+ e.getMessage());
			result = new AACTokenIntrospection();
			result.setActive(false);
		}
		return ResponseEntity.ok(result);
	}
	
}
