package it.smartcommunitylab.aac.roles.scopes;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.scope.base.AbstractInternalApiScope;

public class UserSpacesScope extends AbstractInternalApiScope {

    public static final String SCOPE = Config.SCOPE_USER_SPACES;

    public UserSpacesScope(String realm) {
        super(realm, RolesResource.RESOURCE_ID, SCOPE);

        // require user
        this.subjectType = SystemKeys.RESOURCE_USER;
    }

    // TODO replace with keys for i18n
    @Override
    public String getName() {
        return "Read user's spaces";
    }

    @Override
    public String getDescription() {
        return "Read spaces of the current platform user. Read access only.";
    }

}
