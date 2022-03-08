package it.smartcommunitylab.aac.core.base;

import java.io.Serializable;

import it.smartcommunitylab.aac.SystemKeys;

public abstract class AbstractProviderConfig implements ConfigurableProperties, Serializable {
    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    private final String authority;
    private final String realm;
    private final String provider;

    protected AbstractProviderConfig(String authority, String provider, String realm) {
        this.authority = authority;
        this.realm = realm;
        this.provider = provider;
    }

    /**
     * Private constructor for JPA and other serialization tools.
     * 
     * We need to implement this to enable deserialization of resources via
     * reflection
     */
    @SuppressWarnings("unused")
    private AbstractProviderConfig() {
        this((String) null, (String) null, (String) null);
    }

    public String getAuthority() {
        return authority;
    }

    public String getRealm() {
        return realm;
    }

    public String getProvider() {
        return provider;
    }

    public abstract String getType();
}
