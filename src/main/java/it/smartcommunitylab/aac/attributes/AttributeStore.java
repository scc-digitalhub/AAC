package it.smartcommunitylab.aac.attributes;

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
            String userId,
            Set<Map.Entry<String, String>> attributesSet);

    public void addAttribute(
            String userId,
            String key, String value);
//
//    public void addOrUpdateAttribute(
//            String userId,
//            String key, String value);

    public void updateAttribute(
            String userId,
            String key, String value);

    public void deleteAttribute(
            String userId,
            String key);

}
