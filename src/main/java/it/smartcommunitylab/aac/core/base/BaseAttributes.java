package it.smartcommunitylab.aac.core.base;

import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;

/*
 * Base class which extracts identifiers from keys
 * 
 * TODO implement mapper to bean
 */

public abstract class BaseAttributes extends AbstractAttributes {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    protected final String identifier;

//    protected BaseAttributes(String authority, String provider, String realm, String identifier) {
//        super(authority, provider, realm);
//        Assert.hasText(identifier, "set identifier can not be null");
//        this.identifier = identifier;
//    }

    protected BaseAttributes(String authority, String provider, String realm, String userId, String identifier) {
        super(authority, provider, realm, userId);
        Assert.hasText(identifier, "set identifier can not be null");
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }

}
