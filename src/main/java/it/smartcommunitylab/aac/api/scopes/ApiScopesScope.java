package it.smartcommunitylab.aac.api.scopes;

import it.smartcommunitylab.aac.Config;

public class ApiScopesScope extends AbstractInternalApiScope {

    public static final String SCOPE = AACApiResource.RESOURCE_ID + ".scopes";

    public ApiScopesScope(String realm, String resourceId) {
        super(realm, resourceId, SCOPE);
        setAuthorities(Config.R_ADMIN, Config.R_DEVELOPER);
    }

    // TODO replace with keys for i18n
    @Override
    public String getName() {
        return "Manage scopes";
    }

    @Override
    public String getDescription() {
        return "Manage resources and scopes";
    }

}
