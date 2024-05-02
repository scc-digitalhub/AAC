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

import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import com.nimbusds.openid.connect.sdk.federation.trust.ResolveException;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import org.springframework.util.Assert;

public class DefaultOpenIdProviderResolver implements OpenIdProviderResolver {

    private final EntityStatementResolver entityStatementResolver;

    public DefaultOpenIdProviderResolver() {
        this(new DefaultEntityStatementResolver());
    }

    public DefaultOpenIdProviderResolver(EntityStatementResolver entityStatementResolver) {
        Assert.notNull(entityStatementResolver, "entity statement resolver is required");
        this.entityStatementResolver = entityStatementResolver;
    }

    @Override
    public OIDCProviderMetadata resolveOpenIdProvider(String trustAnchor, String entityId) throws ResolveException {
        //resolve the metadata by building a trust chain
        EntityStatement statement = entityStatementResolver.resolveEntityStatement(trustAnchor, entityId);
        if (statement == null || statement.getClaimsSet() == null || statement.getClaimsSet().getOPMetadata() == null) {
            throw new ResolveException("Invalid op metadata from " + entityId);
        }

        return statement.getClaimsSet().getOPMetadata();
    }
    // @Override
    // public List<EntityID> listOpenIdProviders(String trustAnchor, String entityId) throws ResolveException {
    //     return listEntitiesFromEntity(trustAnchor, entityId, EntityType.OPENID_PROVIDER);
    // }
}
