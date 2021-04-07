package it.smartcommunitylab.aac.attributes.store;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.util.Assert;

import java.util.NoSuchElementException;
import java.util.Set;

public class InMemoryAttributeStore implements AttributeStore {

    private final Map<String, Map<String, Serializable>> attributes;

    private final String authority;
    private final String provider;

    public InMemoryAttributeStore(String authority, String provider) {
        Assert.hasText(authority, "authority can not be null or empty");
        Assert.hasText(provider, "provider can not be null or empty");
        this.authority = authority;
        this.provider = provider;

        this.attributes = new HashMap<>();
    }

    @Override
    public Serializable getAttribute(String entityId, String key) {
        if (!attributes.containsKey(entityId)) {
            return null;
        }

        return attributes.get(entityId).get(key);
    }

    @Override
    public Map<String, Serializable> findAttributes(String entityId) {
        return Collections.unmodifiableMap(attributes.get(entityId));
    }

    @Override
    public void setAttributes(String entityId, Set<Entry<String, Serializable>> attributesSet) {
        Map<String, Serializable> attributesMap = new HashMap<>();
        for (Entry<String, Serializable> entry : attributesSet) {
            attributesMap.put(entry.getKey(), entry.getValue());
        }

        this.attributes.put(entityId, attributesMap);
    }

    @Override
    public void addAttribute(String entityId, String key, Serializable value) {
        if (!this.attributes.containsKey(entityId)) {
            this.attributes.put(entityId, new HashMap<>());
        }

        this.attributes.get(entityId).put(key, value);

    }

    @Override
    public void updateAttribute(String entityId, String key, Serializable value) throws NoSuchElementException {
        if (!this.attributes.containsKey(entityId)) {
            throw new NoSuchElementException();
        }

        if (this.attributes.get(entityId).get(key) == null) {
            throw new NoSuchElementException();
        }

        this.attributes.get(entityId).put(key, value);

    }

    @Override
    public void deleteAttribute(String entityId, String key) {
        if (this.attributes.containsKey(entityId)) {
            this.attributes.get(entityId).remove(key);
        }
    }

}
