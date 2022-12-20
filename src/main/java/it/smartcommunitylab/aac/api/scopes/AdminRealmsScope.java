package it.smartcommunitylab.aac.api.scopes;

import it.smartcommunitylab.aac.Config;

public class AdminRealmsScope extends AbstractInternalApiScope {

    public static final String SCOPE = AdminApiResource.RESOURCE_ID + ".realms";

    public AdminRealmsScope(String realm) {
        super(realm, AdminApiResource.RESOURCE_ID, SCOPE);
        setAuthorities(Config.R_ADMIN);
    }

    // TODO replace with keys for i18n
    @Override
    public String getName() {
        return "Manage realms";
    }

    @Override
    public String getDescription() {
        return "Manage all realms.";
    }

}
