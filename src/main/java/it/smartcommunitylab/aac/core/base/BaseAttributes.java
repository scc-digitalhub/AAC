package it.smartcommunitylab.aac.core.base;

import org.springframework.util.Assert;

/*
 * Base class which extracts identifiers from keys
 * 
 * TODO implement mapper to bean
 */

public abstract class BaseAttributes extends AbstractAttributes {

    protected final String identifier;

    protected BaseAttributes(String authority, String provider, String realm, String identifier) {
        super(authority, provider, realm);
        Assert.hasText(identifier, "set identifier can not be null");
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }

}
