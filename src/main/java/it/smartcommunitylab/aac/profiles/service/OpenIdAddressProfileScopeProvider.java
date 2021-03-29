package it.smartcommunitylab.aac.profiles.service;

import org.springframework.stereotype.Component;

import it.smartcommunitylab.aac.Config;

@Component
public class OpenIdAddressProfileScopeProvider extends ProfileScopeProvider {

    @Override
    protected String getScope() {
        return Config.SCOPE_ADDRESS;
    }

    // TODO replace with keys for i18n
    public String getName() {
        return "Read user's address";
    }

    public String getDescription() {
        return "Basic user's address.";
    }

}
