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

import com.nimbusds.openid.connect.sdk.federation.entities.FederationEntityMetadata;
import com.nimbusds.openid.connect.sdk.federation.trust.ResolveException;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import it.smartcommunitylab.aac.openidfed.resolvers.CachingEntityStatementResolver;
import it.smartcommunitylab.aac.openidfed.resolvers.DefaultFederationEntityResolver;
import it.smartcommunitylab.aac.openidfed.resolvers.DefaultOpenIdProviderResolver;
import it.smartcommunitylab.aac.openidfed.resolvers.EntityStatementResolver;
import it.smartcommunitylab.aac.openidfed.resolvers.FederationEntityResolver;
import it.smartcommunitylab.aac.openidfed.resolvers.OpenIdProviderResolver;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

public class StaticOpenIdProviderDiscoveryService implements OpenIdProviderDiscoveryService {

    private static final EntityStatementResolver DEFAULT_ENTITY_RESOLVER = new CachingEntityStatementResolver();

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final String trustAnchor;
    private final Set<String> entities;

    private final OpenIdProviderResolver providerResolver;
    private final FederationEntityResolver federationResolver;

    public StaticOpenIdProviderDiscoveryService(String trustAnchor, Collection<String> entities) {
        this(
            trustAnchor,
            entities,
            //by default use a caching entity resolver to keep valid statements until *exp*
            DEFAULT_ENTITY_RESOLVER
        );
    }

    public StaticOpenIdProviderDiscoveryService(
        String trustAnchor,
        Collection<String> entities,
        EntityStatementResolver entityStatementResolver
    ) {
        this(
            trustAnchor,
            entities,
            new DefaultOpenIdProviderResolver(entityStatementResolver),
            new DefaultFederationEntityResolver(entityStatementResolver)
        );
    }

    public StaticOpenIdProviderDiscoveryService(
        String trustAnchor,
        Collection<String> entities,
        OpenIdProviderResolver providerResolver,
        FederationEntityResolver federationResolver
    ) {
        Assert.hasText(trustAnchor, "trustAnchor is required for discovery via listing");
        Assert.notNull(entities, "provider entities must be valid");
        Assert.notNull(providerResolver, "openid provider resolver is required");
        Assert.notNull(federationResolver, "federation provider resolver is required");

        this.trustAnchor = trustAnchor;
        this.entities = Collections.unmodifiableSortedSet(new TreeSet<>(entities));
        this.providerResolver = providerResolver;
        this.federationResolver = federationResolver;
    }

    @Override
    public Collection<String> discoverProviders() {
        return entities;
    }

    @Override
    public @Nullable OIDCProviderMetadata findProvider(String entityId) {
        try {
            //first check if provider is in list
            if (!entities.contains(entityId)) {
                return null;
            }

            //fetch via provider
            return providerResolver.resolveOpenIdProvider(trustAnchor, entityId);
        } catch (ResolveException e) {
            logger.error("error resolving provider {}:{}", String.valueOf(entityId), e.getMessage());
            return null;
        }
    }

    @Override
    public @Nullable FederationEntityMetadata loadProviderMetadata(String entityId) {
        try {
            //first check if provider is discovered
            if (!entities.contains(entityId)) {
                return null;
            }

            //fetch via provider
            return federationResolver.resolveFederationEntityMetadata(trustAnchor, entityId);
        } catch (ResolveException e) {
            logger.error("error resolving metadata {}:{}", String.valueOf(entityId), e.getMessage());
            return null;
        }
    }
}
