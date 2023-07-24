package it.smartcommunitylab.aac.profiles.scope;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public class CustomProfileScope extends AbstractProfileScope {

    private final String identifier;

    public CustomProfileScope(String identifier) {
        Assert.hasText(identifier, "identifier can not be null");
        this.identifier = identifier;
        this.scope = "profile." + identifier + ".me";
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    @Override
    public String getScope() {
        return scope;
    }

    // TODO replace with keys for i18n
    @Override
    public String getName() {
        return "Read user's profile " + identifier;
    }

    @Override
    public String getDescription() {
        return StringUtils.capitalize(identifier) + " profile of the current platform user. Read access only.";
    }
}
