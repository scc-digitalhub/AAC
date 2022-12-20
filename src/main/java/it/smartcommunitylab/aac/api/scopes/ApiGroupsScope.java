package it.smartcommunitylab.aac.api.scopes;

import it.smartcommunitylab.aac.Config;

public class ApiGroupsScope extends AbstractInternalApiScope {

    public static final String SCOPE = AACApiResource.RESOURCE_ID + ".groups";

    public ApiGroupsScope(String realm, String resourceId) {
        super(realm, resourceId, SCOPE);
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
