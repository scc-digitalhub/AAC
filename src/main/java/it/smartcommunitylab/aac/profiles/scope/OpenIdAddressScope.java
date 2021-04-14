package it.smartcommunitylab.aac.profiles.scope;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.model.ScopeType;
import it.smartcommunitylab.aac.profiles.model.ProfileClaimsSet;
import it.smartcommunitylab.aac.scope.Scope;

public class OpenIdAddressScope extends Scope {

    @Override
    public String getResourceId() {
        return ProfileClaimsSet.RESOURCE_ID + ".openid";
    }

    @Override
    public ScopeType getType() {
        return ScopeType.USER;
    }

    @Override
    public String getScope() {
        return Config.SCOPE_ADDRESS;
    }

    // TODO replace with keys for i18n
    @Override
    public String getName() {
        return "Read user's address";
    }

    @Override
    public String getDescription() {
        return "Basic user's address.";
    }

}
