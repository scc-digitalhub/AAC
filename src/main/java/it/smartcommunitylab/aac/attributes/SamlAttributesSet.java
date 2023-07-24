package it.smartcommunitylab.aac.attributes;

import it.smartcommunitylab.aac.attributes.model.BooleanAttribute;
import it.smartcommunitylab.aac.attributes.model.StringAttribute;
import it.smartcommunitylab.aac.core.model.Attribute;
import it.smartcommunitylab.aac.core.model.AttributeSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class SamlAttributesSet implements AttributeSet {

    public static final String IDENTIFIER = "aac.saml";
    private static final List<String> keys;

    private Map<String, Attribute> attributes;

    public SamlAttributesSet() {
        this.attributes = new HashMap<>();
    }

    public SamlAttributesSet(Collection<Attribute> attrs) {
        this.attributes = new HashMap<>();
        if (attrs != null) {
            attrs.forEach(a -> this.attributes.put(a.getKey(), a));
        }
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

    public void setUsername(String username) {
        if (username == null) {
            attributes.remove(USERNAME);
            return;
        }

        StringAttribute attr = new StringAttribute(USERNAME);
        attr.setValue(username);

        attributes.put(USERNAME, attr);
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

    public void setEmailVerified(Boolean emailVerified) {
        if (emailVerified == null) {
            attributes.remove(EMAIL_VERIFIED);
            return;
        }

        BooleanAttribute attr = new BooleanAttribute(EMAIL_VERIFIED);
        attr.setValue(emailVerified);

        attributes.put(EMAIL_VERIFIED, attr);
    }

    @Override
    public String getName() {
        // TODO i18n
        return "OpenId user attribute set";
    }

    @Override
    public String getDescription() {
        return "OpenId Connect default user attribute set";
    }

    public static final String NAME = "Name";
    public static final String SURNAME = "Surname";
    public static final String USERNAME = "Username";
    public static final String EMAIL = "Email";
    public static final String EMAIL_VERIFIED = "EmailVerified";

    static {
        List<String> k = new ArrayList<>();
        k.add(NAME);
        k.add(SURNAME);
        k.add(USERNAME);
        k.add(EMAIL);
        k.add(EMAIL_VERIFIED);

        keys = Collections.unmodifiableList(k);
    }
}
