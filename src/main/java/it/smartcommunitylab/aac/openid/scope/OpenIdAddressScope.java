package it.smartcommunitylab.aac.openid.scope;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.model.SubjectType;
import it.smartcommunitylab.aac.openid.scope.OpenIdUserInfoResource.AbstractOpenIdUserInfoScope;

public class OpenIdAddressScope extends AbstractOpenIdUserInfoScope {

    public static final String SCOPE = Config.SCOPE_ADDRESS;

    public OpenIdAddressScope(String realm) {
        super(realm, SCOPE);

        // require user
        this.subjectType = SubjectType.USER;
    }
//
//    // TODO replace with keys for i18n
//    @Override
//    public String getName() {
//        return "Read user's address";
//    }
//
//    @Override
//    public String getDescription() {
//        return "Basic user's address.";
//    }

}
