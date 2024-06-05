package it.smartcommunitylab.aac.claims.provider;

import it.smartcommunitylab.aac.claims.model.ClaimsSet;
import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.core.provider.ResourceProvider;
import it.smartcommunitylab.aac.model.User;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

public interface ClaimsSetExtractor<C extends ClaimsSet> extends ResourceProvider<C> {
    // TODO replace user and client with UserContext, ClientContext
    // also collapse into a single method, where userContext is optional
    public C extract(User user, ClientDetails client, Collection<String> scopes, Map<String, Serializable> extensions);

    public C extract(ClientDetails client, Collection<String> scopes, Map<String, Serializable> extensions);
}
