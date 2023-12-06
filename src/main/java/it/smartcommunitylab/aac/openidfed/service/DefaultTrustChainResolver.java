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

import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import com.nimbusds.openid.connect.sdk.federation.trust.ResolveException;
import com.nimbusds.openid.connect.sdk.federation.trust.TrustChain;
import com.nimbusds.openid.connect.sdk.federation.trust.TrustChainSet;
import it.smartcommunitylab.aac.openidfed.resolvers.TrustChainResolver;
import org.springframework.util.Assert;

public class DefaultTrustChainResolver implements TrustChainResolver {

    /**
     * Trust chain ops
     */
    @Override
    public TrustChain resolveTrustChain(String trustAnchor, String entityId) throws ResolveException {
        Assert.hasText(trustAnchor, "trust anchor can not be null or empty");
        Assert.hasText(entityId, "entityId can not be null or empty");

        //check if we can resolve a trust chain from entityId up to trust anchor
        EntityID trustAnchorID = new EntityID(trustAnchor);
        EntityID entityIdID = new EntityID(entityId);

        com.nimbusds.openid.connect.sdk.federation.trust.TrustChainResolver resolver =
            new com.nimbusds.openid.connect.sdk.federation.trust.TrustChainResolver(trustAnchorID);
        try {
            TrustChainSet resolvedChains = resolver.resolveTrustChains(entityIdID);
            //get the shortest
            TrustChain chain = resolvedChains.getShortest();
            if (chain == null) {
                throw new ResolveException(
                    "Could not resolve a valid trust chain from " + entityId + " to " + trustAnchor
                );
            }

            return chain;
        } catch (ResolveException e) {
            throw new ResolveException("Could not resolve a valid trust chain from " + entityId + " to " + trustAnchor);
        }
    }
}
