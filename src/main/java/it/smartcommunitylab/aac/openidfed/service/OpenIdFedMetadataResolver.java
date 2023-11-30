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
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.oauth2.sdk.GrantType;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.auth.ClientAuthenticationMethod;
import com.nimbusds.openid.connect.sdk.SubjectType;
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
import org.springframework.stereotype.Service;

@Service
public class OpenIdFedMetadataResolver {

    private static final int VALIDITY_SECONDS = 3600;

    public String resolve(OpenIdFedIdentityProviderConfig config) throws JOSEException, ParseException {
        EntityStatement statement = generate(config);
        return statement.getSignedStatement().serialize();
    }

    public EntityStatement generate(OpenIdFedIdentityProviderConfig config) throws JOSEException, ParseException {
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
        // rpMetadata.setEmailContacts(Collections.singletonList("ops@ligo.org"));
        metadata.setClientRegistrationTypes(Collections.singletonList(ClientRegistrationType.AUTOMATIC));
        metadata.setApplicationType(ApplicationType.WEB);
        metadata.setGrantTypes(new HashSet<>(Arrays.asList(GrantType.AUTHORIZATION_CODE, GrantType.REFRESH_TOKEN)));
        metadata.setResponseTypes(Collections.singleton(ResponseType.CODE));

        //TODO build
        metadata.setRedirectionURI(URI.create("http://localhost"));
        metadata.setTokenEndpointAuthMethod(map.getClientAuthenticationMethod());

        metadata.setJWKSet(clientJwks);
        metadata.setSubjectType(map.getSubjectType());
        claims.setRPMetadata(metadata);

        // Sign the entity statement
        return EntityStatement.sign(claims, federationKey);
    }
}
