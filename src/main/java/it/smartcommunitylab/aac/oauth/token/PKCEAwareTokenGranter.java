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

package it.smartcommunitylab.aac.oauth.token;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidClientException;
import org.springframework.security.oauth2.common.exceptions.InvalidGrantException;
import org.springframework.security.oauth2.common.exceptions.InvalidRequestException;
import org.springframework.security.oauth2.common.exceptions.RedirectMismatchException;
import org.springframework.security.oauth2.common.util.OAuth2Utils;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.OAuth2RequestFactory;
import org.springframework.security.oauth2.provider.TokenRequest;
import org.springframework.security.oauth2.provider.code.AuthorizationCodeServices;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;

import it.smartcommunitylab.aac.oauth.AACOAuth2AccessToken;
import it.smartcommunitylab.aac.oauth.AACOAuth2Utils;

/**
 * @author raman
 *
 */
public class PKCEAwareTokenGranter extends AbstractTokenGranter {
    
    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    private static final String GRANT_TYPE = "authorization_code";

    private final AuthorizationCodeServices authorizationCodeServices;
    
    private boolean allowRefresh = false;

    public PKCEAwareTokenGranter(AuthorizationServerTokenServices tokenServices,
            AuthorizationCodeServices authorizationCodeServices, ClientDetailsService clientDetailsService,
            OAuth2RequestFactory requestFactory) {
        this(tokenServices, authorizationCodeServices, clientDetailsService, requestFactory, GRANT_TYPE);
    }

    protected PKCEAwareTokenGranter(AuthorizationServerTokenServices tokenServices,
            AuthorizationCodeServices authorizationCodeServices,
            ClientDetailsService clientDetailsService, OAuth2RequestFactory requestFactory, String grantType) {
        super(tokenServices, clientDetailsService, requestFactory, grantType);
        this.authorizationCodeServices = authorizationCodeServices;
    }	
	
    public void setAllowRefresh(boolean allowRefresh) {
        this.allowRefresh = allowRefresh;
    }

    @Override
    public OAuth2AccessToken grant(String grantType, TokenRequest tokenRequest) {
        // check if PKCE otherwise let another granter handle the request
        String codeVerifier = tokenRequest.getRequestParameters().get(AACOAuth2Utils.CODE_VERIFIER);
        if (codeVerifier == null) {
            return null;
        }

        OAuth2AccessToken token = super.grant(grantType, tokenRequest);

        if (token != null) {
            logger.trace("grant access token for client " + tokenRequest.getClientId() + " request "
                    + tokenRequest.getRequestParameters().toString());

            if (!allowRefresh) {
                AACOAuth2AccessToken norefresh = new AACOAuth2AccessToken(token);
                // we don't want to give refresh tokens to public clients which authenticate via
                // PKCE
                norefresh.setRefreshToken(null);
                token = norefresh;
            }
        }

        return token;
    }
	
	@Override
	protected OAuth2Authentication getOAuth2Authentication(ClientDetails client, TokenRequest tokenRequest) {

		Map<String, String> parameters = tokenRequest.getRequestParameters();
		String authorizationCode = parameters.get("code");
		String redirectUri = parameters.get(OAuth2Utils.REDIRECT_URI);

		if (authorizationCode == null) {
			throw new InvalidRequestException("An authorization code must be supplied.");
		}

		//TODO evaluate rework
		//this implementation consumes authCode *before* checking if client is authorized via challenge/verifier
		//this exposes AAC to an DOS attack where an unauthenticated client can consume not owned codes
		OAuth2Authentication storedAuth = authorizationCodeServices.consumeAuthorizationCode(authorizationCode);
		if (storedAuth == null) {
			throw new InvalidGrantException("Invalid authorization code: " + authorizationCode);
		}
		
		OAuth2Request pendingOAuth2Request = storedAuth.getOAuth2Request();
		// https://jira.springsource.org/browse/SECOAUTH-333
		// This might be null, if the authorization was done without the redirect_uri parameter
		String redirectUriApprovalParameter = pendingOAuth2Request.getRequestParameters().get(
				OAuth2Utils.REDIRECT_URI);

		if ((redirectUri != null || redirectUriApprovalParameter != null)
				&& !pendingOAuth2Request.getRedirectUri().equals(redirectUri)) {
			throw new RedirectMismatchException("Redirect URI mismatch.");
		}

		String pendingClientId = pendingOAuth2Request.getClientId();
		String clientId = tokenRequest.getClientId();
		if (clientId != null && !clientId.equals(pendingClientId)) {
			// just a sanity check.
			throw new InvalidClientException("Client ID mismatch");
		}

		//validate code verifier by extracting code challenge from authorization request
		String codeChallenge = pendingOAuth2Request.getRequestParameters().get(AACOAuth2Utils.CODE_CHALLENGE);
		String codeChallengeMethod = pendingOAuth2Request.getRequestParameters().get(AACOAuth2Utils.CODE_CHALLENGE_METHOD);
		String codeVerifier = tokenRequest.getRequestParameters().get(AACOAuth2Utils.CODE_VERIFIER);
		
		//we need to be sure this is a PKCE request
		if (StringUtils.isEmpty(codeChallenge) || StringUtils.isEmpty(codeChallengeMethod)) {
		    //this is NOT a PKCE authcode
            throw new InvalidGrantException("Invalid authorization code: " + authorizationCode);
		}
		
        // validate challenge+verifier
        if (!AACOAuth2Utils.getCodeChallenge(codeVerifier, codeChallengeMethod).equals(codeChallenge)) {
            // TODO we should re-insert the authentication or avoid consuming it before this
            throw new InvalidGrantException(codeVerifier + " does not match expected code verifier.");
        }
		
		
		
		// Secret is not required in the authorization request, so it won't be available
		// in the pendingAuthorizationRequest. We do want to check that a secret is provided
		// in the token request, but that happens elsewhere.

		Map<String, String> combinedParameters = new HashMap<String, String>(pendingOAuth2Request
				.getRequestParameters());
		// Combine the parameters adding the new ones last so they override if there are any clashes
		combinedParameters.putAll(parameters);
		
//		Authentication oldAuth = SecurityContextHolder.getContext().getAuthentication();
		Authentication userAuth = storedAuth.getUserAuthentication();
		
//		SecurityContextHolder.getContext().setAuthentication(userAuth);
		// Make a new stored request with the combined parameters
		OAuth2Request finalStoredOAuth2Request = pendingOAuth2Request.createOAuth2Request(combinedParameters);
		
//		SecurityContextHolder.getContext().setAuthentication(oldAuth);
		
		return new OAuth2Authentication(finalStoredOAuth2Request, userAuth);

	}

}
