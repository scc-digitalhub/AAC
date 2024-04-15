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

package it.smartcommunitylab.aac.openidfed.resolvers;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import com.nimbusds.openid.connect.sdk.federation.trust.ResolveException;
import com.nimbusds.openid.connect.sdk.federation.trust.TrustChain;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.springframework.data.util.Pair;

public class CachingEntityStatementResolver extends DefaultEntityStatementResolver {

    private static final int DEFAULT_RETRIES = 3;

    private int maxRetries = DEFAULT_RETRIES;

    //loading cache only for resolved
    private final LoadingCache<Pair<String, String>, TrustChain> chains = CacheBuilder.newBuilder()
        .expireAfterWrite(1, TimeUnit.HOURS)
        .maximumSize(100)
        .build(
            new CacheLoader<Pair<String, String>, TrustChain>() {
                @Override
                public TrustChain load(final Pair<String, String> id) throws Exception {
                    if (trustChainResolver == null) {
                        throw new ResolveException("invalid or missing resolver");
                    }

                    //resolve a valid trust chain
                    TrustChain chain = trustChainResolver.resolveTrustChain(id.getFirst(), id.getSecond());
                    EntityStatement statement = chain.getLeafConfiguration();

                    //evaluate expiration *before usage*
                    Date now = Date.from(Instant.now());
                    if (
                        statement.getClaimsSet().getExpirationTime() != null &&
                        now.after(statement.getClaimsSet().getExpirationTime())
                    ) {
                        throw new ResolveException("invalid (expired) metadata");
                    }

                    return chain;
                }
            }
        );

    public CachingEntityStatementResolver() {
        this(new DefaultTrustChainResolver());
    }

    public CachingEntityStatementResolver(TrustChainResolver trustChainResolver) {
        super(trustChainResolver);
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    /**
     * Entity ops
     */

    @Override
    public EntityStatement resolveEntityStatement(String trustAnchor, String entityId) throws ResolveException {
        TrustChain chain = resolvedTrustChain(trustAnchor, entityId);
        if (chain == null) {
            return null;
        }

        //return the statement for the leaf
        //TODO check if policy is evaluated
        //TODO check if entity signature is validated against published keys
        return chain.getLeafConfiguration();
    }

    public TrustChain resolvedTrustChain(String trustAnchor, String entityId) {
        Pair<String, String> id = Pair.of(trustAnchor, entityId);
        return find(id, 0);
    }

    /*
     * Cache trust chains for resolved entities
     */

    private TrustChain find(Pair<String, String> id, int retryCount) {
        //retry handling
        if (retryCount > maxRetries) {
            //max retries exceeded, exit
            return null;
        }
        int count = retryCount + 1;

        //resolve via cache
        try {
            TrustChain chain = chains.get(id);
            EntityStatement statement = chain.getLeafConfiguration();
            if (statement == null) {
                return null;
            }

            //evaluate expiration
            Date now = Date.from(Instant.now());
            if (
                statement.getClaimsSet().getExpirationTime() != null &&
                now.after(statement.getClaimsSet().getExpirationTime())
            ) {
                //clean up and resolve again with retry
                chains.invalidate(id);
                return find(id, count);
            }

            return chain;
        } catch (IllegalArgumentException | UncheckedExecutionException | ExecutionException e) {
            return null;
        }
    }
}
