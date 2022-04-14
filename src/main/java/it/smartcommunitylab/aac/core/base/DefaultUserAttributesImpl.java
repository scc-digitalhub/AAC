package it.smartcommunitylab.aac.core.base;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.model.Attribute;
import it.smartcommunitylab.aac.core.model.AttributeSet;

/*
 * Default User Attributes is an instantiable bean which contains attributes bound to a user
 */

public class DefaultUserAttributesImpl extends BaseAttributes {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    private Set<Attribute> attributes;

    private String uuid;
    private String name;
    private String description;

    public DefaultUserAttributesImpl(String authority, String provider, String realm, String userId,
            String identifier) {
        super(authority, provider, realm, userId, identifier);
        this.attributes = new HashSet<>();
    }

    public DefaultUserAttributesImpl(String authority, String provider, String realm, String userId,
            AttributeSet attributeSet) {
        super(authority, provider, realm, userId, attributeSet.getIdentifier());
        this.attributes = new HashSet<>();
        this.attributes.addAll(attributeSet.getAttributes());
        this.name = attributeSet.getName();
        this.description = attributeSet.getDescription();

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

}
