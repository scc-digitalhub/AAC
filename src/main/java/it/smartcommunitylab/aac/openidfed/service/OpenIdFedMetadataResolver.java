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
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.openid.connect.sdk.federation.api.EntityListingRequest;
import com.nimbusds.openid.connect.sdk.federation.api.EntityListingResponse;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatementClaimsSet;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityType;
import com.nimbusds.openid.connect.sdk.federation.entities.FederationEntityMetadata;
import com.nimbusds.openid.connect.sdk.federation.registration.ClientRegistrationType;
import com.nimbusds.openid.connect.sdk.federation.trust.DefaultEntityStatementRetriever;
import com.nimbusds.openid.connect.sdk.federation.trust.EntityStatementRetriever;
import com.nimbusds.openid.connect.sdk.federation.trust.ResolveException;
import com.nimbusds.openid.connect.sdk.federation.trust.TrustChain;
import com.nimbusds.openid.connect.sdk.federation.trust.TrustChainSet;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import com.nimbusds.openid.connect.sdk.rp.ApplicationType;
import com.nimbusds.openid.connect.sdk.rp.OIDCClientMetadata;
import it.smartcommunitylab.aac.openidfed.provider.OpenIdFedIdentityProviderConfig;
import it.smartcommunitylab.aac.openidfed.provider.OpenIdFedIdentityProviderConfigMap;
import it.smartcommunitylab.aac.openidfed.resolvers.EntityStatementResolver;
import it.smartcommunitylab.aac.openidfed.resolvers.OpenIdProviderResolver;
import it.smartcommunitylab.aac.openidfed.resolvers.TrustChainResolver;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

public class OpenIdFedMetadataResolver {

    // implements TrustChainResolver, EntityStatementResolver, TrustAnchorResolver, OpenIdProviderResolver {

    private static final int VALIDITY_SECONDS = 3600;
    // public String resolveRpMetadata(OpenIdFedIdentityProviderConfig config, HttpServletRequest request)
    //     throws JOSEException, ParseException {
    //     //TODO extract baseUrl from request
    //     String baseUrl = "";
    //     EntityStatement statement = generateRpMetadata(config, baseUrl);
    //     return statement.getSignedStatement().serialize();
    // }

    // public EntityStatement generateRpMetadata(OpenIdFedIdentityProviderConfig config, String baseUrl)
    //     throws JOSEException, ParseException {
    //     OpenIdFedIdentityProviderConfigMap map = config.getConfigMap();

    //     // The required entity statement parameters
    //     EntityID iss = new EntityID(map.getClientId());
    //     EntityID sub = new EntityID(iss.getValue());

    //     Instant issuedAt = Instant.now();
    //     Instant expiresAt = issuedAt.plusSeconds(VALIDITY_SECONDS);

    //     JWK federationKey = config.getFederationJWK();
    //     JWK clientKey = config.getClientJWK();
    //     JWKSet federationJwks = new JWKSet(federationKey);
    //     JWKSet clientJwks = new JWKSet(clientKey);

    //     List<EntityID> authorities = map
    //         .getAuthorityHints()
    //         .stream()
    //         .map(a -> new EntityID(a))
    //         .collect(Collectors.toList());

    //     EntityStatementClaimsSet claims = new EntityStatementClaimsSet(
    //         iss,
    //         sub,
    //         Date.from(issuedAt),
    //         Date.from(expiresAt),
    //         federationJwks.toPublicJWKSet()
    //     );

    //     claims.setAuthorityHints(authorities);

    //     // The relying party metadata
    //     OIDCClientMetadata metadata = new OIDCClientMetadata();
    //     metadata.setName(map.getClientName());
    //     metadata.setEmailContacts(map.getContacts());
    //     metadata.setClientRegistrationTypes(Collections.singletonList(config.getClientRegistrationType()));
    //     metadata.setApplicationType(config.getApplicationType());
    //     metadata.setGrantTypes(new HashSet<>(config.getGrantTypes()));
    //     metadata.setResponseTypes(new HashSet<>(config.getResponseTypes()));

    //     //TODO build
    //     metadata.setRedirectionURI(URI.create("http://localhost"));
    //     metadata.setTokenEndpointAuthMethod(config.getClientAuthenticationMethod());

    //     metadata.setJWKSet(clientJwks.toPublicJWKSet());
    //     metadata.setSubjectType(config.getSubjectType());
    //     claims.setRPMetadata(metadata);

    //     // Sign the entity statement
    //     return EntityStatement.sign(claims, federationKey);
    // }

    // /**
    //  * Trust mark operations
    //  */

    // public void requestTrustMarkStatus(String entity, String id, String subject) {}

    // public void requestTrustMarkListing(String entity, String id) {}

    // /**
    //  * Trust anchor operations
    //  */
    // @Override
    // public FederationEntityMetadata resolveTrustAnchor(String trustAnchor) throws ResolveException {
    //     //fetch trust anchor statement about itself to extract listing endpoint
    //     //no validation needed, this is the trust anchor
    //     EntityStatement statement = fetchEntityStatement(trustAnchor);
    //     if (
    //         statement == null ||
    //         statement.getClaimsSet() == null ||
    //         statement.getClaimsSet().getFederationEntityMetadata() == null
    //     ) {
    //         throw new ResolveException("Invalid federation metadata from " + trustAnchor);
    //     }

    //     return statement.getClaimsSet().getFederationEntityMetadata();
    // }

    // /**
    //  * OpenId provider operations
    //  */
    // public OIDCProviderMetadata resolveOpenIdProvider(String trustAnchor, String entityId) throws ResolveException {
    //     //resolve the metadata by building a trust chain
    //     EntityStatement statement = resolveEntityStatement(trustAnchor, entityId);
    //     if (statement == null || statement.getClaimsSet() == null || statement.getClaimsSet().getOPMetadata() == null) {
    //         throw new ResolveException("Invalid op metadata from " + entityId);
    //     }

    //     return statement.getClaimsSet().getOPMetadata();
    // }

    // public List<EntityID> listOpenIdProviders(String trustAnchor, String entityId) throws ResolveException {
    //     return listEntitiesFromEntity(trustAnchor, entityId, EntityType.OPENID_PROVIDER);
    // }

    // /**
    //  * Entity ops
    //  */

    // public EntityStatement fetchEntityStatement(String entityId) throws ResolveException {
    //     Assert.hasText(entityId, "entityId can not be null or empty");

    //     EntityID entityIdID = new EntityID(entityId);
    //     EntityStatementRetriever retriever = new DefaultEntityStatementRetriever();

    //     //fetch statement via resolver - does not validate
    //     EntityStatement statement = retriever.fetchEntityConfiguration(entityIdID);
    //     if (
    //         statement == null ||
    //         statement.getClaimsSet() == null ||
    //         statement.getClaimsSet().getFederationEntityMetadata() == null
    //     ) {
    //         throw new ResolveException("Invalid federation metadata from " + entityId);
    //     }

    //     return statement;
    // }

    // public EntityStatement resolveEntityStatement(String trustAnchor, String entityId) throws ResolveException {
    //     Assert.hasText(trustAnchor, "trust anchor can not be null or empty");
    //     Assert.hasText(entityId, "entityId can not be null or empty");

    //     //resolve a valid trust chain
    //     TrustChain chain = resolveTrustChain(trustAnchor, entityId);

    //     //return the statement for the leaf
    //     return chain.getLeafConfiguration();
    // }

    // public List<EntityID> listEntitiesFromEntity(String trustAnchor, String entityId, @Nullable EntityType entityType)
    //     throws ResolveException {
    //     //resolve metadata to extract listing endpoint
    //     EntityStatement statement = resolveEntityStatement(trustAnchor, entityId);
    //     if (
    //         statement == null ||
    //         statement.getClaimsSet() == null ||
    //         statement.getClaimsSet().getFederationEntityMetadata() == null
    //     ) {
    //         throw new ResolveException("Invalid federation metadata from " + trustAnchor);
    //     }

    //     URI listEndpoint = statement.getClaimsSet().getFederationEntityMetadata().getFederationListEndpointURI();
    //     if (listEndpoint == null) {
    //         throw new ResolveException("Missing list endpoint for " + trustAnchor);
    //     }

    //     //make list request and collect results
    //     EntityListingRequest request = new EntityListingRequest(listEndpoint);
    //     if (entityType != null) {
    //         request = new EntityListingRequest(listEndpoint, entityType);
    //     }

    //     HTTPRequest httpRequest = request.toHTTPRequest();
    //     HTTPResponse httpResponse;
    //     try {
    //         httpResponse = httpRequest.send();
    //     } catch (IOException e) {
    //         throw new ResolveException(
    //             "Couldn't retrieve entity configuration for " + httpRequest.getURL() + ": " + e.getMessage(),
    //             e
    //         );
    //     }

    //     try {
    //         EntityListingResponse listing = EntityListingResponse.parse(httpResponse);
    //         if (!listing.indicatesSuccess()) {
    //             throw new ResolveException("Listing entities from " + httpRequest.getURL() + "failed");
    //         }

    //         return listing.toSuccessResponse().getEntityListing();
    //     } catch (ParseException e) {
    //         throw new ResolveException(
    //             "Couldn't parse listing response for " + httpRequest.getURL() + ": " + e.getMessage(),
    //             e
    //         );
    //     }
    // }

    // /**
    //  * Trust chain ops
    //  */
    // @Override
    // public TrustChain resolveTrustChain(String trustAnchor, String entityId) throws ResolveException {
    //     Assert.hasText(trustAnchor, "trust anchor can not be null or empty");
    //     Assert.hasText(entityId, "entityId can not be null or empty");

    //     //check if we can resolve a trust chain from entityId up to trust anchor
    //     EntityID trustAnchorID = new EntityID(trustAnchor);
    //     EntityID entityIdID = new EntityID(entityId);

    //     com.nimbusds.openid.connect.sdk.federation.trust.TrustChainResolver resolver =
    //         new com.nimbusds.openid.connect.sdk.federation.trust.TrustChainResolver(trustAnchorID);
    //     try {
    //         TrustChainSet resolvedChains = resolver.resolveTrustChains(entityIdID);
    //         //get the shortest
    //         TrustChain chain = resolvedChains.getShortest();
    //         if (chain == null) {
    //             throw new ResolveException(
    //                 "Could not resolve a valid trust chain from " + entityId + " to " + trustAnchor
    //             );
    //         }

    //         return chain;
    //     } catch (ResolveException e) {
    //         throw new ResolveException("Could not resolve a valid trust chain from " + entityId + " to " + trustAnchor);
    //     }
    // }
}
