package it.smartcommunitylab.aac.roles.scopes;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.model.SubjectType;
import it.smartcommunitylab.aac.roles.scopes.RolesResource.AbstractRolesScope;

public class UserRolesScope extends AbstractRolesScope {

    public static final String SCOPE = Config.SCOPE_USER_ROLE;

    public UserRolesScope(String realm) {
        super(realm, SCOPE);

        // require user
        this.subjectType = SubjectType.USER;
    }

//    // TODO replace with keys for i18n
//    @Override
//    public String getName() {
//        return "Read user's roles";
//    }
//
//    @Override
//    public String getDescription() {
//        return "Roles and authorities of the current platform user. Read access only.";
//    }

}
