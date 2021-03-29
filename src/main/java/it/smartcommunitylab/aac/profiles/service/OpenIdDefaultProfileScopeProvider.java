package it.smartcommunitylab.aac.profiles.service;

import org.springframework.stereotype.Component;

import it.smartcommunitylab.aac.Config;

@Component
public class OpenIdDefaultProfileScopeProvider extends ProfileScopeProvider {

    @Override
    protected String getScope() {
        return Config.SCOPE_PROFILE;
    }

    // TODO replace with keys for i18n
    public String getName() {
        return "Read user's standard profile";
    }

    public String getDescription() {
        return "Basic user profile data (name, surname, email). Read access only.";
    }

}
