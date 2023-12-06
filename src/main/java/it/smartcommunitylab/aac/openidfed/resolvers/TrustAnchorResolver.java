package it.smartcommunitylab.aac.openidfed.resolvers;

import com.nimbusds.openid.connect.sdk.federation.entities.FederationEntityMetadata;
import com.nimbusds.openid.connect.sdk.federation.trust.ResolveException;

/**
 * Trust anchor operations
 */
public interface TrustAnchorResolver {
    FederationEntityMetadata resolveTrustAnchor(String trustAnchor) throws ResolveException;
}
