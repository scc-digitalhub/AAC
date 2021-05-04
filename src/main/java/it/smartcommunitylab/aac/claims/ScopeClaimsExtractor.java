package it.smartcommunitylab.aac.claims;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.core.UserDetails;
import it.smartcommunitylab.aac.model.User;

/*
 * Claim extractor is the interface service need to expose to be able to produce claims in response to scopes.
 * Note that we expect implementations to always return a valid set (or null) and not perform authorization decisions here.
 * 
 * When asked for a scope, either return the claimSet or null.
 * We also need extractors to identify themselves via a combo (resourceid + scope).
 * 
 * Note that extractors will be given the whole list of scopes requested for building their response, 
 * but they need to respond to the defined scope they are invoked on.
 */

public interface ScopeClaimsExtractor {

    public String getRealm();

    public String getResourceId();

    // a list of scopes (for the declared resource) this extractor will answer to
    public Collection<String> getScopes();

    public ClaimsSet extractUserClaims(String scope, User user, ClientDetails client, Collection<String> scopes,
            Map<String, Serializable> extensions)
            throws InvalidDefinitionException, SystemException;

    public ClaimsSet extractClientClaims(String scope, ClientDetails client, Collection<String> scopes,
            Map<String, Serializable> extensions)
            throws InvalidDefinitionException, SystemException;

}
