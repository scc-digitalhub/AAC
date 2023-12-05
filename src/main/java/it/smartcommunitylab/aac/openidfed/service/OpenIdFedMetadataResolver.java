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

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.oauth2.sdk.GrantType;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatementClaimsSet;
import com.nimbusds.openid.connect.sdk.federation.registration.ClientRegistrationType;
import com.nimbusds.openid.connect.sdk.rp.ApplicationType;
import com.nimbusds.openid.connect.sdk.rp.OIDCClientMetadata;
import it.smartcommunitylab.aac.openidfed.provider.OpenIdFedIdentityProviderConfig;
import it.smartcommunitylab.aac.openidfed.provider.OpenIdFedIdentityProviderConfigMap;
import java.net.URI;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;

public class OpenIdFedMetadataResolver {

    private static final int VALIDITY_SECONDS = 3600;

    public String resolveRpMetadata(OpenIdFedIdentityProviderConfig config, HttpServletRequest request)
        throws JOSEException, ParseException {
        //TODO extract baseUrl from request
        String baseUrl = "";
        EntityStatement statement = generateRpMetadata(config, baseUrl);
        return statement.getSignedStatement().serialize();
    }

    public EntityStatement generateRpMetadata(OpenIdFedIdentityProviderConfig config, String baseUrl)
        throws JOSEException, ParseException {
        OpenIdFedIdentityProviderConfigMap map = config.getConfigMap();

        // The required entity statement parameters
        EntityID iss = new EntityID(map.getClientId());
        EntityID sub = new EntityID(iss.getValue());

        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(VALIDITY_SECONDS);

        JWK federationKey = config.getFederationJWK();
        JWK clientKey = config.getClientJWK();
        JWKSet federationJwks = new JWKSet(federationKey);
        JWKSet clientJwks = new JWKSet(clientKey);

        List<EntityID> authorities = map
            .getAuthorityHints()
            .stream()
            .map(a -> new EntityID(a))
            .collect(Collectors.toList());

        EntityStatementClaimsSet claims = new EntityStatementClaimsSet(
            iss,
            sub,
            Date.from(issuedAt),
            Date.from(expiresAt),
            federationJwks.toPublicJWKSet()
        );

        claims.setAuthorityHints(authorities);

        // The relying party metadata
        OIDCClientMetadata metadata = new OIDCClientMetadata();
        metadata.setName(map.getClientName());
        metadata.setEmailContacts(map.getContacts());
        metadata.setClientRegistrationTypes(Collections.singletonList(config.getClientRegistrationType()));
        metadata.setApplicationType(config.getApplicationType());
        metadata.setGrantTypes(new HashSet<>(config.getGrantTypes()));
        metadata.setResponseTypes(new HashSet<>(config.getResponseTypes()));

        //TODO build
        metadata.setRedirectionURI(URI.create("http://localhost"));
        metadata.setTokenEndpointAuthMethod(config.getClientAuthenticationMethod());

        metadata.setJWKSet(clientJwks.toPublicJWKSet());
        metadata.setSubjectType(config.getSubjectType());
        claims.setRPMetadata(metadata);

        // Sign the entity statement
        return EntityStatement.sign(claims, federationKey);
    }

    /**
     * Federation entity operations
     */

    public void fetchEntityConfiguration(String issuer) {}

    public void fetchEntityStatement(String entity, String issuer, String subject) {}

    public void resolveEntityStatement(String entity, String anchor, String subject) {}

    /**
     * Trust mark operations
     */

    public void requestTrustMarkStatus(String entity, String id, String subject) {}

    public void requestTrustMarkListing(String entity, String id) {}

    /**
     * Trust anchor operations
     */
    public void resolveTAMetadata(String identifier) {}

    /**
     * OpenId provider operations
     */
    public void resolveOPMetadata(String identifier) {}
}
