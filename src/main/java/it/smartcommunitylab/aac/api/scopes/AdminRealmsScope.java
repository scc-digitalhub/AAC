package it.smartcommunitylab.aac.api.scopes;

import it.smartcommunitylab.aac.model.ScopeType;
import it.smartcommunitylab.aac.scope.Scope;

public class AdminRealmsScope extends Scope {

    public static final String SCOPE = AdminResource.RESOURCE_ID + ".realms";

    @Override
    public String getResourceId() {
        return AdminResource.RESOURCE_ID;
    }

    @Override
    public String getScope() {
        return SCOPE;
    }

    @Override
    public ScopeType getType() {
        return ScopeType.USER;
    }

    // TODO replace with keys for i18n
    @Override
    public String getName() {
        return "Manage realms";
    }

    @Override
    public String getDescription() {
        return "Manage all realms.";
    }
}
