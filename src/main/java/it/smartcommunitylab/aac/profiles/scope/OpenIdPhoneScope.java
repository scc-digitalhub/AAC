package it.smartcommunitylab.aac.profiles.scope;

import it.smartcommunitylab.aac.Config;

public class OpenIdPhoneScope extends AbstractProfileScope {

    public static final String SCOPE = Config.SCOPE_PHONE;

    @Override
    public String getScope() {
        return SCOPE;
    }

    // TODO replace with keys for i18n
    @Override
    public String getName() {
        return "Read user's phone";
    }

    @Override
    public String getDescription() {
        return "Basic user's phone.";
    }
}
