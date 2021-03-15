package it.smartcommunitylab.aac.attributes;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.Set;

public class InMemoryAttributeStore implements AttributeStore {

    private final Map<String, Map<String, String>> attributes;

    public InMemoryAttributeStore() {
        this.attributes = new HashMap<>();
    }

    @Override
    public String getAttribute(String userId, String key) {
        if (!attributes.containsKey(userId)) {
            return null;
        }

        return attributes.get(userId).get(key);
    }

    @Override
    public Map<String, String> findAttributes(String userId) {
        return Collections.unmodifiableMap(attributes.get(userId));
    }

    @Override
    public void setAttributes(String userId, Set<Entry<String, String>> attributesSet) {
        Map<String, String> attributesMap = new HashMap<>();
        for (Entry<String, String> entry : attributesSet) {
            attributesMap.put(entry.getKey(), entry.getValue());
        }

        this.attributes.put(userId, attributesMap);
    }

    @Override
    public void addAttribute(String userId, String key, String value) {
        if (!this.attributes.containsKey(userId)) {
            this.attributes.put(userId, new HashMap<>());
        }

        this.attributes.get(userId).put(key, value);

    }

    @Override
    public void updateAttribute(String userId, String key, String value) throws NoSuchElementException {
        if (!this.attributes.containsKey(userId)) {
            throw new NoSuchElementException();
        }

        if (this.attributes.get(userId).get(key) == null) {
            throw new NoSuchElementException();
        }

        this.attributes.get(userId).put(key, value);

    }

    @Override
    public void deleteAttribute(String userId, String key) {
        if (this.attributes.containsKey(userId)) {
            this.attributes.get(userId).remove(key);
        }
    }

}
