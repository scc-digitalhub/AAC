package it.smartcommunitylab.aac.profiles.scope;

import it.smartcommunitylab.aac.profiles.claims.ProfileClaimsSet;
import it.smartcommunitylab.aac.scope.base.AbstractInternalApiResource;

public class ProfileApiResource extends AbstractInternalApiResource {

    public static final String RESOURCE_ID = ProfileClaimsSet.RESOURCE_ID;

    public ProfileApiResource(String realm, String baseUrl) {
        super(realm, baseUrl, RESOURCE_ID);

        // scopes are registered via provider based on available profiles
    }

    // TODO replace with keys for i18n
    @Override
    public String getName() {
        return "User profiles";
    }

    @Override
    public String getDescription() {
        return "Access user profiles: basic, account, custom";
    }
}
