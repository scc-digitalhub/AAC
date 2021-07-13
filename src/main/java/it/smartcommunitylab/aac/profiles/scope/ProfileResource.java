package it.smartcommunitylab.aac.profiles.scope;

import it.smartcommunitylab.aac.profiles.claims.ProfileClaimsSet;
import it.smartcommunitylab.aac.scope.Resource;

public class ProfileResource extends Resource {
    @Override
    public String getResourceId() {
        return ProfileClaimsSet.RESOURCE_ID;
    }

    // TODO replace with keys for i18n
    @Override
    public String getName() {
        return "User profile";
    }

    @Override
    public String getDescription() {
        return "Access user profile: basic, account, custom";
    }
}
