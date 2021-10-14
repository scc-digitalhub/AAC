package it.smartcommunitylab.aac.oauth.scope;

import it.smartcommunitylab.aac.scope.Resource;

public class OAuth2DCRResource extends Resource {
    public static final String RESOURCE_ID = "aac.oauth2";

    @Override
    public String getResourceId() {
        return RESOURCE_ID;
    }

    // TODO replace with keys for i18n
    @Override
    public String getName() {
        return "OAuth2 Dynamic Client Registration";
    }

    @Override
    public String getDescription() {
        return "OAuth2 Dynamic Client Registration";
    }
}
