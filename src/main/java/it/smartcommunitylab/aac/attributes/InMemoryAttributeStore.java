package it.smartcommunitylab.aac.attributes;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
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
        return attributes.get(userId);
    }

    @Override
    public void setAttributes(String userId, Set<Entry<String, String>> attributesMap) {
        // TODO Auto-generated method stub

    }

    @Override
    public void addAttribute(String userId, String key, String value) {
        // TODO Auto-generated method stub

    }

    @Override
    public void addOrUpdateAttribute(String userId, String key, String value) {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateAttribute(String userId, String key, String value) {
        // TODO Auto-generated method stub

    }

    @Override
    public void deleteAttribute(String userId, String key) {
        // TODO Auto-generated method stub

    }

}
