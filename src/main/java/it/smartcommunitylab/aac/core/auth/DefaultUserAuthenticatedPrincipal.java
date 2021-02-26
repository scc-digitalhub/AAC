package it.smartcommunitylab.aac.core.auth;

import java.util.Map;

import org.springframework.util.Assert;

import it.smartcommunitylab.aac.core.UserAuthenticatedPrincipal;

public class DefaultUserAuthenticatedPrincipal implements UserAuthenticatedPrincipal {

    private final String userId;
    private final String name;
    private final Map<String, String> attributes;

    public DefaultUserAuthenticatedPrincipal(String userId, String name, Map<String, String> attributes) {
        Assert.notNull(name, "name cannot be null");
        Assert.notNull(userId, "userId cannot be null");
        Assert.notNull(attributes, "attributes cannot be null");

        this.userId = userId;
        this.name = name;
        this.attributes = attributes;
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

}
