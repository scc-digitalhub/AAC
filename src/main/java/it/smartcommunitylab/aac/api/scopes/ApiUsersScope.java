package it.smartcommunitylab.aac.api.scopes;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.api.scopes.AACApiResource.AbstractAACApiScope;

public class ApiUsersScope extends AbstractAACApiScope {

    public static final String SCOPE = AACApiResource.RESOURCE_ID + ".users";

    public ApiUsersScope(String realm) {
        super(realm, SCOPE);
        setAuthorities(Config.R_ADMIN);
    }

//    // TODO replace with keys for i18n
//    @Override
//    public String getName() {
//        return "Manage users";
//    }
//
//    @Override
//    public String getDescription() {
//        return "Manage user identities, accounts and attributes.";
//    }

}
