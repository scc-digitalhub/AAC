package it.smartcommunitylab.aac.core.base;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.model.UserAttributes;

/*
 * Abstract class for user attributes
 * 
 * all implementations should derive from this
 */
public abstract class AbstractAttributes extends AbstractBaseUserResource implements UserAttributes {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    protected AbstractAttributes(String authority, String provider, String realm) {
        super(authority, provider, realm);
    }

    protected AbstractAttributes(String authority, String provider, String realm, String userId) {
        super(authority, provider, realm, userId);
    }

    @Override
    public final String getType() {
        return SystemKeys.RESOURCE_ATTRIBUTES;
    }

    // local attributes identifier, for this set for this user
    @Override
    public String getId() {
        if (getIdentifier() == null || getUserId() == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(getUserId()).append(SystemKeys.URN_SEPARATOR);
        sb.append(getIdentifier());

        return sb.toString();
    }
}
