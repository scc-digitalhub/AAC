package it.smartcommunitylab.aac.api.scopes;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.api.scopes.AACApiResource.AbstractAACApiScope;

public class ApiAttributesScope extends AbstractAACApiScope {

    public static final String SCOPE = AACApiResource.RESOURCE_ID + ".attributes";

    public ApiAttributesScope(String realm) {
        super(realm, SCOPE);
        setAuthorities(Config.R_ADMIN);
    }

//    // TODO replace with keys for i18n
//    @Override
//    public String getName() {
//        return "Manage attribute sets";
//    }
//
//    @Override
//    public String getDescription() {
//        return "Manage custom attribute sets definitions";
//    }

}
