package it.smartcommunitylab.aac.core.base;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.model.UserAttributes;

/*
 * Abstract class for user attributes
 * 
 * all implementations should derive from this
 */
public abstract class AbstractAttributes extends AbstractBaseResource implements UserAttributes {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    protected AbstractAttributes(String authority, String provider, String realm) {
        super(authority, provider, realm);
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_ATTRIBUTES;
    }

}
