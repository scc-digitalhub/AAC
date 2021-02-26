package it.smartcommunitylab.aac.core.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.smartcommunitylab.aac.core.persistence.AttributeEntity;
import it.smartcommunitylab.aac.core.persistence.AttributeEntityRepository;

@Service
public class AttributeEntityService {

    @Autowired
    private AttributeEntityRepository attributeRepository;

    /*
     * crud
     */

    public List<AttributeEntity> setAttributes(
            String authority, String provider,
            String userId,
            Set<Map.Entry<String, String>> attributesMap) {
        List<AttributeEntity> attributes = new ArrayList<>();

        // we sync attributes with those received by deleting missing
        List<AttributeEntity> oldAttributes = attributeRepository.findByAuthorityAndProviderAndUserId(authority,
                provider, userId);

        List<AttributeEntity> toRemove = new ArrayList<>();
        toRemove.addAll(oldAttributes);

        for (Map.Entry<String, String> attr : attributesMap) {
            AttributeEntity a = addOrUpdateAttribute(authority, provider,
                    userId, attr.getKey(), attr.getValue());
            attributes.add(a);
            if (toRemove.contains(a)) {
                toRemove.remove(a);
            }
        }

        // remove orphans
        attributeRepository.deleteAll(toRemove);

        return attributes;

    }

    public AttributeEntity addOrUpdateAttribute(
            String authority, String provider,
            String userId,
            String key, String value) {
        AttributeEntity a = attributeRepository.findByAuthorityAndProviderAndUserIdAndKey(authority, provider, userId,
                key);
        if (a == null) {
            a = new AttributeEntity();
            a.setAuthority(authority);
            a.setProvider(provider);
            a.setUserId(userId);
            a.setKey(key);
        }

        a.setValue(value);
        a = attributeRepository.saveAndFlush(a);

        return a;
    }

    public AttributeEntity addAttribute(
            String authority, String provider,
            String userId,
            String key, String value) {

        AttributeEntity a = attributeRepository.findByAuthorityAndProviderAndUserIdAndKey(authority, provider, userId,
                key);
        if (a != null) {
            throw new IllegalArgumentException("duplicate key");
        }

        a = new AttributeEntity();
        a.setAuthority(authority);
        a.setProvider(provider);
        a.setUserId(userId);
        a.setKey(key);
        a.setValue(value);

        a = attributeRepository.save(a);

        return a;

    }

    public AttributeEntity updateAttribute(
            String authority, String provider,
            String userId,
            String key, String value) {
        AttributeEntity a = attributeRepository.findByAuthorityAndProviderAndUserIdAndKey(authority, provider, userId,
                key);
        if (a == null) {
            throw new NoSuchElementException();
        }

        a.setValue(value);
        a = attributeRepository.saveAndFlush(a);

        return a;
    }

    public void deleteAttribute(
            String authority, String provider,
            String userId,
            String key) {

        AttributeEntity a = attributeRepository.findByAuthorityAndProviderAndUserIdAndKey(authority, provider, userId,
                key);
        if (a != null) {
            attributeRepository.delete(a);
        }

    }

    public AttributeEntity getAttribute(String authority, String provider, String userId, String key) {
        return attributeRepository.findByAuthorityAndProviderAndUserIdAndKey(authority, provider, userId,
                key);
    }

    public List<AttributeEntity> findAttributes(String authority, String provider) {
        return findByAuthorityAndProvider(authority, provider);
    }

    public List<AttributeEntity> findAttributes(String authority, String provider, String userId) {
        return findByAuthorityAndProviderAndUserId(authority, provider, userId);
    }

    /*
     * expose finders
     * 
     * we don't enforce access patterns or security here
     */

    public List<AttributeEntity> findByAuthority(String authority) {
        return attributeRepository.findByAuthority(authority);
    }

    public List<AttributeEntity> findByAuthorityAndUserId(String authority, String userId) {
        return attributeRepository.findByAuthorityAndUserId(authority, userId);
    }

    public List<AttributeEntity> findByAuthorityAndProvider(String authority, String provider) {
        return attributeRepository.findByAuthorityAndProvider(authority, provider);
    }

    public List<AttributeEntity> findByAuthorityAndProviderAndUserId(String authority, String provider, String userId) {
        return attributeRepository.findByAuthorityAndProviderAndUserId(authority, provider, userId);
    }

    public AttributeEntity findByAuthorityAndProviderAndUserIdAndKey(String authority, String provider, String userId,
            String key) {
        return attributeRepository.findByAuthorityAndProviderAndUserIdAndKey(authority, provider, userId, key);
    }

}
