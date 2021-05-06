package it.smartcommunitylab.aac.profiles;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.attributes.model.StringAttribute;
import it.smartcommunitylab.aac.core.base.BaseAttributes;
import it.smartcommunitylab.aac.core.model.Attribute;

public class BasicProfileAttributesSet extends BaseAttributes {
    public static final String IDENTIFIER = "profile.basicprofile.me";

    private final String userId;
    private Map<String, Attribute> attributes;

    public BasicProfileAttributesSet(String authority, String provider, String realm, String userId) {
        super(authority, provider, realm, IDENTIFIER);
        Assert.hasText(userId, "userId can not be null or blank");
        this.userId = userId;
        this.attributes = new HashMap<>();
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

    public void setName(String name) {
        if (name == null) {
            attributes.remove(NAME);
            return;
        }

        StringAttribute attr = new StringAttribute(NAME);
        attr.setValue(name);

        attributes.put(NAME, attr);
    }

    public void setSurname(String surname) {
        if (surname == null) {
            attributes.remove(SURNAME);
            return;
        }

        StringAttribute attr = new StringAttribute(SURNAME);
        attr.setValue(surname);

        attributes.put(SURNAME, attr);
    }

    public void setEmail(String email) {
        if (email == null) {
            attributes.remove(EMAIL);
            return;
        }

        StringAttribute attr = new StringAttribute(EMAIL);
        attr.setValue(email);

        attributes.put(EMAIL, attr);
    }

    public static final String USERNAME = "username";
    public static final String NAME = "name";
    public static final String SURNAME = "surname";
    public static final String EMAIL = "email";

}
