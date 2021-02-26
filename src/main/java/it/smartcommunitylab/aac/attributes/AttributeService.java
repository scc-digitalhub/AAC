package it.smartcommunitylab.aac.attributes;

import java.util.Map;

/*
 * Read only interface for accessing attribute stores
 * 
 * could be used for implementing persistent or in memory services
 */
public interface AttributeService {
    public String getAttribute(
            String userId,
            String key);

    public Map<String, String> findAttributes(String userId);
}
