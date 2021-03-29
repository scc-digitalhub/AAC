package it.smartcommunitylab.aac.profiles.service;

import org.springframework.stereotype.Component;

import it.smartcommunitylab.aac.Config;

@Component
public class OpenIdPhoneProfileScopeProvider extends ProfileScopeProvider {

    @Override
    protected String getScope() {
        return Config.SCOPE_PHONE;
    }

    // TODO replace with keys for i18n
    public String getName() {
        return "Read user's phone";
    }

    public String getDescription() {
        return "Basic user's phone.";
    }

}
