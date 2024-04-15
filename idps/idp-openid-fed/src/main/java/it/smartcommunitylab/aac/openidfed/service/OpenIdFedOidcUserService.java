/**
 * Copyright 2024 the original author or authors
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

package it.smartcommunitylab.aac.openidfed.service;

import com.nimbusds.jose.jwk.JWKSet;
import it.smartcommunitylab.aac.jwt.JoseRestOperations;
import it.smartcommunitylab.aac.oidc.events.OAuth2UserResponseEvent;
import it.smartcommunitylab.aac.openidfed.auth.OpenIdFedUserRequestEntityConverter;
import it.smartcommunitylab.aac.openidfed.provider.OpenIdFedIdentityProviderConfig;
import java.text.ParseException;
import java.util.Collections;
import java.util.Map;
import net.minidev.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.converter.ClaimTypeConverter;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public class OpenIdFedOidcUserService extends OidcUserService implements ApplicationEventPublisherAware {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final OpenIdFedIdentityProviderConfig config;

    private DefaultOAuth2UserService oauth2UserService;
    private AuditAwareClaimTypeConverter claimTypeConverter;
    private OpenIdFedUserRequestEntityConverter userRequestEntityConverter;

    public OpenIdFedOidcUserService(OpenIdFedIdentityProviderConfig config) {
        Assert.notNull(config, "provider config is required");
        this.config = config;

        //configure oauth2 user service
        oauth2UserService = new DefaultOAuth2UserService();
        userRequestEntityConverter = new OpenIdFedUserRequestEntityConverter(config);
        oauth2UserService.setRequestEntityConverter(userRequestEntityConverter);

        super.setOauth2UserService(oauth2UserService);

        //instrument a custom claim converter to audit response
        //hack
        claimTypeConverter =
            new AuditAwareClaimTypeConverter(new ClaimTypeConverter(createDefaultClaimTypeConverters()));

        super.setClaimTypeConverterFactory(clientRegistration -> claimTypeConverter);

        //always load user profile - hack
        //TODO add method to evaluate if scopes OR claims are requested
        setAccessibleScopes(Collections.singleton("openid"));
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher eventPublisher) {
        userRequestEntityConverter.setApplicationEventPublisher(eventPublisher);
        claimTypeConverter.setApplicationEventPublisher(eventPublisher);
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        Assert.notNull(userRequest, "userRequest cannot be null");
        Assert.notNull(userRequest.getClientRegistration(), "clientRegistration cannot be null");

        //rebuild restTemplate for the provider configuration
        String jwksUri = userRequest.getClientRegistration().getProviderDetails().getJwkSetUri();
        JWKSet jwks = null;
        try {
            //try to parse jwks from metadata
            Object value = userRequest
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
                config.getConfigMap().getUserInfoJWEAlg() != null && config.getConfigMap().getUserInfoJWEEnc() != null
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
                config.getConfigMap().getUserInfoJWEAlg() != null && config.getConfigMap().getUserInfoJWEEnc() != null
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

        //set error handler
        restOperations.setErrorHandler(new OAuth2ErrorResponseErrorHandler());

        oauth2UserService.setRestOperations(restOperations);

        //delegate
        return super.loadUser(userRequest);
    }

    private class AuditAwareClaimTypeConverter
        implements Converter<Map<String, Object>, Map<String, Object>>, ApplicationEventPublisherAware {

        private ClaimTypeConverter claimTypeConverter;
        private ApplicationEventPublisher eventPublisher;

        public AuditAwareClaimTypeConverter(ClaimTypeConverter claimTypeConverter) {
            Assert.notNull(claimTypeConverter, "claim type converter can not be null");
            this.claimTypeConverter = claimTypeConverter;
        }

        public Map<String, Object> convert(Map<String, Object> claims) {
            if (eventPublisher != null) {
                OAuth2UserResponseEvent event = new OAuth2UserResponseEvent(
                    config.getAuthority(),
                    config.getProvider(),
                    config.getRealm(),
                    claims
                );

                eventPublisher.publishEvent(event);
            }

            return claimTypeConverter.convert(claims);
        }

        @Override
        public void setApplicationEventPublisher(ApplicationEventPublisher eventPublisher) {
            this.eventPublisher = eventPublisher;
        }
    }
}
