package it.smartcommunitylab.aac.groups.scopes;

import it.smartcommunitylab.aac.scope.Resource;

public class GroupsResource extends Resource {

    public static final String RESOURCE_ID = "aac.groups";

    @Override
    public String getResourceId() {
        return RESOURCE_ID;
    }

    // TODO replace with keys for i18n
    @Override
    public String getName() {
        return "Groups";
    }

    @Override
    public String getDescription() {
        return "Access groups for user and clients";
    }

}