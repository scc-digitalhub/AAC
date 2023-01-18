package it.smartcommunitylab.aac.api.scopes;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.scope.base.AbstractInternalApiScope;

public class ApiScopesScope extends AbstractInternalApiScope {

    public static final String SCOPE = AACApiResource.RESOURCE_ID + ".scopes";

    public ApiScopesScope(String realm) {
        super(realm, AACApiResource.RESOURCE_ID, SCOPE);
        setAuthorities(Config.R_ADMIN, Config.R_DEVELOPER);
    }

//    // TODO replace with keys for i18n
//    @Override
//    public String getName() {
//        return "Manage scopes";
//    }
//
//    @Override
//    public String getDescription() {
//        return "Manage resources and scopes";
//    }

}
