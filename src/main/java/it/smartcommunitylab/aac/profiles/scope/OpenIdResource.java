package it.smartcommunitylab.aac.profiles.scope;

import it.smartcommunitylab.aac.profiles.claims.ProfileClaimsSet;
import it.smartcommunitylab.aac.scope.Resource;

public class OpenIdResource extends Resource {
    @Override
    public String getResourceId() {
        return ProfileClaimsSet.RESOURCE_ID + ".openid";
    }

    // TODO replace with keys for i18n
    @Override
    public String getName() {
        return "OpenId profile";
    }

    @Override
    public String getDescription() {
        return "Access user openid profile: basic, email, address, phone";
    }
}
