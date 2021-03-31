package it.smartcommunitylab.aac.profiles.scope;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import it.smartcommunitylab.aac.profiles.model.ProfileClaimsSet;
import it.smartcommunitylab.aac.scope.Scope;
import it.smartcommunitylab.aac.scope.ScopeProvider;

/*
 * A simple scope provider which return a single scope as defined by subclasses
 */
public abstract class ProfileScopeProvider implements ScopeProvider {

    @Override
    public String getResourceId() {
        return ProfileClaimsSet.RESOURCE_ID;
    }

    @Override
    public Collection<Scope> getScopes() {
        return Collections.singleton(buildScope(getScope()));
    }

    protected Scope buildScope(String scope) {
        Scope s = new Scope(scope);
        s.setResourceId(getResourceId());
        s.setName(getName());
        s.setDescription(getDescription());
        s.setType(getType());

        return s;
    }

    protected String getType() {
        return Scope.TYPE_USER;
    }

    protected String getDescription() {
        return null;
    }

    protected String getName() {
        return null;
    }

    protected abstract String getScope();
}
