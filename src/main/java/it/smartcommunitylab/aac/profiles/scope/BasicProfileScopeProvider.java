package it.smartcommunitylab.aac.profiles.scope;

import org.springframework.stereotype.Component;

import it.smartcommunitylab.aac.Config;

@Component
public class BasicProfileScopeProvider extends ProfileScopeProvider {

    @Override
    protected String getScope() {
        return Config.SCOPE_BASIC_PROFILE;
    }

    // TODO replace with keys for i18n
    public String getName() {
        return "Read user's basic profile";
    }

    public String getDescription() {
        return "Basic profile of the current platform user. Read access only.";
    }

}
