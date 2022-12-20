package it.smartcommunitylab.aac.api.scopes;

import it.smartcommunitylab.aac.Config;

public class ApiServicesScope extends AbstractInternalApiScope {

    public static final String SCOPE = AACApiResource.RESOURCE_ID + ".services";

    public ApiServicesScope(String realm, String resourceId) {
        super(realm, resourceId, SCOPE);
        setAuthorities(Config.R_ADMIN, Config.R_DEVELOPER);
    }

    // TODO replace with keys for i18n
    @Override
    public String getName() {
        return "Manage services";
    }

    @Override
    public String getDescription() {
        return "Manage custom services";
    }

}
