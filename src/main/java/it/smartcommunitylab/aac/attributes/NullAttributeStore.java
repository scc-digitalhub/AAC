package it.smartcommunitylab.aac.attributes;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/*
 * A null store, simply return null to all requests
 * 
 * used to ensure not persistence (not even in memory) to attributes for classes which wish to store attributes
 */

public class NullAttributeStore implements AttributeStore {

    @Override
    public String getAttribute(String userId, String key) {
        return null;
    }

    @Override
    public Map<String, String> findAttributes(String userId) {
        return null;
    }

    @Override
    public void setAttributes(String userId, Set<Entry<String, String>> attributesSet) {
    }

    @Override
    public void addAttribute(String userId, String key, String value) {
    }

    @Override
    public void updateAttribute(String userId, String key, String value) {
    }

    @Override
    public void deleteAttribute(String userId, String key) {
    }

}
