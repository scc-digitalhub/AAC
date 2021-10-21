package it.smartcommunitylab.aac.roles.scopes;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.model.ScopeType;
import it.smartcommunitylab.aac.scope.Scope;

public class SpacesScope extends Scope {

    @Override
    public String getResourceId() {
        return RolesResource.RESOURCE_ID;
    }

    @Override
    public ScopeType getType() {
        return ScopeType.GENERIC;
    }

    @Override
    public String getScope() {
        return Config.SCOPE_USER_SPACES;
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
