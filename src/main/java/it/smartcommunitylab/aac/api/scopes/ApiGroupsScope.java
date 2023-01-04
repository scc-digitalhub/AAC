package it.smartcommunitylab.aac.api.scopes;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.scope.base.AbstractInternalApiScope;

public class ApiGroupsScope extends AbstractInternalApiScope {

    public static final String SCOPE = AACApiResource.RESOURCE_ID + ".groups";

    public ApiGroupsScope(String realm) {
        super(realm, AACApiResource.RESOURCE_ID, SCOPE);
        setAuthorities(Config.R_ADMIN);
    }

    // TODO replace with keys for i18n
    @Override
    public String getName() {
        return "Manage groups";
    }

    @Override
    public String getDescription() {
        return "Manage realm groups and memberships.";
    }

}
