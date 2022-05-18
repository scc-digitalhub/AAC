package it.smartcommunitylab.aac.core.base;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.model.UserAuthenticatedPrincipal;

/*
 * Abstract class for user authenticated principal
 * 
 * all implementations should derive from this
 */

public abstract class AbstractAuthenticatedPrincipal extends AbstractBaseUserResource
        implements UserAuthenticatedPrincipal {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    protected AbstractAuthenticatedPrincipal(String authority, String provider, String realm) {
        super(authority, provider, realm);
    }

    protected AbstractAuthenticatedPrincipal(String authority, String provider, String realm, String userId) {
        super(authority, provider, realm, userId);
    }

    @Override
    public final String getType() {
        return SystemKeys.RESOURCE_PRINCIPAL;
    }

}
