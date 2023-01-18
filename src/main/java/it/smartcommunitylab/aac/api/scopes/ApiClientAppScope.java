package it.smartcommunitylab.aac.api.scopes;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.scope.base.AbstractInternalApiScope;

public class ApiClientAppScope extends AbstractInternalApiScope {

    public static final String SCOPE = AACApiResource.RESOURCE_ID + ".clientapp";

    public ApiClientAppScope(String realm) {
        super(realm, AACApiResource.RESOURCE_ID, SCOPE);
        setAuthorities(Config.R_ADMIN, Config.R_DEVELOPER);
    }

//    // TODO replace with keys for i18n
//    @Override
//    public String getName() {
//        return "Manage client apps";
//    }
//
//    @Override
//    public String getDescription() {
//        return "Manage client applications.";
//    }

}
