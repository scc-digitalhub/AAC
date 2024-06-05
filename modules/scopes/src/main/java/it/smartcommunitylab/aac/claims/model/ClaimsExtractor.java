package it.smartcommunitylab.aac.claims.model;

import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.model.User;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

public interface ClaimsExtractor<C extends Claim> {
    // TODO replace user and client with UserContext, ClientContext
    // also collapse into a single method, where userContext is optional
    Collection<C> extractUserClaims(
        User user,
        ClientDetails client,
        Collection<String> scopes,
        Map<String, Serializable> extensions
    );

    Collection<C> extractClientClaims(
        ClientDetails client,
        Collection<String> scopes,
        Map<String, Serializable> extensions
    );
}
