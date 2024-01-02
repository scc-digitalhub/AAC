package it.smartcommunitylab.aac.openidfed.resolvers;

import com.nimbusds.openid.connect.sdk.federation.trust.ResolveException;
import com.nimbusds.openid.connect.sdk.federation.trust.TrustChain;

/**
 * Trust chain ops
 */
public interface TrustChainResolver {
    TrustChain resolveTrustChain(String trustAnchor, String entityId) throws ResolveException;
}
