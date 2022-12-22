package it.smartcommunitylab.aac.api.scopes;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.scope.base.AbstractInternalApiScope;

public class ApiAuditScope extends AbstractInternalApiScope {

    public static final String SCOPE = AACApiResource.RESOURCE_ID + ".audit";

    public ApiAuditScope(String realm, String resourceId) {
        super(realm, resourceId, SCOPE);
        setAuthorities(Config.R_ADMIN);
    }

    // TODO replace with keys for i18n
    @Override
    public String getName() {
        return "Read audit log";
    }

    @Override
    public String getDescription() {
        return "Audit log for events. Read access only.";
    }

}
