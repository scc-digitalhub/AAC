package it.smartcommunitylab.aac.api.scopes;

import it.smartcommunitylab.aac.scope.Resource;

public class AdminResource extends Resource {

    public static final String RESOURCE_ID = "aac.admin";

    @Override
    public String getResourceId() {
        return RESOURCE_ID;
    }

    // TODO replace with keys for i18n
    @Override
    public String getName() {
        return "AAC Admin api";
    }

    @Override
    public String getDescription() {
        return "Access AAC admin api";
    }

}