package it.smartcommunitylab.aac.profiles.scope;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.scope.base.AbstractInternalApiScope;

public class OpenIdEmailScope extends AbstractInternalApiScope {

    public static final String SCOPE = Config.SCOPE_EMAIL;

    public OpenIdEmailScope(String realm) {
        super(SystemKeys.AUTHORITY_OIDC, realm, OpenIdUserInfoResource.RESOURCE_ID, SCOPE);

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
