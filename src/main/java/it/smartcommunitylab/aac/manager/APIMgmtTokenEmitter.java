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

package it.smartcommunitylab.aac.manager;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.TokenRequest;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.manager.RoleManager.ROLE;
import it.smartcommunitylab.aac.model.ClientAppInfo;
import it.smartcommunitylab.aac.model.ClientDetailsEntity;
import it.smartcommunitylab.aac.repository.ClientDetailsRepository;

/**
 * @author raman
 *
 */
@Transactional
public class APIMgmtTokenEmitter {
	
	private static final String API_MGT_CLIENT_ID = "API_MGT_CLIENT_ID";
	private static final String[] GRANT_TYPES = new String []{"password","client_credentials"};
	private static final String[] API_MGT_SCOPES = new String[]{"openid","apim:subscribe","apim:api_view","apim:subscription_view","apim:api_create"};
	
	@Autowired
	@Qualifier("appTokenServices")
	AuthorizationServerTokenServices tokenService;
	@Autowired
	private UserManager userManager;
	@Autowired
	private ClientDetailsRepository clientDetailsRepository;
	
	
	public String createToken() throws Exception {
		Map<String, String> requestParameters = new HashMap<>();
		String apiManagerName = userManager.getAPIManagerName();
		if (apiManagerName == null) {
			return null;
		}
		requestParameters.put("username", apiManagerName);
		requestParameters.put("password", "");
		
		ClientDetails clientDetails = getAPIMgmtClient();
		TokenRequest tokenRequest = new TokenRequest(requestParameters, clientDetails.getClientId(), scopes(), "password");
		OAuth2Request oAuth2Request = tokenRequest.createOAuth2Request(clientDetails);
		Collection<? extends GrantedAuthority> list = authorities();
		OAuth2Authentication oAuth2Authentication = new OAuth2Authentication(oAuth2Request, new UsernamePasswordAuthenticationToken(apiManagerName, "", list));
		OAuth2AccessToken accessToken = tokenService.createAccessToken(oAuth2Authentication);
		return accessToken.getValue();
	}

	/**
	 * @return
	 */
	private Collection<? extends GrantedAuthority> authorities() {
		// TODO user authorities
		List<GrantedAuthority> list = new LinkedList<>();
		list.add(new SimpleGrantedAuthority(ROLE.user.roleName()));
		return list;
	}

	/**
	 * @return
	 */
	private Collection<String> scopes() {
		return Arrays.asList(API_MGT_SCOPES);
	}

	/**
	 * @return
	 * @throws Exception 
	 */
	private ClientDetails getAPIMgmtClient() throws Exception {
		ClientDetails client = clientDetailsRepository.findByClientId(API_MGT_CLIENT_ID);
		if (client == null) {
			ClientDetailsEntity entity = new ClientDetailsEntity();
			ClientAppInfo info = new ClientAppInfo();
			info.setName(API_MGT_CLIENT_ID);
			entity.setAdditionalInformation(info.toJson());
			entity.setClientId(API_MGT_CLIENT_ID);
			entity.setAuthorities(Config.AUTHORITY.ROLE_CLIENT_TRUSTED.name());
			entity.setAuthorizedGrantTypes(defaultGrantTypes());
			entity.setDeveloperId(0L);
			entity.setClientSecret(generateClientSecret());
			entity = clientDetailsRepository.save(entity);
			client = entity;
		}
		return client;
	}

	/**
	 * @return
	 */
	private String generateClientSecret() {
		return UUID.randomUUID().toString();
	}

	/**
	 * @return
	 */
	private String defaultGrantTypes() {
		return StringUtils.arrayToCommaDelimitedString(GRANT_TYPES);
	}

}
