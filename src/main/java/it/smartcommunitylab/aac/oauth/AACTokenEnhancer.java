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

import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.TokenEnhancer;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.openid.service.OIDCTokenEnhancer;

/**
 * Handle token result for 'operation.confirmed' scope: remove refresh token.
 * @author raman
 *
 */
public class AACTokenEnhancer implements TokenEnhancer {

	
	private OIDCTokenEnhancer tokenEnhancer;
	/**
	 * @param tokenEnhancer
	 */
	public AACTokenEnhancer(OIDCTokenEnhancer tokenEnhancer) {
		this.tokenEnhancer = tokenEnhancer;
	}

	@Override
	public OAuth2AccessToken enhance(OAuth2AccessToken accessToken, OAuth2Authentication authentication) {
		if (accessToken.getScope().contains(Config.SCOPE_OPERATION_CONFIRMED)) {
			DefaultOAuth2AccessToken token = (DefaultOAuth2AccessToken) accessToken;
			token.setRefreshToken(null);
		}
		if (accessToken.getScope().contains(Config.OPENID_SCOPE)) {
			DefaultOAuth2AccessToken token = (DefaultOAuth2AccessToken) accessToken;
			token.setAdditionalInformation(Collections.singletonMap("id_token", tokenEnhancer.createIdToken(accessToken, authentication).serialize()));
		}
		return accessToken;
	}

}
