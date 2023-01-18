package it.smartcommunitylab.aac.oauth.scope;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.scope.base.AbstractInternalApiScope;

public class OAuth2DCRScope extends AbstractInternalApiScope {

    public static final String SCOPE = Config.SCOPE_DYNAMIC_CLIENT_REGISTRATION;

    public OAuth2DCRScope(String realm) {
        super(SystemKeys.AUTHORITY_OAUTH2, realm, OAuth2DCRResource.RESOURCE_ID, SCOPE);

        // require developer or admin role
        setAuthorities(Config.R_ADMIN, Config.R_DEVELOPER);
    }

//    // TODO replace with keys for i18n
//    @Override
//    public String getName() {
//        return "Dynamic client registration";
//    }
//
//    @Override
//    public String getDescription() {
//        return "Dynamic client registration for OAuth2/OIDC";
//    }

}