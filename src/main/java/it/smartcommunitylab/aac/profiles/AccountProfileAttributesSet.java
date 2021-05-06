package it.smartcommunitylab.aac.profiles;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.util.Assert;

import it.smartcommunitylab.aac.attributes.model.StringAttribute;
import it.smartcommunitylab.aac.core.base.BaseAttributes;
import it.smartcommunitylab.aac.core.model.Attribute;

public class AccountProfileAttributesSet extends BaseAttributes {
    public static final String IDENTIFIER = "profile.accountprofile.me";

    private final String userId;
    private Map<String, Attribute> attributes;

    public AccountProfileAttributesSet(String authority, String provider, String realm, String userId) {
        super(authority, provider, realm, IDENTIFIER);
        Assert.hasText(userId, "userId can not be null or blank");
        this.userId = userId;
        this.attributes = new HashMap<>();

        // build userId
        StringAttribute attr = new StringAttribute(USER_ID);
        attr.setValue(userId);
        attributes.put(USER_ID, attr);
    }

    @Override
    public String getUserId() {
        return userId;
    }

    @Override
    public String getAttributesId() {
        return IDENTIFIER + ":" + userId;
    }

    @Override
    public Collection<String> getKeys() {
        return attributes.keySet();
    }

    @Override
    public Collection<Attribute> getAttributes() {
        return attributes.values();
    }

    // basic attributes
    public void setUsername(String username) {
        if (username == null) {
            attributes.remove(USERNAME);
            return;
        }

        StringAttribute attr = new StringAttribute(USERNAME);
        attr.setValue(username);

        attributes.put(USERNAME, attr);
    }

    // additional attributes
    public void setAttribute(String key, String value) {
        if (key == null) {
            return;
        }

        if (USERNAME.equals(key) || USER_ID.equals(key)) {
            return;
        }

        if (value == null) {
            attributes.remove(key);
            return;
        }

        StringAttribute attr = new StringAttribute(key);
        attr.setValue(value);
        attributes.put(key, attr);

    }

    public static final String USER_ID = "user_id";
    public static final String USERNAME = "username";

}
