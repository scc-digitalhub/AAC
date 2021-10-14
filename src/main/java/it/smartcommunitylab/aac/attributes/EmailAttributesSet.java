package it.smartcommunitylab.aac.attributes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import it.smartcommunitylab.aac.attributes.model.BooleanAttribute;
import it.smartcommunitylab.aac.attributes.model.StringAttribute;
import it.smartcommunitylab.aac.core.model.Attribute;
import it.smartcommunitylab.aac.core.model.AttributeSet;

@Component
public class EmailAttributesSet implements AttributeSet {
    public static final String IDENTIFIER = "aac.email";
    public static final List<String> keys;

    private Map<String, Attribute> attributes;

    public EmailAttributesSet() {
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
        return "User email attributes";
    }

    @Override
    public String getDescription() {
        return "User email addresses and status";
    }

    public static final String EMAIL = "email";
    public static final String EMAIL_VERIFIED = "email_verified";

    static {
        List<String> k = new ArrayList<>();
        k.add(EMAIL);
        k.add(EMAIL_VERIFIED);
        keys = Collections.unmodifiableList(k);
    }
}
