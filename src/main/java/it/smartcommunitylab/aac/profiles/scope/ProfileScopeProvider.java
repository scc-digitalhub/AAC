package it.smartcommunitylab.aac.profiles.scope;

import it.smartcommunitylab.aac.profiles.model.ProfileClaimsSet;
import it.smartcommunitylab.aac.scope.ScopeApprover;
import it.smartcommunitylab.aac.scope.ScopeProvider;

/*
 * A simple scope provider which return profile scopes
 */
public abstract class ProfileScopeProvider implements ScopeProvider {

    @Override
    public String getResourceId() {
        return ProfileClaimsSet.RESOURCE_ID;
    }

}
