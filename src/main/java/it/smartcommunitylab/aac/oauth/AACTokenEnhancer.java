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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final Logger logger = LoggerFactory.getLogger(getClass());
    
	private OIDCTokenEnhancer tokenEnhancer;
	private AACJwtTokenConverter tokenConverter;
	
	/**
	 * @param tokenEnhancer
	 */
	public AACTokenEnhancer(OIDCTokenEnhancer tokenEnhancer) {
	    logger.debug("create AACTokenEnhancer with OIDCTokenEnhancer");
		this.tokenEnhancer = tokenEnhancer;
		this.tokenConverter = null;
	}
	
    public AACTokenEnhancer(OIDCTokenEnhancer tokenEnhancer, AACJwtTokenConverter tokenConverter) {
        logger.debug("create AACTokenEnhancer with OIDCTokenEnhancer and AACJwtTokenConverter");
        this.tokenEnhancer = tokenEnhancer;
        this.tokenConverter = tokenConverter;
    }	

	@Override
	public OAuth2AccessToken enhance(OAuth2AccessToken accessToken, OAuth2Authentication authentication) {
	    logger.debug("enhance for token " +accessToken);
		if (accessToken.getScope().contains(Config.SCOPE_OPERATION_CONFIRMED)) {
			DefaultOAuth2AccessToken token = (DefaultOAuth2AccessToken) accessToken;
			token.setRefreshToken(null);
		}
		
		//first convert to JWT
	    if(tokenConverter != null) {
            accessToken = tokenConverter.enhance(accessToken, authentication);
        }        
		
	    //then build id_token to calculate correct at_hash
		if (accessToken.getScope().contains(Config.SCOPE_OPENID)) {
			DefaultOAuth2AccessToken token = (DefaultOAuth2AccessToken) accessToken;
			token.setAdditionalInformation(Collections.singletonMap("id_token", tokenEnhancer.createIdToken(accessToken, authentication).serialize()));
		} 
		
	
        // rewrite token type because we don't want "bearer" lowercase in response...
        if (accessToken.getTokenType().equals(OAuth2AccessToken.BEARER_TYPE.toLowerCase())) {
            DefaultOAuth2AccessToken token = (DefaultOAuth2AccessToken) accessToken;
            token.setTokenType(OAuth2AccessToken.BEARER_TYPE);
        }
	    
		return accessToken;
	}

}
