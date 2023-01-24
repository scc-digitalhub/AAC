package it.smartcommunitylab.aac.profiles.provider;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

import org.springframework.util.Assert;

import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.profiles.claims.AbstractProfileClaim;
import it.smartcommunitylab.aac.profiles.model.AbstractProfile;
import it.smartcommunitylab.aac.profiles.scope.ProfileResource;

public abstract class AbstractProfileClaimsExtractor<C extends AbstractProfileClaim<P>, P extends AbstractProfile> {

    protected final ProfileResource resource;

    public AbstractProfileClaimsExtractor(ProfileResource resource) {
        Assert.notNull(resource, "resource can not be null");
        this.resource = resource;
    }

    public abstract Collection<C> extractProfileClaims(User user, ClientDetails client,
            Collection<String> scopes,
            Map<String, Serializable> extensions);
}
