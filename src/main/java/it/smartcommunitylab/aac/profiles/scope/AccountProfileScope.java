package it.smartcommunitylab.aac.profiles.scope;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.model.ScopeType;
import it.smartcommunitylab.aac.profiles.model.ProfileClaimsSet;
import it.smartcommunitylab.aac.scope.Scope;

public class AccountProfileScope extends Scope {

    @Override
    public String getResourceId() {
        return ProfileClaimsSet.RESOURCE_ID + ".account";
    }

    @Override
    public ScopeType getType() {
        return ScopeType.USER;
    }

    @Override
    public String getScope() {
        return Config.SCOPE_ACCOUNT_PROFILE;
    }

    // TODO replace with keys for i18n
    @Override
    public String getName() {
        return "Read user's account profile";
    }

    @Override
    public String getDescription() {
        return "Account profile of the current platform user. Read access only.";
    }

}
