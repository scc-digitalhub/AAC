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

import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import com.nimbusds.openid.connect.sdk.federation.trust.DefaultEntityStatementRetriever;
import com.nimbusds.openid.connect.sdk.federation.trust.EntityStatementRetriever;
import com.nimbusds.openid.connect.sdk.federation.trust.ResolveException;
import com.nimbusds.openid.connect.sdk.federation.trust.TrustChain;
import org.springframework.util.Assert;

public class DefaultEntityStatementResolver implements EntityStatementResolver {

    private final TrustChainResolver trustChainResolver;

    public DefaultEntityStatementResolver() {
        this(new DefaultTrustChainResolver());
    }

    public DefaultEntityStatementResolver(TrustChainResolver trustChainResolver) {
        Assert.notNull(trustChainResolver, "trust chain resolver is required");
        this.trustChainResolver = trustChainResolver;
    }

    /**
     * Entity ops
     */

    public EntityStatement fetchEntityStatement(String entityId) throws ResolveException {
        Assert.hasText(entityId, "entityId can not be null or empty");

        EntityID entityIdID = new EntityID(entityId);
        EntityStatementRetriever retriever = new DefaultEntityStatementRetriever();

        //fetch statement via resolver - does not validate
        EntityStatement statement = retriever.fetchEntityConfiguration(entityIdID);
        if (
            statement == null ||
            statement.getClaimsSet() == null ||
            statement.getClaimsSet().getFederationEntityMetadata() == null
        ) {
            throw new ResolveException("Invalid federation metadata from " + entityId);
        }

        return statement;
    }

    public EntityStatement resolveEntityStatement(String trustAnchor, String entityId) throws ResolveException {
        Assert.hasText(trustAnchor, "trust anchor can not be null or empty");
        Assert.hasText(entityId, "entityId can not be null or empty");

        //resolve a valid trust chain
        TrustChain chain = trustChainResolver.resolveTrustChain(trustAnchor, entityId);

        //return the statement for the leaf
        //TODO check if policy is evaluated
        //TODO check if entity signature is validated against published keys
        return chain.getLeafConfiguration();
    }
}
