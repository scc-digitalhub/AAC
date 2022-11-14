package it.smartcommunitylab.aac.core.base;

import java.io.Serializable;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.model.UserResource;

public abstract class AbstractBaseUserResource implements UserResource, Serializable {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    private final String authority;
    private final String realm;
    private final String provider;
    private String userId;

    protected AbstractBaseUserResource(String authority, String provider, String realm) {
        this.authority = authority;
        this.realm = realm;
        this.provider = provider;
    }

    protected AbstractBaseUserResource(String authority, String provider, String realm, String userId) {
        this.authority = authority;
        this.realm = realm;
        this.provider = provider;
        this.userId = userId;
    }

    /**
     * Private constructor for JPA and other serialization tools.
     * 
     * We need to implement this to enable deserialization of resources via
     * reflection
     */
    @SuppressWarnings("unused")
    private AbstractBaseUserResource() {
        this((String) null, (String) null, (String) null);
    }

    @Override
    public String getAuthority() {
        return authority;
    }

    @Override
    public String getRealm() {
        return realm;
    }

    @Override
    public String getProvider() {
        return provider;
    }

    @Override
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

}
