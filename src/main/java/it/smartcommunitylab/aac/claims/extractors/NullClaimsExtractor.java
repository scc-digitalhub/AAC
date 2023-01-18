package it.smartcommunitylab.aac.claims.extractors;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

import it.smartcommunitylab.aac.claims.base.AbstractClaim;
import it.smartcommunitylab.aac.claims.base.AbstractClaimsExtractor;
import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.scope.base.AbstractApiResource;

public class NullClaimsExtractor<R extends AbstractApiResource<?>>
        extends AbstractClaimsExtractor {

    public NullClaimsExtractor(String authority, String provider, String realm, String resource) {
        super(authority, provider, realm, resource);

    }

    @Override
    protected Collection<AbstractClaim> extractUserClaims(User user, ClientDetails client, Collection<String> scopes,
            Map<String, Serializable> extensions) {
        return null;
    }

    @Override
    protected Collection<AbstractClaim> extractClientClaims(ClientDetails client, Collection<String> scopes,
            Map<String, Serializable> extensions) {
        return null;
    }

}
