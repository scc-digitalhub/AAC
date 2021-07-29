package it.smartcommunitylab.aac.core.base;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.model.UserIdentity;

/*
 * Abstract identity 
 * 
 * all implementations should derive from this
 */
public abstract class AbstractIdentity extends AbstractBaseResource implements UserIdentity {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    protected AbstractIdentity(String authority, String provider, String realm) {
        super(authority, provider, realm);
    }

    public String getType() {
        return SystemKeys.RESOURCE_IDENTITY;
    }

}
