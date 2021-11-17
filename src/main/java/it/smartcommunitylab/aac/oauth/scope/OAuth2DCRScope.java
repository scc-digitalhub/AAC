package it.smartcommunitylab.aac.oauth.scope;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.model.ScopeType;
import it.smartcommunitylab.aac.scope.Scope;

public class OAuth2DCRScope extends Scope {

    public static final String SCOPE = Config.SCOPE_DYNAMIC_CLIENT_REGISTRATION;

    @Override
    public String getResourceId() {
        return OAuth2DCRResource.RESOURCE_ID;
    }

    @Override
    public ScopeType getType() {
        return ScopeType.GENERIC;
    }

    @Override
    public String getScope() {
        return SCOPE;
    }

    // TODO replace with keys for i18n
    @Override
    public String getName() {
        return "Dynamic client registration";
    }

    @Override
    public String getDescription() {
        return "Dynamic client registration for OAuth2/OIDC";
    }

}