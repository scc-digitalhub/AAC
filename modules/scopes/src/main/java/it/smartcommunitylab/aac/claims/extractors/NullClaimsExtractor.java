package it.smartcommunitylab.aac.claims.extractors;

import it.smartcommunitylab.aac.claims.base.AbstractClaim;
import it.smartcommunitylab.aac.claims.model.ClaimsExtractor;
import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.model.User;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

public class NullClaimsExtractor implements ClaimsExtractor<AbstractClaim> {

    @Override
    public Collection<AbstractClaim> extractUserClaims(
        User user,
        ClientDetails client,
        Collection<String> scopes,
        Map<String, Serializable> extensions
    ) {
        return null;
    }

    @Override
    public Collection<AbstractClaim> extractClientClaims(
        ClientDetails client,
        Collection<String> scopes,
        Map<String, Serializable> extensions
    ) {
        return null;
    }
}
