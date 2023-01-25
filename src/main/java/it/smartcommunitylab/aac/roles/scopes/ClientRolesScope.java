package it.smartcommunitylab.aac.roles.scopes;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.model.SubjectType;
import it.smartcommunitylab.aac.roles.scopes.RolesResource.AbstractRolesScope;

public class ClientRolesScope extends AbstractRolesScope {

    public static final String SCOPE = Config.SCOPE_CLIENT_ROLE;

    public ClientRolesScope(String realm) {
        super(realm, SCOPE);

        // require client
        this.subjectType = SubjectType.CLIENT;
    }

    // TODO replace with keys for i18n
//    @Override
//    public String getName() {
//        return "Read client's roles";
//    }
//
//    @Override
//    public String getDescription() {
//        return "Roles and authorities of the current client. Read access only.";
//    }

}
