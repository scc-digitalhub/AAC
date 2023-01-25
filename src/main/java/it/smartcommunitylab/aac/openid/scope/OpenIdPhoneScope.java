package it.smartcommunitylab.aac.openid.scope;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.model.SubjectType;
import it.smartcommunitylab.aac.openid.scope.OpenIdUserInfoResource.AbstractOpenIdUserInfoScope;

public class OpenIdPhoneScope extends AbstractOpenIdUserInfoScope {

    public static final String SCOPE = Config.SCOPE_PHONE;

    public OpenIdPhoneScope(String realm) {
        super(realm, SCOPE);

        // require user
        this.subjectType = SubjectType.USER;
    }
//
//    // TODO replace with keys for i18n
//    @Override
//    public String getName() {
//        return "Read user's phone";
//    }
//
//    @Override
//    public String getDescription() {
//        return "Basic user's phone.";
//    }

}
