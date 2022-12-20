package it.smartcommunitylab.aac.api.scopes;

import it.smartcommunitylab.aac.Config;

public class ApiProviderScope extends AbstractInternalApiScope {

    public static final String SCOPE = AACApiResource.RESOURCE_ID + ".provider";

    public ApiProviderScope(String realm, String resourceId) {
        super(realm, resourceId, SCOPE);
        setAuthorities(Config.R_ADMIN);
    }

    // TODO replace with keys for i18n
    @Override
    public String getName() {
        return "Manage providers";
    }

    @Override
    public String getDescription() {
        return "Manage identity and attribute providers.";
    }

}
