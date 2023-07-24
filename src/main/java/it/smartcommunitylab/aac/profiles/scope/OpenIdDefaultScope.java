package it.smartcommunitylab.aac.profiles.scope;

import it.smartcommunitylab.aac.Config;

public class OpenIdDefaultScope extends AbstractProfileScope {

    public static final String SCOPE = Config.SCOPE_PROFILE;

    @Override
    public String getScope() {
        return SCOPE;
    }

    // TODO replace with keys for i18n
    @Override
    public String getName() {
        return "Read user's standard profile";
    }

    @Override
    public String getDescription() {
        return "Basic user profile data (name, surname, email). Read access only.";
    }
}
