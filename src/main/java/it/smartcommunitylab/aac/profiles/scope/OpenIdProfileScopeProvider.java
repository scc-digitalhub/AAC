package it.smartcommunitylab.aac.profiles.scope;

import org.springframework.stereotype.Component;

import it.smartcommunitylab.aac.Config;

@Component
public class OpenIdProfileScopeProvider extends ProfileScopeProvider {

    @Override
    protected String getScope() {
        return Config.SCOPE_OPENID;
    }

    // TODO replace with keys for i18n
    public String getName() {
        return "OpenId";
    }

    public String getDescription() {
        return "User identity information (username and identifier). Read access only.";
    }

}
