package it.smartcommunitylab.aac.core.base;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import it.smartcommunitylab.aac.core.model.Attribute;
import it.smartcommunitylab.aac.core.model.UserAttributes;

/*
 * Default User Attributes is an instantiable bean which contains attributes bound to a user
 */

public class DefaultUserAttributesImpl extends BaseAttributes {

    private String userId;
    private Set<Attribute> attributes;

    public DefaultUserAttributesImpl(String authority, String provider, String realm, String identifier) {
        super(authority, provider, realm, identifier);
        this.attributes = new HashSet<>();
    }

    @Override
    public String getAttributesId() {
        return identifier + ":" + userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public String getUserId() {
        return userId;
    }

    public Collection<Attribute> getAttributes() {
        return attributes;
    }

    public void addAttributes(Collection<Attribute> attributes) {
        this.attributes.addAll(attributes);
    }

    public void addAttribute(Attribute attr) {
        this.attributes.add(attr);
    }

    public void deleteAttribute(String key) {
        Set<Attribute> toRemove = attributes.stream().filter(a -> a.getKey().equals(key)).collect(Collectors.toSet());
        if (!toRemove.isEmpty()) {
            attributes.removeAll(toRemove);
        }
    }

    public boolean hasAttribute(String key) {
        Set<Attribute> match = attributes.stream().filter(a -> a.getKey().equals(key)).collect(Collectors.toSet());
        return !match.isEmpty();
    }

    public Collection<Attribute> getAttribute(String key) {
        return attributes.stream().filter(a -> a.getKey().equals(key)).collect(Collectors.toSet());
    }

    @Override
    public Collection<String> getKeys() {
        return attributes.stream().map(a -> a.getKey()).collect(Collectors.toSet());
    }

}
