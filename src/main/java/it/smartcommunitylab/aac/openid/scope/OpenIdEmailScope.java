package it.smartcommunitylab.aac.openid.scope;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.openid.scope.OpenIdUserInfoResource.AbstractOpenIdUserInfoScope;

public class OpenIdEmailScope extends AbstractOpenIdUserInfoScope {

    public static final String SCOPE = Config.SCOPE_EMAIL;

    public OpenIdEmailScope(String realm) {
        super(realm, SCOPE);

        // require user
        this.subjectType = SystemKeys.RESOURCE_USER;
    }

//    // TODO replace with keys for i18n
//    @Override
//    public String getName() {
//        return "Read user's email";
//    }
//
//    @Override
//    public String getDescription() {
//        return "Basic user's email";
//    }

}
