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
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import com.nimbusds.openid.connect.sdk.federation.trust.ResolveException;
import it.smartcommunitylab.aac.openidfed.resolvers.EntityStatementResolver;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.springframework.util.Assert;

public class OpenIdFedMetadataRepository {

    // private static final int DEFAULT_RETRIES = 3;

    // private final String trustAnchor;
    // private int maxRetries = DEFAULT_RETRIES;
    // private EntityStatementResolver resolver;

    // //loading cache
    // private final LoadingCache<String, EntityStatement> statements = CacheBuilder
    //     .newBuilder()
    //     .expireAfterWrite(1, TimeUnit.HOURS)
    //     .maximumSize(100)
    //     .build(
    //         new CacheLoader<String, EntityStatement>() {
    //             @Override
    //             public EntityStatement load(final String id) throws Exception {
    //                 if (resolver == null) {
    //                     throw new ResolveException("invalid or missing resolver");
    //                 }

    //                 EntityStatement statement = resolver.resolveEntityStatement(trustAnchor, id);

    //                 //evaluate expiration *before usage*
    //                 Date now = Date.from(Instant.now());
    //                 if (
    //                     statement.getClaimsSet().getExpirationTime() != null &&
    //                     now.after(statement.getClaimsSet().getExpirationTime())
    //                 ) {
    //                     throw new ResolveException("invalid (expired) metadata");
    //                 }

    //                 return statement;
    //             }
    //         }
    //     );

    // public OpenIdFedMetadataRepository(String trustAnchor) {
    //     Assert.hasText(trustAnchor, "trust anchor must be set and valid");
    //     this.trustAnchor = trustAnchor;
    //     this.resolver = new OpenIdFedMetadataResolver();
    // }

    // public void setResolver(EntityStatementResolver resolver) {
    //     Assert.notNull(resolver, "resolver can not be null");
    //     this.resolver = resolver;
    // }

    // public void setMaxRetries(int maxRetries) {
    //     this.maxRetries = maxRetries;
    // }

    // public EntityStatement findByEntityId(String entityId) {
    //     return findByEntityId(entityId, 0);
    // }

    // public EntityStatement findByEntityId(String entityId, int retryCount) {
    //     //retry handling
    //     if (retryCount > maxRetries) {
    //         //max retries exceeded, exit
    //         return null;
    //     }
    //     int count = retryCount + 1;

    //     //resolve via cache
    //     try {
    //         EntityStatement statement = statements.get(entityId);
    //         if (statement == null) {
    //             return null;
    //         }

    //         //evaluate expiration
    //         Date now = Date.from(Instant.now());
    //         if (
    //             statement.getClaimsSet().getExpirationTime() != null &&
    //             now.after(statement.getClaimsSet().getExpirationTime())
    //         ) {
    //             //clean up and resolve again with retry
    //             statements.invalidate(now);
    //             return findByEntityId(entityId, count);
    //         }

    //         return statement;
    //     } catch (IllegalArgumentException | UncheckedExecutionException | ExecutionException e) {
    //         return null;
    //     }
    // }
}
