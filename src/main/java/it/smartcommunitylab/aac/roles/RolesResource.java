package it.smartcommunitylab.aac.roles;

import it.smartcommunitylab.aac.scope.Resource;

public class RolesResource extends Resource {

    public static final String RESOURCE_ID = "aac.roles";

    @Override
    public String getResourceId() {
        return RESOURCE_ID;
    }

    // TODO replace with keys for i18n
    @Override
    public String getName() {
        return "User roles";
    }

    @Override
    public String getDescription() {
        return "Access user roles and groups";
    }

}