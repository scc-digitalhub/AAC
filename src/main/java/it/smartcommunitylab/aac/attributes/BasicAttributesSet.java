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
public class BasicAttributesSet implements AttributeSet {
    public static final String IDENTIFIER = "aac.basic";
    private static final List<String> keys;

    private Map<String, Attribute> attributes;

    public BasicAttributesSet() {
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

    @Override
    public String getName() {
        // TODO i18n
        return "User base attributes";
    }

    @Override
    public String getDescription() {
        return "Base information about user";
    }

    public static final String USERNAME = "username";
    public static final String NAME = "name";
    public static final String SURNAME = "surname";
    public static final String EMAIL = "email";

    static {
        List<String> k = new ArrayList<>();
        k.add(USERNAME);
        k.add(NAME);
        k.add(SURNAME);
        k.add(EMAIL);
        keys = Collections.unmodifiableList(k);
    }
}
