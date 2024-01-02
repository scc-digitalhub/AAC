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

package it.smartcommunitylab.aac.openidfed.provider;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.accounts.persistence.UserAccountService;
import it.smartcommunitylab.aac.attributes.OpenIdAttributesSet;
import it.smartcommunitylab.aac.attributes.mapper.OpenIdAttributesMapper;
import it.smartcommunitylab.aac.attributes.model.AttributeSet;
import it.smartcommunitylab.aac.claims.ScriptExecutionService;
import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.auth.ExtendedAuthenticationProvider;
import it.smartcommunitylab.aac.identity.provider.IdentityProvider;
import it.smartcommunitylab.aac.jwt.JoseRestOperations;
import it.smartcommunitylab.aac.oidc.OIDCKeys;
import it.smartcommunitylab.aac.oidc.auth.OIDCAuthenticationException;
import it.smartcommunitylab.aac.oidc.auth.OIDCAuthenticationToken;
import it.smartcommunitylab.aac.oidc.auth.OIDCIdTokenDecoderFactory;
import it.smartcommunitylab.aac.oidc.model.OIDCUserAccount;
import it.smartcommunitylab.aac.oidc.model.OIDCUserAuthenticatedPrincipal;
import java.io.Serializable;
import java.text.ParseException;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minidev.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthorizationCodeAuthenticationProvider;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationToken;
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.NimbusJwtClientAuthenticationParametersConverter;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequestEntityConverter;
import org.springframework.security.oauth2.client.oidc.authentication.OidcAuthorizationCodeAuthenticationProvider;
import org.springframework.security.oauth2.client.oidc.authentication.OidcIdTokenDecoderFactory;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public class OpenIdFedAuthenticationProvider
    extends ExtendedAuthenticationProvider<OIDCUserAuthenticatedPrincipal, OIDCUserAccount> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final UserAccountService<OIDCUserAccount> accountService;
    private final OpenIdFedIdentityProviderConfig config;
    private final String repositoryId;

    protected String customMappingFunction;
    protected ScriptExecutionService executionService;
    protected final OpenIdAttributesMapper openidMapper;

    private final OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient;

    // private final LoadingCache<String, OAuth2UserService<OAuth2UserRequest, OAuth2User>> oauth2Services = CacheBuilder
    //     .newBuilder()
    //     .expireAfterWrite(5, TimeUnit.MINUTES)
    //     .build(
    //         new CacheLoader<String, OAuth2UserService<OAuth2UserRequest, OAuth2User>>() {
    //             @Override
    //             public OAuth2UserService<OAuth2UserRequest, OAuth2User> load(String key) throws Exception {
    //                 return federationResolver
    //                     .listFederationEntities(trustAnchor, key, EntityType.OPENID_PROVIDER)
    //                     .stream()
    //                     .map(e -> e.getValue())
    //                     .collect(Collectors.toList());
    //             }
    //         }
    //     );

    public OpenIdFedAuthenticationProvider(
        String providerId,
        UserAccountService<OIDCUserAccount> accountService,
        OpenIdFedIdentityProviderConfig config,
        String realm
    ) {
        this(SystemKeys.AUTHORITY_OPENIDFED, providerId, accountService, config, realm);
    }

    public OpenIdFedAuthenticationProvider(
        String authority,
        String providerId,
        UserAccountService<OIDCUserAccount> accountService,
        OpenIdFedIdentityProviderConfig config,
        String realm
    ) {
        super(authority, providerId, realm);
        Assert.notNull(accountService, "account service is mandatory");
        Assert.notNull(config, "provider config is mandatory");

        this.config = config;
        this.accountService = accountService;

        // repositoryId is always providerId, isolate data per provider
        this.repositoryId = providerId;

        // attribute mapper to extract email
        this.openidMapper = new OpenIdAttributesMapper();

        // build appropriate client auth request converter
        OAuth2AuthorizationCodeGrantRequestEntityConverter requestEntityConverter =
            new OAuth2AuthorizationCodeGrantRequestEntityConverter();

        // private key jwt resolver, as per
        // https://tools.ietf.org/html/rfc7523#section-2.2
        // fetch key
        JWK jwk = config.getClientSignatureJWK();
        // build resolver only for this registration to retrieve client key
        Function<ClientRegistration, JWK> jwkResolver = clientRegistration -> jwk;
        requestEntityConverter.addParametersConverter(
            new NimbusJwtClientAuthenticationParametersConverter<>(jwkResolver)
        );

        // we support only authCode login
        DefaultAuthorizationCodeTokenResponseClient responseClient = new DefaultAuthorizationCodeTokenResponseClient();
        responseClient.setRequestEntityConverter(requestEntityConverter);

        this.accessTokenResponseClient = responseClient;
        //build rest template with support for jwt/jose
        //TODO remove and build a custom oauth2userservice because we can not know keys beforehand
        // JoseRestOperations restOperations = new JoseRestOperations(realm);

    }

    @Override
    public Authentication doAuthenticate(Authentication authentication) throws AuthenticationException {
        OAuth2LoginAuthenticationToken loginAuthenticationToken = (OAuth2LoginAuthenticationToken) authentication;

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
            //build oauth2 user service for this registration
            //TODO cache by registrationId
            DefaultOAuth2UserService oauth2UserService = new DefaultOAuth2UserService();
            String jwksUri = loginAuthenticationToken.getClientRegistration().getProviderDetails().getJwkSetUri();
            JWKSet jwks = null;
            try {
                //try to parse jwks from metadata
                Object value = loginAuthenticationToken
                    .getClientRegistration()
                    .getProviderDetails()
                    .getConfigurationMetadata()
                    .get("jwks");

                if (value instanceof JSONObject) {
                    jwks = JWKSet.parse(((JSONObject) value).toJSONString());
                }
            } catch (ParseException e) {
                logger.error("error reading jwks from metadata: " + e.getMessage(), e);
            }

            JoseRestOperations restOperations = null;

            if (StringUtils.hasText(jwksUri)) {
                //we expect a response encrypted with out public key, or just signed with op keys
                restOperations = new JoseRestOperations(jwksUri);
                if (
                    config.getConfigMap().getUserInfoJWEAlg() != null &&
                    config.getConfigMap().getUserInfoJWEEnc() != null
                ) {
                    restOperations =
                        new JoseRestOperations(
                            jwksUri,
                            config.getClientEncryptionJWK(),
                            config.getConfigMap().getUserInfoJWEAlg().getValue(),
                            config.getConfigMap().getUserInfoJWEEnc().getValue()
                        );
                }
            }

            if (!StringUtils.hasText(jwksUri) && jwks != null) {
                //use jwks
                restOperations = new JoseRestOperations(jwks);
                if (
                    config.getConfigMap().getUserInfoJWEAlg() != null &&
                    config.getConfigMap().getUserInfoJWEEnc() != null
                ) {
                    restOperations =
                        new JoseRestOperations(
                            jwks,
                            config.getClientEncryptionJWK(),
                            config.getConfigMap().getUserInfoJWEAlg().getValue(),
                            config.getConfigMap().getUserInfoJWEEnc().getValue()
                        );
                }
            }

            if (restOperations == null) {
                throw new OAuth2AuthenticationException("invalid_request");
            }

            oauth2UserService.setRestOperations(restOperations);

            //build oidc provider
            OidcUserService oidcUserService = new OidcUserService();
            oidcUserService.setOauth2UserService(oauth2UserService);
            //always load user profile - hack
            //TODO evaluate if scopes OR claims are requested
            oidcUserService.setAccessibleScopes(Collections.singleton("openid"));
            OidcAuthorizationCodeAuthenticationProvider oidcProvider = new OidcAuthorizationCodeAuthenticationProvider(
                accessTokenResponseClient,
                oidcUserService
            );
            // replace jwtDecoderFactory to support providers with jwks in place of jwksUri
            oidcProvider.setJwtDecoderFactory(new OIDCIdTokenDecoderFactory());

            // use a custom authorities mapper to cleanup authorities spring injects
            // default impl translates the whole oauth response as an authority..
            oidcProvider.setAuthoritiesMapper(nullAuthoritiesMapper);

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

                auth =
                    new OIDCAuthenticationToken(
                        subject,
                        authenticationToken.getPrincipal(),
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
        } catch (NullPointerException e) {
            throw new OIDCAuthenticationException(
                new OAuth2Error(OAuth2ErrorCodes.SERVER_ERROR),
                e.getMessage(),
                authorizationRequestUri,
                authorizationResponseUri,
                null,
                null
            );
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication != null && OAuth2LoginAuthenticationToken.class.isAssignableFrom(authentication);
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

        boolean defaultVerifiedStatus = config.getConfigMap().getTrustEmailAddress() != null
            ? config.getConfigMap().getTrustEmailAddress()
            : false;
        boolean emailVerified = StringUtils.hasText(oidcAttributes.get(OpenIdAttributesSet.EMAIL_VERIFIED))
            ? Boolean.parseBoolean(oidcAttributes.get(OpenIdAttributesSet.EMAIL_VERIFIED))
            : defaultVerifiedStatus;

        if (Boolean.TRUE.equals(config.getConfigMap().getAlwaysTrustEmailAddress())) {
            emailVerified = true;
        }

        // read username from attributes, mapper can replace it
        username =
            StringUtils.hasText(oidcAttributes.get(OpenIdAttributesSet.PREFERRED_USERNAME))
                ? oidcAttributes.get(OpenIdAttributesSet.PREFERRED_USERNAME)
                : user.getUsername();

        // update principal
        user.setUsername(username);
        user.setEmail(email);
        user.setEmailVerified(emailVerified);

        return user;
    }

    private final GrantedAuthoritiesMapper nullAuthoritiesMapper = (authorities -> Collections.emptyList());
}
