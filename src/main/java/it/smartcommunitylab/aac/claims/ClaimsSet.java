package it.smartcommunitylab.aac.claims;

import java.io.Serializable;
import java.util.Map;

/*
 * A claims set describing an entity
 */
public interface ClaimsSet {

    // a claimset is produced by a resource
    public String getResourceId();

    // a claim set is generated in response to a scope
    public String getScope();

    // the set can describe the client or the user, or none
    public boolean isUser();

    public boolean isClient();

    // a claim set can be namespaced. When empty claims will be merged top level
    public String getNamespace();

    // the claim set
    public Map<String, Serializable> getClaims();

}
