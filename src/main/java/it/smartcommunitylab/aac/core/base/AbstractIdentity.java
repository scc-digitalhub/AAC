package it.smartcommunitylab.aac.core.base;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.model.UserIdentity;

/*
 * Abstract identity 
 * 
 * all implementations should derive from this
 */
public abstract class AbstractIdentity extends AbstractBaseUserResource implements UserIdentity {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    protected AbstractIdentity(String authority, String provider, String realm) {
        super(authority, provider, realm);
    }

    protected AbstractIdentity(String authority, String provider, String realm, String userId) {
        super(authority, provider, realm, userId);
    }

    @Override
    public final String getType() {
        return SystemKeys.RESOURCE_IDENTITY;
    }

}
