package it.smartcommunitylab.aac.openid.scope;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.openid.scope.OpenIdResource.AbstractOpenIdScope;

public class OfflineAccessScope extends AbstractOpenIdScope {

    public static final String SCOPE = Config.SCOPE_OFFLINE_ACCESS;

    public OfflineAccessScope(String realm) {
        super(realm, SCOPE);

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
