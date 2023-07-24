package it.smartcommunitylab.aac.api.scopes;

import it.smartcommunitylab.aac.scope.Resource;

public class ApiResource extends Resource {

    public static final String RESOURCE_ID = "aac.api";

    @Override
    public String getResourceId() {
        return RESOURCE_ID;
    }

    // TODO replace with keys for i18n
    @Override
    public String getName() {
        return "AAC Api";
    }

    @Override
    public String getDescription() {
        return "Access AAC api";
    }
}
