package it.smartcommunitylab.aac.profiles.scope;

import it.smartcommunitylab.aac.model.ScopeType;
import it.smartcommunitylab.aac.profiles.claims.ProfileClaimsSet;
import it.smartcommunitylab.aac.scope.Scope;

public abstract class AbstractProfileScope extends Scope {

    @Override
    public String getResourceId() {
        return ProfileClaimsSet.RESOURCE_ID;
    }

    @Override
    public ScopeType getType() {
        return ScopeType.USER;
    }
}
