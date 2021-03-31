package it.smartcommunitylab.aac.claims;

import java.util.Collection;

import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.core.UserDetails;
import it.smartcommunitylab.aac.model.User;

/*
 * Claim extractor is the interface service need to expose to be able to produce claims.
 * Note that we expect implementations to always return a valid set (or null) and not perform authorization decisions here.
 * 
 * When asked, either return the claimSet or null.
 * We also need extractors to identify themselves via resourceId.
 * 
 * Note that extractors will be given the whole list of scopes requested for building their response.
 * 
 * This interface is separated from scopeExtractor to address resources wanting to produce claims when included as audience
 */

public interface ResourceClaimsExtractor {

    public String getResourceId();

    public ClaimsSet extractUserClaims(User user, ClientDetails client, Collection<String> scopes)
            throws InvalidDefinitionException, SystemException;

    public ClaimsSet extractClientClaims(ClientDetails client, Collection<String> scopes)
            throws InvalidDefinitionException, SystemException;

}
