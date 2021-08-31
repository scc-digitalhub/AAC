package it.smartcommunitylab.aac.core.auth;

import java.util.Collections;
import java.util.Map;

import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;

public class DefaultUserAuthenticatedPrincipal implements UserAuthenticatedPrincipal {
    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    private final String authority;
    private final String provider;
    private final String realm;

    private final String userId;
    private String name;
    private Map<String, String> attributes;

    public DefaultUserAuthenticatedPrincipal(String authority, String provider, String realm, String userId) {
        Assert.notNull(userId, "userId cannot be null");
        Assert.notNull(authority, "authority cannot be null");
        Assert.notNull(provider, "provider cannot be null");
        Assert.notNull(realm, "realm cannot be null");

        this.userId = userId;
        this.authority = authority;
        this.realm = realm;
        this.provider = provider;
        this.attributes = Collections.emptyMap();
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

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

}
