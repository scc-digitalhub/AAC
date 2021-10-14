package it.smartcommunitylab.aac.openid.scope;

import it.smartcommunitylab.aac.scope.Resource;

public class OpenIdResource extends Resource {
    public static final String RESOURCE_ID = "aac.openid";

    @Override
    public String getResourceId() {
        return RESOURCE_ID;
    }

    // TODO replace with keys for i18n
    @Override
    public String getName() {
        return "OpenId Connect";
    }

    @Override
    public String getDescription() {
        return "OpenId Connect core";
    }
}
