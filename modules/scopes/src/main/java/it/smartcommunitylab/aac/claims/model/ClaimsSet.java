package it.smartcommunitylab.aac.claims.model;

import it.smartcommunitylab.aac.core.model.Resource;
import java.util.Collection;

/*
 * A claims set describing an entity
 */
public interface ClaimsSet extends Resource {
    // unique identifier
    public String getClaimsSetId();

    // a claimset is produced by a resource
    // (indicator as per RFC8707)
    public String getResource();

    // subject identifier
    public String getSubjectId();

    // a claim set can be namespaced. When provided it will be prepended to keys
    public String getNamespace();

    // the claim set.
    // each claim should be translated to a single value.
    // Multiple claims under the same key will be merged into a collection
    public Collection<Claim> getClaims();
}
