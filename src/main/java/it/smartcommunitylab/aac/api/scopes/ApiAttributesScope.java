package it.smartcommunitylab.aac.api.scopes;

import it.smartcommunitylab.aac.Config;

public class ApiAttributesScope extends AbstractInternalApiScope {

    public static final String SCOPE = AACApiResource.RESOURCE_ID + ".attributes";

    public ApiAttributesScope(String realm, String resourceId) {
        super(realm, resourceId, SCOPE);
        setAuthorities(Config.R_ADMIN);
    }

    // TODO replace with keys for i18n
    @Override
    public String getName() {
        return "Manage attribute sets";
    }

    @Override
    public String getDescription() {
        return "Manage custom attribute sets definitions";
    }

}
