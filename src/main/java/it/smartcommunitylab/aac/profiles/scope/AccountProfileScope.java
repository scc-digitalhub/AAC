package it.smartcommunitylab.aac.profiles.scope;

import it.smartcommunitylab.aac.Config;

public class AccountProfileScope extends AbstractProfileScope {

    public static final String SCOPE = Config.SCOPE_ACCOUNT_PROFILE;

    @Override
    public String getScope() {
        return SCOPE;
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
