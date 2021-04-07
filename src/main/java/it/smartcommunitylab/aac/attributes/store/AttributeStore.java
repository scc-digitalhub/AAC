package it.smartcommunitylab.aac.attributes.store;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

/*
 * Read-write interface for managing attribute stores
 * 
 * could be used for implementing persistent or in memory services
 */
public interface AttributeStore extends AttributeService {

    /*
     * crud
     */
    public void setAttributes(
            String entityId,
            Set<Map.Entry<String, Serializable>> attributesSet);

    public void addAttribute(
            String entityId,
            String key, Serializable value);
//
//    public void addOrUpdateAttribute(
//            String userId,
//            String key, String value);

    public void updateAttribute(
            String entityId,
            String key, Serializable value);

    public void deleteAttribute(
            String entityId,
            String key);

}
