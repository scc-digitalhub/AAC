package it.smartcommunitylab.aac.profiles.scope;

import org.springframework.stereotype.Component;

import it.smartcommunitylab.aac.Config;

@Component
public class AccountProfileScopeProvider extends ProfileScopeProvider {

    @Override
    protected String getScope() {
        return Config.SCOPE_ACCOUNT_PROFILE;
    }

    // TODO replace with keys for i18n
    public String getName() {
        return "Read user's account profile";
    }

    public String getDescription() {
        return "Account profile of the current platform user. Read access only.";
    }

}
