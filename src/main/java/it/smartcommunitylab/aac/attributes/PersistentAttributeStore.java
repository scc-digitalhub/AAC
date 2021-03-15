package it.smartcommunitylab.aac.attributes;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import it.smartcommunitylab.aac.core.persistence.AttributeEntity;
import it.smartcommunitylab.aac.core.service.AttributeEntityService;

public class PersistentAttributeStore implements AttributeStore {

    private AttributeEntityService attributeService;

    private final String authority;
    private final String provider;

    public PersistentAttributeStore(String authority, String provider, AttributeEntityService attributeService) {
        this.attributeService = attributeService;
        this.authority = authority;
        this.provider = provider;
    }

    public AttributeEntityService getAttributeService() {
        return attributeService;
    }

    public void setAttributeService(AttributeEntityService attributeService) {
        this.attributeService = attributeService;
    }

    public String getAuthority() {
        return authority;
    }

    public String getProvider() {
        return provider;
    }

    @Override
    public String getAttribute(String userId, String key) {
        AttributeEntity attr = attributeService.getAttribute(authority, provider, userId, key);
        if (attr == null) {
            return null;
        }

        return attr.getValue();
    }

    @Override
    public Map<String, String> findAttributes(String userId) {
        List<AttributeEntity> attributes = attributeService.findAttributes(authority, provider, userId);
        if (attributes.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> map = new HashMap<>();
        for (AttributeEntity attr : attributes) {
            map.put(attr.getKey(), attr.getValue());
        }

        return map;

    }

    @Override
    public void setAttributes(String userId, Set<Entry<String, String>> attributesMap) {
        // replace all attributes
        List<AttributeEntity> attributes = attributeService.setAttributes(authority, provider, userId, attributesMap);
    }

    @Override
    public void addAttribute(String userId, String key, String value) {
        // add as new, will trigger error if exists
        AttributeEntity attribute = attributeService.addAttribute(authority, provider, userId, key, value);
    }

    @Override
    public void updateAttribute(String userId, String key, String value) {
        AttributeEntity attribute = attributeService.updateAttribute(authority, provider, userId, key, value);

    }

    @Override
    public void deleteAttribute(String userId, String key) {
        attributeService.deleteAttribute(authority, provider, userId, key);
    }

}
