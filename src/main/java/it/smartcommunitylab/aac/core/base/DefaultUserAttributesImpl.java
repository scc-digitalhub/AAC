package it.smartcommunitylab.aac.core.base;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import it.smartcommunitylab.aac.core.model.UserAttributes;

/*
 * Default User Attributes is an instantiable bean which contains attributes bound to a user
 */

public class DefaultUserAttributesImpl extends BaseAttributes {

    private String internalUserId;
    private Map<String, String> attributes;

    public DefaultUserAttributesImpl(String authority, String provider, String realm) {
        super(authority, provider, realm);
        this.attributes = new HashMap<>();
    }

    public String getInternalUserId() {
        return internalUserId;
    }

    public void setInternalUserId(String internalUserId) {
        this.internalUserId = internalUserId;
    }

    @Override
    public String getUserId() {
        // leverage the default mapper to translate internalId
        return exportInternalId(internalUserId);
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void addAttributes(Collection<Map.Entry<String, String>> attributes) {
        // we pack attribute list in map, overlapping keys will contain last value
        for (Map.Entry<String, String> attr : attributes) {
            this.attributes.put(attr.getKey(), attr.getValue());
        }
    }

    public void setAttributes(Collection<Map.Entry<String, String>> attributes) {
        // we pack attribute list in map, overlapping keys will contain last value
        this.attributes = new HashMap<>();
        addAttributes(attributes);
    }

    public void addAttribute(String key, String value) {
        if (value == null) {
            // set empty
            value = "";
        }
        this.attributes.put(key, value);
    }

    public void deleteAttribute(String key) {
        this.attributes.remove(key);
    }

    public boolean hasAttribute(String key) {
        return attributes.containsKey(key);
    }

    public String getAttribute(String key) {
        if (attributes.containsKey(key)) {
            return attributes.get(key);
        } else {
            return null;
        }
    }

}
