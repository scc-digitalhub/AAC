package it.smartcommunitylab.aac.group.scopes;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.model.ScopeType;
import it.smartcommunitylab.aac.scope.Scope;

public class ClientGroupsScope extends Scope {

    public static final String SCOPE = Config.SCOPE_CLIENT_GROUP;

    @Override
    public String getResourceId() {
        return GroupsResource.RESOURCE_ID;
    }

    @Override
    public ScopeType getType() {
        return ScopeType.CLIENT;
    }

    @Override
    public String getScope() {
        return SCOPE;
    }

    // TODO replace with keys for i18n
    @Override
    public String getName() {
        return "Read client's groups";
    }

    @Override
    public String getDescription() {
        return "Groups of the current client. Read access only.";
    }

}
