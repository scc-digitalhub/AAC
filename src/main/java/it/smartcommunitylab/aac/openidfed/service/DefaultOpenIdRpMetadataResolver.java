/**
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

package it.smartcommunitylab.aac.openidfed.service;

import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatementClaimsSet;
import com.nimbusds.openid.connect.sdk.federation.entities.FederationEntityMetadata;
import com.nimbusds.openid.connect.sdk.rp.OIDCClientMetadata;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.openidfed.provider.OpenIdFedIdentityProviderConfig;
import it.smartcommunitylab.aac.openidfed.provider.OpenIdFedIdentityProviderConfigMap;
import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.web.util.UrlUtils;
import org.springframework.util.Assert;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

public class DefaultOpenIdRpMetadataResolver implements OpenIdRpMetadataResolver {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final int VALIDITY_SECONDS = 3600;
    private int validitySeconds = VALIDITY_SECONDS;

    public void setValiditySeconds(int validitySeconds) {
        Assert.isTrue(validitySeconds > 30, "validity must be >=30 seconds");
        this.validitySeconds = validitySeconds;
    }

    public String resolveEntityMetadata(OpenIdFedIdentityProviderConfig config, HttpServletRequest request) {
        // extract baseUrl from request
        String baseUrl = extractBaseUrl(request);
        EntityStatement statement = generateEntityStatement(config, baseUrl);
        return statement.getSignedStatement().serialize();
    }

    public EntityStatement generateEntityStatement(OpenIdFedIdentityProviderConfig config, String baseUrl) {
        try {
            OpenIdFedIdentityProviderConfigMap map = config.getConfigMap();
            //expand client identifier as url
            String clientId = expandRedirectUri(baseUrl, config.getClientId(), "id").toString();

            // entity statement
            EntityID iss = new EntityID(clientId);
            EntityID sub = new EntityID(clientId);

            Instant issuedAt = Instant.now();
            Instant expiresAt = issuedAt.plusSeconds(validitySeconds);

            JWK federationKey = config.getFederationJWK();
            JWKSet federationJwks = new JWKSet(federationKey);

            List<EntityID> authorities = map.getAuthorityHints() != null
                ? map.getAuthorityHints().stream().map(EntityID::new).collect(Collectors.toList())
                : Collections.emptyList();

            EntityStatementClaimsSet claims = new EntityStatementClaimsSet(
                iss,
                sub,
                Date.from(issuedAt),
                Date.from(expiresAt),
                federationJwks.toPublicJWKSet()
            );

            claims.setAuthorityHints(authorities);

            // client metadata
            JWKSet clientJwks = config.getClientJWKSet();

            OIDCClientMetadata metadata = new OIDCClientMetadata();
            metadata.setName(map.getClientName());
            metadata.setEmailContacts(map.getContacts());
            metadata.setClientRegistrationTypes(Collections.singletonList(config.getClientRegistrationType()));
            metadata.setApplicationType(config.getApplicationType());
            metadata.setGrantTypes(new HashSet<>(config.getGrantTypes()));
            metadata.setResponseTypes(new HashSet<>(config.getResponseTypes()));
            metadata.setScope(Scope.parse(config.getScopes()));

            //set JWS algorithms supported for responses
            //default to RSA which is MANDATORY for everyone to support
            //TODO make configurable
            metadata.setUserInfoJWSAlg(JWSAlgorithm.RS256);
            metadata.setIDTokenJWSAlg(JWSAlgorithm.RS256);

            //set JWE algorithms if provided
            //TODO derive from key if present
            if (map.getUserInfoJWEAlg() != null) {
                metadata.setUserInfoJWEAlg(JWEAlgorithm.parse(map.getUserInfoJWEAlg().getValue()));
            }
            if (map.getUserInfoJWEEnc() != null) {
                metadata.setUserInfoJWEEnc(EncryptionMethod.parse(map.getUserInfoJWEEnc().getValue()));
            }

            //use a custom field for client_id because we lack support
            metadata.setCustomField("client_id", clientId);

            // build uri
            metadata.setRedirectionURI(expandRedirectUri(baseUrl, config.getRedirectUrl(), "login"));
            metadata.setTokenEndpointAuthMethod(config.getClientAuthenticationMethod());

            metadata.setJWKSet(clientJwks.toPublicJWKSet());
            metadata.setSubjectType(config.getSubjectType());
            claims.setRPMetadata(metadata);

            //federation entity
            FederationEntityMetadata federation = new FederationEntityMetadata();
            federation.setOrganizationName(map.getOrganizationName());
            federation.setContacts(map.getContacts());
            //TODO set realm login as homepage
            federation.setHomepageURI(null);
            //TODO set realm logo
            federation.setLogoURI(null);
            federation.setFederationResolveEndpointURI(expandRedirectUri(baseUrl, config.getRedirectUrl(), "resolve"));
            claims.setFederationEntityMetadata(federation);

            // sign and return
            return EntityStatement.sign(claims, federationKey);
        } catch (JOSEException e) {
            logger.error("error building metadata for {}:{}", String.valueOf(config.getProvider()), e.getMessage());
            throw new SystemException("Error building metadata for provider");
        }
    }

    public static String extractBaseUrl(HttpServletRequest request) {
        UriComponents uriComponents = UriComponentsBuilder
            .fromHttpUrl(UrlUtils.buildFullRequestUrl(request))
            .replacePath(request.getContextPath())
            .replaceQuery(null)
            .fragment(null)
            .build();

        return uriComponents.toUriString();
    }

    public static URI expandRedirectUri(String baseUrl, String redirectUri, String action) {
        Map<String, String> uriVariables = new HashMap<>();
        uriVariables.put("baseUrl", baseUrl);
        uriVariables.put("action", (action != null) ? action : "");

        return UriComponentsBuilder.fromUriString(redirectUri).buildAndExpand(uriVariables).toUri();
    }
}
