package it.smartcommunitylab.aac.attributes.store;

import java.io.Serializable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.springframework.util.Assert;

public class PersistentAttributeStore implements AttributeStore {

    private AutoJdbcAttributeStore attributeStore;

    private final String authority;
    private final String providerId;

    public PersistentAttributeStore(String authority, String provider, AutoJdbcAttributeStore attributeStore) {
        Assert.notNull(attributeStore, "attribute store is mandatory");
        Assert.hasText(authority, "authority can not be null or empty");
        Assert.hasText(provider, "provider can not be null or empty");
        this.attributeStore = attributeStore;
        this.authority = authority;
        this.providerId = provider;
    }

    @Override
    public Serializable getAttribute(String entityId, String key) {
        return attributeStore.getAttribute(providerId, entityId, key);
    }

    @Override
    public Map<String, Serializable> findAttributes(String entityId) {
        return attributeStore.findAttributes(providerId, entityId);
    }

    @Override
    public void setAttributes(String entityId, Set<Entry<String, Serializable>> attributesSet) {
        attributeStore.setAttributes(providerId, entityId, attributesSet);
    }

    @Override
    public void addAttribute(String entityId, String key, Serializable value) {
        attributeStore.addAttribute(providerId, entityId, key, value);
    }

    @Override
    public void updateAttribute(String entityId, String key, Serializable value) {
        attributeStore.updateAttribute(providerId, entityId, key, value);
    }

    @Override
    public void deleteAttribute(String entityId, String key) {
        attributeStore.deleteAttribute(providerId, entityId, key);
    }

}
