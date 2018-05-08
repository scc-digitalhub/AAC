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

package it.smartcommunitylab.aac.oauth;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.util.StringUtils;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.common.exceptions.InvalidGrantException;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2RequestFactory;
import org.springframework.security.oauth2.provider.TokenRequest;
import org.springframework.security.oauth2.provider.token.AbstractTokenGranter;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;

import it.smartcommunitylab.aac.manager.ProviderServiceAdapter;
import it.smartcommunitylab.aac.model.User;

/**
 * @author raman
 *
 */
public class NativeTokenGranter extends AbstractTokenGranter {

	private ProviderServiceAdapter providerService;

	public NativeTokenGranter(ProviderServiceAdapter providerService, AuthorizationServerTokenServices tokenServices, ClientDetailsService clientDetailsService,
			OAuth2RequestFactory requestFactory, String grantType) {
		super(tokenServices, clientDetailsService, requestFactory, grantType);
		this.providerService = providerService;
	}

	protected OAuth2Authentication getOAuth2Authentication(ClientDetails client, TokenRequest tokenRequest) {
		Map<String, String> params = tokenRequest.getRequestParameters();
		String authority = params.get("authority");
		String token = params.get("token");
		if (StringUtils.isEmpty(authority) || StringUtils.isEmpty(token)) {
			throw new InvalidGrantException("Missing token or authority parameters");
		}
				
		User nativeUser = providerService.updateNativeUser(authority, token, params);
		
		List<GrantedAuthority> list = Collections.<GrantedAuthority> singletonList(new SimpleGrantedAuthority("ROLE_USER"));

//		Authentication user = new UsernamePasswordAuthenticationToken(nativeUser.getId().toString(), null, list);
//		
		tokenRequest.setScope(providerService.userScopes(nativeUser, tokenRequest.getScope(), true));
		
		UserDetails user = new org.springframework.security.core.userdetails.User(nativeUser.getId().toString(), "", list);
		AbstractAuthenticationToken a = new AACAuthenticationToken(user, null, authority, list);
		a.setDetails(authority);
		SecurityContextHolder.getContext().setAuthentication(a);
		
		OAuth2Authentication authentication = new OAuth2Authentication(tokenRequest.createOAuth2Request(client), a);
		return authentication;
	}
}
