package it.smartcommunitylab.aac.attributes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import it.smartcommunitylab.aac.attributes.model.StringAttribute;
import it.smartcommunitylab.aac.core.model.Attribute;
import it.smartcommunitylab.aac.core.model.AttributeSet;

@Component
public class AccountAttributesSet implements AttributeSet {
    public static final String IDENTIFIER = "aac.account";
    private static final List<String> keys;

    private Map<String, Attribute> attributes;

    public AccountAttributesSet() {
        this.attributes = new HashMap<>();
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public Collection<String> getKeys() {
        return keys;
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

    public void setUserId(String userId) {
        if (userId == null) {
            attributes.remove(USER_ID);
            return;
        }

        StringAttribute attr = new StringAttribute(USER_ID);
        attr.setValue(userId);

        attributes.put(USER_ID, attr);
    }

    public void setId(String id) {
        if (id == null) {
            attributes.remove(ID);
            return;
        }

        StringAttribute attr = new StringAttribute(ID);
        attr.setValue(id);

        attributes.put(ID, attr);
    }

    // additional attributes
    public void setAttribute(String key, String value) {
        if (key == null) {
            return;
        }

        if (USERNAME.equals(key) || USER_ID.equals(key) || ID.equals(key)) {
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

    @Override
    public String getName() {
        // TODO i18n
        return "User account attributes";
    }

    @Override
    public String getDescription() {
        return "Details about user accounts";
    }

    public static final String USER_ID = "user_id";
    public static final String USERNAME = "username";
    public static final String ID = "id";

    static {
        List<String> k = new ArrayList<>();
        k.add(USERNAME);
        k.add(USER_ID);
        k.add(ID);
        keys = Collections.unmodifiableList(k);
    }
}
