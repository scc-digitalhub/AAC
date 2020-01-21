/*******************************************************************************
 * Copyright 2015-2019 Smart Community Lab, FBK
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

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.common.DefaultExpiringOAuth2RefreshToken;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.ExpiringOAuth2RefreshToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.security.oauth2.common.exceptions.InvalidGrantException;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.TokenRequest;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.TokenEnhancer;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import it.smartcommunitylab.aac.Config;

/**
 * @author raman
 *
 */
@Transactional
public class NonRemovingTokenServices extends DefaultTokenServices {
    private final Logger logger = LoggerFactory.getLogger(getClass());

	private ExtTokenStore localTokenStore;

	private static final Logger traceUserLogger = LoggerFactory.getLogger("traceUserToken");

	private static final int SCOPE_OPERATION_CONFIRMED_DURATION = 30;

	/** threshold for access token */
	protected int tokenThreshold = 10*60;

    private int refreshTokenValiditySeconds = 60 * 60 * 24 * 30; // default 30 days.

    private int accessTokenValiditySeconds = 60 * 60 * 12; // default 12 hours.
    
	protected TokenEnhancer tokenEnhancer;
	
	/**
	 * Do not remove access token if expired
	 */
	@Override
	public OAuth2Authentication loadAuthentication(String accessTokenValue) throws AuthenticationException {
		OAuth2AccessToken accessToken = localTokenStore.readAccessToken(accessTokenValue);
		if (accessToken == null) {
			throw new InvalidTokenException("Invalid access token: " + accessTokenValue);
		}
		else if (accessToken.isExpired()) {
			logger.error("Accessing expired token: "+accessTokenValue);
			throw new InvalidTokenException("Access token expired: " + accessTokenValue);
		}

		OAuth2Authentication result = localTokenStore.readAuthentication(accessToken);
		return result;
	}

	@Transactional(propagation=Propagation.REQUIRES_NEW, isolation=Isolation.SERIALIZABLE)
	public OAuth2AccessToken refreshAccessToken(String refreshTokenValue, TokenRequest request) throws AuthenticationException {
		return refreshWithRepeat(refreshTokenValue, request, false);
	}

	private OAuth2AccessToken refreshWithRepeat(String refreshTokenValue, TokenRequest request, boolean repeat) {
		OAuth2AccessToken accessToken = localTokenStore.readAccessTokenForRefreshToken(refreshTokenValue);

		//DISABLED check for access token, we will generate a new one if needed
		//no need to check for user approval, on removal also refresh token are invalidated
//        if (accessToken == null) {
//            throw new InvalidGrantException("Invalid refresh token: " + refreshTokenValue);
//        }

		if(accessToken != null) {
    		if ((accessToken.getExpiration().getTime() - System.currentTimeMillis()) > tokenThreshold*1000L ) {
    			return accessToken;
    		}
		}

		try {
			OAuth2AccessToken res = super.refreshAccessToken(refreshTokenValue, request);
			OAuth2Authentication auth = localTokenStore.readAuthentication(res);
			traceUserLogger.info(String.format("'type':'refresh','user':'%s','token':'%s'", auth.getName(), res.getValue()));
			return res;
		} catch (RuntimeException e) {
			// do retry: it may be the case of race condition so retry the operation but only once
			if (!repeat) return refreshWithRepeat(refreshTokenValue, request, true);
			throw e;
		}
	}

	@Transactional(isolation=Isolation.SERIALIZABLE)
	public OAuth2AccessToken createAccessToken(OAuth2Authentication authentication) throws AuthenticationException {
		logger.debug("create access token for authentication "+authentication.getName());
	    OAuth2AccessToken existingAccessToken = localTokenStore.getAccessToken(authentication);
		OAuth2RefreshToken refreshToken = null;
		if (existingAccessToken != null) {
			if (existingAccessToken.isExpired()) {
			     logger.debug("existing access token for authentication "+authentication.getName() + " is expired");       
				if (existingAccessToken.getRefreshToken() != null) {
					refreshToken = existingAccessToken.getRefreshToken();
					// The token store could remove the refresh token when the access token is removed, but we want to
					// be sure...
					localTokenStore.removeRefreshToken(refreshToken);
				}
				localTokenStore.removeAccessToken(existingAccessToken);
			} else {
                logger.debug("existing access token for authentication "+authentication.getName() + " is valid");       			    
			    //need to check if value is changed via enhancer
			    OAuth2AccessToken accessToken = tokenEnhancer != null ? tokenEnhancer.enhance(existingAccessToken, authentication) : existingAccessToken;
			    if (!existingAccessToken.getValue().equals(accessToken.getValue())) {
			        logger.debug("existing access token for authentication "+authentication.getName() + " needs to be updated");       

			        //update db via remove + store otherwise we'll get more than 1 token per key..
			        localTokenStore.removeAccessToken(existingAccessToken);
			        localTokenStore.storeAccessToken(accessToken, authentication);
			    }
			    return accessToken; 
			}
		}

		// Only create a new refresh token if there wasn't an existing one associated with an expired access token.
		// Clients might be holding existing refresh tokens, so we re-use it in the case that the old access token
		// expired.
		if (refreshToken == null) {
			refreshToken = createRefreshToken(authentication);
		}
		// But the refresh token itself might need to be re-issued if it has expired.
		else if (refreshToken instanceof ExpiringOAuth2RefreshToken) {
			ExpiringOAuth2RefreshToken expiring = (ExpiringOAuth2RefreshToken) refreshToken;
			if (isExpired(expiring)) {
				refreshToken = createRefreshToken(authentication);
			}
		}
        logger.debug("create access token for authentication "+authentication.getName() + " as new");     
		OAuth2AccessToken accessToken = createAccessToken(authentication, refreshToken);
		localTokenStore.storeAccessToken(accessToken, authentication);
		if (refreshToken != null) {
			localTokenStore.storeRefreshToken(refreshToken, authentication);
		}
		traceUserLogger.info(String.format("'type':'new','user':'%s','scope':'%s','token':'%s'", authentication.getName(), String.join(" ", accessToken.getScope()), accessToken.getValue()));
		return accessToken;
	}
	
    private ExpiringOAuth2RefreshToken createRefreshToken(OAuth2Authentication authentication) {
        if (!isSupportRefreshToken(authentication.getOAuth2Request())) {
            return null;
        }
        int validitySeconds = getRefreshTokenValiditySeconds(authentication.getOAuth2Request());
        // use a secure string as value to respect requirement
        // https://tools.ietf.org/html/rfc6749#section-10.10
        // 160bit = a buffer of 20 random bytes
        String value = generateSecureString(20);
        ExpiringOAuth2RefreshToken refreshToken = new DefaultExpiringOAuth2RefreshToken(value,
                new Date(System.currentTimeMillis() + (validitySeconds * 1000L)));
        return refreshToken;
    }

	private OAuth2AccessToken createAccessToken(OAuth2Authentication authentication, OAuth2RefreshToken refreshToken) {
        // use a secure string as value to respect requirement
        // https://tools.ietf.org/html/rfc6749#section-10.10
        // 160bit = a buffer of 20 random bytes
        String value = generateSecureString(20);
		DefaultOAuth2AccessToken token = new DefaultOAuth2AccessToken(value);
		int validitySeconds = getAccessTokenValiditySeconds(authentication.getOAuth2Request());
		
		//custom validity for client_credentials grants
		if("client_credentials".equals(authentication.getOAuth2Request().getGrantType())) {
		    validitySeconds = getRefreshTokenValiditySeconds(authentication.getOAuth2Request());
		}
		
		if (!authentication.isClientOnly()) {			
			token.setExpiration(new Date(System.currentTimeMillis() + (getUserAccessTokenValiditySeconds(authentication.getOAuth2Request()) * 1000L)));
		} else if (validitySeconds > 0) {
			token.setExpiration(new Date(System.currentTimeMillis() + (validitySeconds * 1000L)));
		} else {
			token.setExpiration(new Date(Long.MAX_VALUE));
		}
		
		token.setRefreshToken(refreshToken);
		token.setScope(authentication.getOAuth2Request().getScope());

		logger.info("Created token " + token.getValue() + " expires at " + token.getExpiration());
		return tokenEnhancer != null ? tokenEnhancer.enhance(token, authentication) : token;
	}

	@Override
	public void setTokenEnhancer(TokenEnhancer accessTokenEnhancer) {
		super.setTokenEnhancer(accessTokenEnhancer);
		this.tokenEnhancer = accessTokenEnhancer;
	}

	@Override
	public void setTokenStore(TokenStore tokenStore) {
		super.setTokenStore(tokenStore);
		assert tokenStore instanceof ExtTokenStore;
		this.localTokenStore = (ExtTokenStore)tokenStore;
	}

	/**
	 * @param tokenThreshold the tokenThreshold to set
	 */
	public void setTokenThreshold(int tokenThreshold) {
		this.tokenThreshold = tokenThreshold;
	}
	
    /**
     * The validity (in seconds) of the refresh token. If less than or equal to zero
     * then the tokens will be non-expiring.
     * 
     * @param refreshTokenValiditySeconds The validity (in seconds) of the refresh
     *                                    token.
     */
    public void setRefreshTokenValiditySeconds(int refreshTokenValiditySeconds) {
        super.setRefreshTokenValiditySeconds(refreshTokenValiditySeconds);
        this.refreshTokenValiditySeconds = refreshTokenValiditySeconds;
    }
    
    /**
     * The default validity (in seconds) of the access token. Zero or negative for
     * non-expiring tokens. If a client details service is set the validity period
     * will be read from he client, defaulting to this value if not defined by the
     * client.
     * 
     * @param accessTokenValiditySeconds The validity (in seconds) of the access
     *                                   token.
     */
    public void setAccessTokenValiditySeconds(int accessTokenValiditySeconds) {
        super.setAccessTokenValiditySeconds(accessTokenValiditySeconds);
        this.accessTokenValiditySeconds = accessTokenValiditySeconds;
    }

	@Override
	protected boolean isExpired(OAuth2RefreshToken refreshToken) {
		return false;
	}

	protected int getUserAccessTokenValiditySeconds(OAuth2Request clientAuth) {
		if (clientAuth.getScope().contains(Config.SCOPE_OPERATION_CONFIRMED)) return SCOPE_OPERATION_CONFIRMED_DURATION;
		return accessTokenValiditySeconds;
	}
	
    private String generateSecureString(int length) {
        SecureRandom random = new SecureRandom();
        byte[] accessTokenBuffer = new byte[length];
        random.nextBytes(accessTokenBuffer);
        return new String(Base64.getEncoder().encode(accessTokenBuffer));
    }
	
	
}
