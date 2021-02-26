package it.smartcommunitylab.aac.core.base;

import java.util.Collection;

/*
 * Base class which extracts identifiers from keys
 * 
 * TODO implement mapper to bean
 */

public abstract class BaseAttributes extends AbstractAttributes {

    private String identifier;

    protected BaseAttributes(String authority, String provider, String realm) {
        super(authority, provider, realm);
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public Collection<String> getKeys() {
        return getAttributes().keySet();
    }

}
