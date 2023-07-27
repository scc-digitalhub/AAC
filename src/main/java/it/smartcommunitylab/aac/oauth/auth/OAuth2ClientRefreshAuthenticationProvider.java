/*
 * Copyright 2023 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.oauth.auth;

import com.nimbusds.oauth2.sdk.GrantType;
import com.nimbusds.oauth2.sdk.ParseException;
import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.common.NoSuchClientException;
import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.core.auth.ClientAuthentication;
import it.smartcommunitylab.aac.core.auth.ClientAuthenticationProvider;
import it.smartcommunitylab.aac.core.auth.UserAuthentication;
import it.smartcommunitylab.aac.oauth.model.OAuth2ClientDetails;
import it.smartcommunitylab.aac.oauth.service.OAuth2ClientDetailsService;
import it.smartcommunitylab.aac.oauth.store.ExtTokenStore;
import java.util.Collection;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.security.oauth2.common.exceptions.InvalidGrantException;
import org.springframework.security.oauth2.common.exceptions.InvalidRequestException;
import org.springframework.security.oauth2.core.endpoint.PkceParameterNames;
import org.springframework.security.oauth2.provider.ClientRegistrationException;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public class OAuth2ClientRefreshAuthenticationProvider
    extends ClientAuthenticationProvider
    implements InitializingBean {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final OAuth2ClientDetailsService clientDetailsService;

    // we need to peek at token store to load original auth
    private final ExtTokenStore tokenStore;

    public OAuth2ClientRefreshAuthenticationProvider(
        OAuth2ClientDetailsService clientDetailsService,
        ExtTokenStore tokenStore
    ) {
        Assert.notNull(tokenStore, "token store is required");
        Assert.notNull(clientDetailsService, "client details service is required");
        this.clientDetailsService = clientDetailsService;
        this.tokenStore = tokenStore;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(clientService, "client service is required");
    }

    @Override
    public ClientAuthentication authenticate(Authentication authentication) throws AuthenticationException {
        Assert.isInstanceOf(
            OAuth2ClientRefreshAuthenticationToken.class,
            authentication,
            "Only OAuth2ClientRefreshAuthenticationToken is supported"
        );

        OAuth2ClientRefreshAuthenticationToken authRequest = (OAuth2ClientRefreshAuthenticationToken) authentication;
        String clientId = authRequest.getPrincipal();
        String refreshTokenValue = authRequest.getRefreshToken();
        String authenticationMethod = authRequest.getAuthenticationMethod();

        if (!StringUtils.hasText(clientId) || !StringUtils.hasText(refreshTokenValue)) {
            throw new BadCredentialsException("missing required parameters in request");
        }

        try {
            // load details, we need to check request
            OAuth2ClientDetails client = clientDetailsService.loadClientByClientId(clientId);

            // check if client can authenticate with this scheme
            if (!client.getAuthenticationMethods().contains(authenticationMethod)) {
                this.logger.debug("Failed to authenticate since client can not use scheme " + authenticationMethod);
                throw new BadCredentialsException("invalid authentication");
            }

            /*
             * We authenticate clients by checking if refresh token rotation is set and if
             * the original authentication used the same method
             */
            if (!client.isRefreshTokenRotation()) {
                this.logger.debug("Failed to authenticate since client has token rotation disabled");
                throw new BadCredentialsException("invalid authentication");
            }

            OAuth2RefreshToken refreshToken = tokenStore.readRefreshToken(refreshTokenValue);
            if (refreshToken == null) {
                throw new InvalidGrantException("Invalid refresh token: " + refreshTokenValue);
            }

            OAuth2Authentication oauth = tokenStore.readAuthenticationForRefreshToken(refreshToken);
            if (oauth == null) {
                // don't leak
                throw new BadCredentialsException("invalid request");
            }

            // check if userAuth is present
            Authentication userAuth = oauth.getUserAuthentication();
            if (userAuth == null || !(userAuth instanceof UserAuthentication)) {
                throw new InvalidRequestException("refresh requires a valid user authentication");
            }

            OAuth2Request oauth2Request = oauth.getOAuth2Request();

            if (!oauth2Request.getClientId().equals(clientId)) {
                // client id does not match
                throw new BadCredentialsException("invalid request");
            }

            // check request has offline_access scope
            Set<String> scopes = oauth2Request.getScope();
            if (!scopes.contains(Config.SCOPE_OFFLINE_ACCESS)) {
                throw new InvalidRequestException("refresh requires offline_access scope");
            }

            // check request was auth_code
            GrantType grantType = null;
            try {
                grantType = GrantType.parse(oauth2Request.getGrantType());
            } catch (ParseException e) {
                // invalid grant type, should not happen here
                throw new InvalidRequestException("invalid token");
            }

            if (!GrantType.AUTHORIZATION_CODE.equals(grantType)) {
                throw new InvalidRequestException("refresh requires auth_code grant ype as origin");
            }

            // check auth method is PKCE
            // TODO evaluate skipping this check
            String codeChallenge = oauth2Request.getRequestParameters().get(PkceParameterNames.CODE_CHALLENGE);
            String codeChallengeMethod = oauth2Request
                .getRequestParameters()
                .get(PkceParameterNames.CODE_CHALLENGE_METHOD);

            // we need to be sure this is a PKCE request
            if (!StringUtils.hasText(codeChallenge) || !StringUtils.hasText(codeChallengeMethod)) {
                // this is NOT a PKCE authcode
                throw new BadCredentialsException("invalid request");
            }

            // load authorities from clientService
            Collection<GrantedAuthority> authorities;
            try {
                ClientDetails clientDetails = clientService.loadClient(clientId);
                authorities = clientDetails.getAuthorities();
            } catch (NoSuchClientException e) {
                throw new ClientRegistrationException("invalid client");
            }

            // result contains credentials, someone later on will need to call
            // eraseCredentials
            OAuth2ClientRefreshAuthenticationToken result = new OAuth2ClientRefreshAuthenticationToken(
                clientId,
                refreshTokenValue,
                authenticationMethod,
                authorities
            );

            // save details
            // TODO add ClientDetails in addition to oauth2ClientDetails
            result.setOAuth2ClientDetails(client);
            result.setWebAuthenticationDetails(authRequest.getWebAuthenticationDetails());

            return result;
        } catch (ClientRegistrationException e) {
            throw new BadCredentialsException("invalid authentication");
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return (OAuth2ClientRefreshAuthenticationToken.class.isAssignableFrom(authentication));
    }
}
