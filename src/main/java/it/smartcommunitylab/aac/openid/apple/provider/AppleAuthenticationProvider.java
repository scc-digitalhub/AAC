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

package it.smartcommunitylab.aac.openid.apple.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.accounts.persistence.UserAccountService;
import it.smartcommunitylab.aac.attributes.OpenIdAttributesSet;
import it.smartcommunitylab.aac.attributes.model.AttributeSet;
import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.identity.provider.IdentityProvider;
import it.smartcommunitylab.aac.openid.OIDCKeys;
import it.smartcommunitylab.aac.openid.apple.auth.AppleClientAuthenticationParametersConverter;
import it.smartcommunitylab.aac.openid.apple.model.AppleOidcUserData;
import it.smartcommunitylab.aac.openid.auth.OIDCAuthenticationException;
import it.smartcommunitylab.aac.openid.auth.OIDCAuthenticationToken;
import it.smartcommunitylab.aac.openid.model.OIDCUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.openid.persistence.OIDCUserAccount;
import it.smartcommunitylab.aac.openid.provider.OIDCAuthenticationProvider;
import it.smartcommunitylab.aac.openid.service.IdTokenOidcUserService;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationToken;
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequestEntityConverter;
import org.springframework.security.oauth2.client.oidc.authentication.OidcAuthorizationCodeAuthenticationProvider;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public class AppleAuthenticationProvider extends OIDCAuthenticationProvider {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final UserAccountService<OIDCUserAccount> accountService;
    private final String repositoryId;

    private final OidcAuthorizationCodeAuthenticationProvider oidcProvider;

    private static ObjectMapper mapper = new ObjectMapper();

    public AppleAuthenticationProvider(
        String providerId,
        UserAccountService<OIDCUserAccount> accountService,
        AppleIdentityProviderConfig config,
        String realm
    ) {
        super(SystemKeys.AUTHORITY_APPLE, providerId, accountService, config.toOidcProviderConfig(), realm);
        Assert.notNull(config, "provider config is mandatory");

        this.accountService = accountService;

        // repositoryId is always providerId, oidc isolates data per provider
        this.repositoryId = providerId;

        // build appropriate client auth request converter
        OAuth2AuthorizationCodeGrantRequestEntityConverter requestEntityConverter =
            new OAuth2AuthorizationCodeGrantRequestEntityConverter();
        // use set to replace all standard parameters, we need to control the request
        requestEntityConverter.setParametersConverter(new AppleClientAuthenticationParametersConverter(config));

        // we support only authCode login
        DefaultAuthorizationCodeTokenResponseClient accessTokenResponseClient =
            new DefaultAuthorizationCodeTokenResponseClient();
        accessTokenResponseClient.setRequestEntityConverter(requestEntityConverter);

        // use id token user service, apple does not expose a userinfo endpoint
        this.oidcProvider =
            new OidcAuthorizationCodeAuthenticationProvider(accessTokenResponseClient, new IdTokenOidcUserService());
    }

    @Override
    public Authentication doAuthenticate(Authentication authentication) throws AuthenticationException {
        // extract registrationId and check if matches our providerId
        OAuth2LoginAuthenticationToken loginAuthenticationToken = (OAuth2LoginAuthenticationToken) authentication;
        String registrationId = loginAuthenticationToken.getClientRegistration().getRegistrationId();
        if (!getProvider().equals(registrationId)) {
            // this login is not for us, let others process it
            return null;
        }

        // TODO extract codeResponse + tokenResponse for audit
        String authorizationRequestUri = loginAuthenticationToken
            .getAuthorizationExchange()
            .getAuthorizationRequest()
            .getAuthorizationRequestUri();
        String authorizationResponseUri = loginAuthenticationToken
            .getAuthorizationExchange()
            .getAuthorizationResponse()
            .getRedirectUri();

        try {
            // delegate to oidc provider
            Authentication auth = oidcProvider.authenticate(authentication);

            if (auth != null) {
                // convert to our authToken and clear exchange information, those are not
                // serializable..
                OAuth2LoginAuthenticationToken authenticationToken = (OAuth2LoginAuthenticationToken) auth;
                // extract sub identifier
                String subject = authenticationToken.getPrincipal().getAttribute(IdTokenClaimNames.SUB);
                if (!StringUtils.hasText(subject)) {
                    throw new OAuth2AuthenticationException(new OAuth2Error("invalid_request"));
                }

                // check if account is present and locked
                OIDCUserAccount account = accountService.findAccountById(repositoryId, subject);
                if (account != null && account.isLocked()) {
                    throw new OIDCAuthenticationException(
                        new OAuth2Error("invalid_request"),
                        "account not available",
                        authorizationRequestUri,
                        authorizationResponseUri,
                        null,
                        null
                    );
                }

                OAuth2User principal = authenticationToken.getPrincipal();

                // check if authRequest contains user profile
                // apple sends `user` along with `code` not with token response..
                OAuth2AuthorizationRequest authorizationRequest = loginAuthenticationToken
                    .getAuthorizationExchange()
                    .getAuthorizationRequest();
                if (authorizationRequest.getAdditionalParameters().containsKey("user")) {
                    // fetch and parse
                    try {
                        // map to user info, expect a json as string
                        AppleOidcUserData userData = mapper.readValue(
                            (String) authorizationRequest.getAdditionalParameters().get("user"),
                            AppleOidcUserData.class
                        );

                        Map<String, Object> claims = new HashMap<>();
                        claims.put("email", userData.getEmail());
                        claims.put("firstName", userData.getFirstName());
                        claims.put("lastName", userData.getLastName());
                        OidcUserInfo userInfo = new OidcUserInfo(claims);

                        // rebuild principal with new info
                        principal =
                            new DefaultOidcUser(
                                principal.getAuthorities(),
                                ((OidcUser) principal).getIdToken(),
                                userInfo
                            );
                    } catch (Exception e) {
                        // skip invalid data
                        logger.error("cannot parse userinfo from user data");
                        logger.trace(
                            "userdata: " + String.valueOf(authorizationRequest.getAdditionalParameters().get("user"))
                        );
                    }
                }

                auth =
                    new OIDCAuthenticationToken(
                        subject,
                        principal,
                        authenticationToken.getAccessToken(),
                        authenticationToken.getRefreshToken(),
                        Collections.singleton(new SimpleGrantedAuthority(Config.R_USER))
                    );
            }

            return auth;
        } catch (OAuth2AuthenticationException e) {
            throw new OIDCAuthenticationException(
                e.getError(),
                e.getMessage(),
                authorizationRequestUri,
                authorizationResponseUri,
                null,
                null
            );
        }
    }

    @Override
    protected OIDCUserAuthenticatedPrincipal createUserPrincipal(Object principal) {
        // we need to unpack user and fetch properties
        OAuth2User oauthDetails = (OAuth2User) principal;

        // upstream subject identifier
        String subject = oauthDetails.getAttribute(IdTokenClaimNames.SUB);

        // name is always available, is mapped via provider configuration
        String username = oauthDetails.getName();

        // we still don't have userId
        String userId = null;

        // rebuild details to clear authorities
        // by default they contain the response body, ie. the full accessToken +
        // everything else

        // fetch attributes from user when provided
        // these are provided only at first login, so update when not null
        String firstName = oauthDetails.getAttribute("firstName");
        String lastName = oauthDetails.getAttribute("lastName");

        // need account to load saved attributes
        OIDCUserAccount account = accountService.findAccountById(repositoryId, subject);
        if (account != null) {
            firstName = firstName != null ? firstName : account.getGivenName();
            lastName = lastName != null ? lastName : account.getFamilyName();
        }

        // bind principal to ourselves
        OIDCUserAuthenticatedPrincipal user = new OIDCUserAuthenticatedPrincipal(
            getAuthority(),
            getProvider(),
            getRealm(),
            userId,
            subject
        );
        user.setUsername(username);
        user.setPrincipal(oauthDetails);

        // custom attribute mapping
        if (executionService != null && StringUtils.hasText(customMappingFunction)) {
            try {
                // get all attributes from principal except jwt attrs
                // TODO handle all attributes not only strings.
                Map<String, Serializable> principalAttributes = user
                    .getAttributes()
                    .entrySet()
                    .stream()
                    .filter(e -> !OIDCKeys.JWT_ATTRIBUTES.contains(e.getKey()))
                    .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
                // execute script
                Map<String, Serializable> customAttributes = executionService.executeFunction(
                    IdentityProvider.ATTRIBUTE_MAPPING_FUNCTION,
                    customMappingFunction,
                    principalAttributes
                );

                // update map
                if (customAttributes != null) {
                    // replace map
                    principalAttributes =
                        customAttributes
                            .entrySet()
                            .stream()
                            .filter(e -> !OIDCKeys.JWT_ATTRIBUTES.contains(e.getKey()))
                            .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
                    user.setAttributes(principalAttributes);
                }
            } catch (SystemException | InvalidDefinitionException ex) {
                logger.debug("error mapping principal attributes via script: " + ex.getMessage());
            }
        }

        // map attributes to openid set and flatten to string
        AttributeSet oidcAttributeSet = openidMapper.mapAttributes(user.getAttributes());
        Map<String, String> oidcAttributes = oidcAttributeSet
            .getAttributes()
            .stream()
            .collect(Collectors.toMap(a -> a.getKey(), a -> a.exportValue()));

        // fetch email when available
        String email = oidcAttributes.get(OpenIdAttributesSet.EMAIL);

        boolean emailVerified = StringUtils.hasText(oidcAttributes.get(OpenIdAttributesSet.EMAIL_VERIFIED))
            ? Boolean.parseBoolean(oidcAttributes.get(OpenIdAttributesSet.EMAIL_VERIFIED))
            : false;

        // update principal
        user.setEmail(email);
        user.setEmailVerified(emailVerified);

        // use email as username when provided
        if (StringUtils.hasText(email)) {
            user.setUsername(email);
        }

        // save attributes from user as standard
        Map<String, Serializable> userAttributes = new HashMap<>();
        userAttributes.put(OpenIdAttributesSet.GIVEN_NAME, firstName);
        userAttributes.put(OpenIdAttributesSet.FAMILY_NAME, lastName);
        user.setAttributes(userAttributes);

        return user;
    }
}
