package it.smartcommunitylab.aac.api.scopes;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.scope.base.AbstractInternalApiScope;

public class ApiRealmScope extends AbstractInternalApiScope {

    public static final String SCOPE = AACApiResource.RESOURCE_ID + ".realm";

    public ApiRealmScope(String realm) {
        super(realm, AACApiResource.RESOURCE_ID, SCOPE);
        setAuthorities(Config.R_ADMIN);
    }

//    // TODO replace with keys for i18n
//    @Override
//    public String getName() {
//        return "Manage realm";
//    }
//
//    @Override
//    public String getDescription() {
//        return "Manage realm settings and customization.";
//    }

}
