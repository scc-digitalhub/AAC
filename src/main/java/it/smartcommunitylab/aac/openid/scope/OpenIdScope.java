package it.smartcommunitylab.aac.openid.scope;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.model.SubjectType;
import it.smartcommunitylab.aac.openid.scope.OpenIdResource.AbstractOpenIdScope;

public class OpenIdScope extends AbstractOpenIdScope {

    public static final String SCOPE = Config.SCOPE_OPENID;

    public OpenIdScope(String realm) {
        super(realm, SCOPE);

        // require user
        this.subjectType = SubjectType.USER;
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
