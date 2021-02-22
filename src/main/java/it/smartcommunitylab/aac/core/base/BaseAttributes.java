package it.smartcommunitylab.aac.core.base;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import it.smartcommunitylab.aac.core.model.UserAttributes;

public class BaseAttributes implements UserAttributes {

    private String authority;
    private String provider;
    private Map<String, String> attributes;

    public BaseAttributes(String authority, String provider,
            Collection<Map.Entry<String, String>> attributes) {
        this.authority = authority;
        this.provider = provider;
        setAttributes(attributes);
    }

    public String getAuthority() {
        return authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Collection<Map.Entry<String, String>> attributes) {
        // we pack attribute list in map, overlapping keys will contain last value
        this.attributes = new HashMap<>();
        for (Map.Entry<String, String> attr : attributes) {
            this.attributes.put(attr.getKey(), attr.getValue());
        }
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
