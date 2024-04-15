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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityType;
import com.nimbusds.openid.connect.sdk.federation.entities.FederationEntityMetadata;
import com.nimbusds.openid.connect.sdk.federation.trust.ResolveException;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import it.smartcommunitylab.aac.openidfed.resolvers.CachingEntityStatementResolver;
import it.smartcommunitylab.aac.openidfed.resolvers.DefaultFederationEntityResolver;
import it.smartcommunitylab.aac.openidfed.resolvers.DefaultOpenIdProviderResolver;
import it.smartcommunitylab.aac.openidfed.resolvers.EntityStatementResolver;
import it.smartcommunitylab.aac.openidfed.resolvers.FederationEntityResolver;
import it.smartcommunitylab.aac.openidfed.resolvers.OpenIdProviderResolver;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

public class ListingOpenIdProviderDiscoveryService implements OpenIdProviderDiscoveryService {

    private static final EntityStatementResolver DEFAULT_ENTITY_RESOLVER = new CachingEntityStatementResolver();

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final String trustAnchor;
    private final OpenIdProviderResolver providerResolver;
    private final FederationEntityResolver federationResolver;

    //TODO add tree walk over intermediaries to discover providers not listed at top level
    //keep a short loading cache for list responses for a given entity
    private final LoadingCache<String, List<String>> entities = CacheBuilder.newBuilder()
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build(
            new CacheLoader<String, List<String>>() {
                @Override
                public List<String> load(String key) throws Exception {
                    return federationResolver
                        .listFederationEntities(trustAnchor, key, EntityType.OPENID_PROVIDER)
                        .stream()
                        .map(e -> e.getValue())
                        .collect(Collectors.toList());
                }
            }
        );

    public ListingOpenIdProviderDiscoveryService(String trustAnchor) {
        this(
            trustAnchor,
            //by default use a caching entity resolver to keep valid statements until *exp*
            DEFAULT_ENTITY_RESOLVER
        );
    }

    public ListingOpenIdProviderDiscoveryService(String trustAnchor, EntityStatementResolver entityStatementResolver) {
        this(
            trustAnchor,
            new DefaultOpenIdProviderResolver(entityStatementResolver),
            new DefaultFederationEntityResolver(entityStatementResolver)
        );
    }

    public ListingOpenIdProviderDiscoveryService(
        String trustAnchor,
        OpenIdProviderResolver providerResolver,
        FederationEntityResolver federationResolver
    ) {
        Assert.hasText(trustAnchor, "trustAnchor is required for discovery via listing");
        Assert.notNull(providerResolver, "openid provider resolver is required");
        Assert.notNull(federationResolver, "federation provider resolver is required");

        this.trustAnchor = trustAnchor;
        this.providerResolver = providerResolver;
        this.federationResolver = federationResolver;
    }

    @Override
    public List<String> discoverProviders() {
        try {
            //TODO add tree walk over intermediaries to discover providers not listed at top level
            // for now we list only from top level
            return entities.get(trustAnchor);
        } catch (ExecutionException e) {
            logger.error("error reading providers", e);
            return Collections.emptyList();
        }
    }

    @Override
    public @Nullable OIDCProviderMetadata findProvider(String entityId) {
        try {
            //first check if provider is discovered
            if (!entities.get(trustAnchor).contains(entityId)) {
                // return null;
            }

            //fetch via provider
            return providerResolver.resolveOpenIdProvider(trustAnchor, entityId);
        } catch (ExecutionException e) {
            logger.error("error reading providers", e);
        } catch (ResolveException e) {
            logger.error("error resolving provider {}:{}", String.valueOf(entityId), e.getMessage());
        }

        return null;
    }

    @Override
    public @Nullable FederationEntityMetadata loadProviderMetadata(String entityId) {
        try {
            //first check if provider is discovered
            if (!entities.get(trustAnchor).contains(entityId)) {
                return null;
            }

            //fetch via provider
            return federationResolver.resolveFederationEntityMetadata(trustAnchor, entityId);
        } catch (ExecutionException e) {
            logger.error("error reading providers", e);
        } catch (ResolveException e) {
            logger.error("error resolving metadata {}:{}", String.valueOf(entityId), e.getMessage());
        }

        return null;
    }
}
