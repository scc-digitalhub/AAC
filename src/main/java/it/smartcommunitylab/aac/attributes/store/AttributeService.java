package it.smartcommunitylab.aac.attributes.store;

import java.io.Serializable;
import java.util.Map;

/*
 * Read only interface for accessing attribute stores
 *
 * could be used for implementing persistent or in memory services
 */
public interface AttributeService {
    public Serializable getAttribute(String entityId, String key);

    public Map<String, Serializable> findAttributes(String entityId);
}
