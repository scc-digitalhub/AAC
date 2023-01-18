package it.smartcommunitylab.aac.openid.scope;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.scope.base.AbstractInternalApiScope;

public class OfflineAccessScope extends AbstractInternalApiScope {

    public static final String SCOPE = Config.SCOPE_OFFLINE_ACCESS;

    public OfflineAccessScope(String realm) {
        super(SystemKeys.AUTHORITY_OIDC, realm, OpenIdResource.RESOURCE_ID, SCOPE);

        // require user
        this.subjectType = SystemKeys.RESOURCE_USER;
    }

//    // TODO replace with keys for i18n
//    @Override
//    public String getName() {
//        return "Offline access";
//    }
//
//    @Override
//    public String getDescription() {
//        return "Offline access for obtaining refresh tokens";
//    }

}
