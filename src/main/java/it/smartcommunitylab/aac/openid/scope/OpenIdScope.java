package it.smartcommunitylab.aac.openid.scope;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.scope.base.AbstractInternalApiScope;

public class OpenIdScope extends AbstractInternalApiScope {

    public static final String SCOPE = Config.SCOPE_OPENID;

    public OpenIdScope(String realm) {
        super(SystemKeys.AUTHORITY_OIDC, realm, OpenIdResource.RESOURCE_ID, SCOPE);

        // require user
        this.subjectType = SystemKeys.RESOURCE_USER;
    }

//    // TODO replace with keys for i18n
//    @Override
//    public String getName() {
//        return "OpenId";
//    }
//
//    @Override
//    public String getDescription() {
//        return "User identity information (username and identifier). Read access only.";
//    }

}
