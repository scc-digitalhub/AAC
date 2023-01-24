package it.smartcommunitylab.aac.profiles.scope;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;

public class CustomProfileScope extends AbstractProfileScope {

    private final String identifier;

    public CustomProfileScope(String realm, String identifier) {
        super(realm, "profile." + identifier + ".me");
        Assert.hasText(identifier, "identifier can not be null");
        this.identifier = identifier;

        // require user
        this.subjectType = SystemKeys.RESOURCE_USER;
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
