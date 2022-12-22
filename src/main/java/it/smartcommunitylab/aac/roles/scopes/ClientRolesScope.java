package it.smartcommunitylab.aac.roles.scopes;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.scope.base.AbstractInternalApiScope;

public class ClientRolesScope extends AbstractInternalApiScope {

    public static final String SCOPE = Config.SCOPE_CLIENT_ROLE;

    public ClientRolesScope(String realm) {
        super(realm, RolesResource.RESOURCE_ID, SCOPE);

        // require client
        this.subjectType = SystemKeys.RESOURCE_CLIENT;
    }

    // TODO replace with keys for i18n
    @Override
    public String getName() {
        return "Read client's roles";
    }

    @Override
    public String getDescription() {
        return "Roles and authorities of the current client. Read access only.";
    }

}
