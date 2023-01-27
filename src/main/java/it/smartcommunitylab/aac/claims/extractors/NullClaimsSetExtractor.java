package it.smartcommunitylab.aac.claims.extractors;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

import it.smartcommunitylab.aac.claims.base.AbstractClaimsSetExtractor;
import it.smartcommunitylab.aac.claims.base.DefaultClientClaimsSet;
import it.smartcommunitylab.aac.claims.base.DefaultUserClaimsSet;
import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.model.User;

public class NullClaimsSetExtractor extends AbstractClaimsSetExtractor {

    public NullClaimsSetExtractor(String authority, String provider, String realm, String resource) {
        super(authority, provider, realm, resource);

    }

    @Override
    public DefaultUserClaimsSet extract(User user, ClientDetails client, Collection<String> scopes,
            Map<String, Serializable> extensions) {
        return null;
    }

    @Override
    public DefaultClientClaimsSet extract(ClientDetails client, Collection<String> scopes,
            Map<String, Serializable> extensions) {
        return null;
    }

}
