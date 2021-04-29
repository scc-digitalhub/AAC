package it.smartcommunitylab.aac.profiles.scope;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.model.ScopeType;
import it.smartcommunitylab.aac.profiles.model.ProfileClaimsSet;
import it.smartcommunitylab.aac.scope.Scope;

public class OpenIdEmailScope extends AbstractProfileScope {

    @Override
    public String getScope() {
        return Config.SCOPE_EMAIL;
    }

    // TODO replace with keys for i18n
    @Override
    public String getName() {
        return "Read user's email";
    }

    @Override
    public String getDescription() {
        return "Basic user's email";
    }

}
