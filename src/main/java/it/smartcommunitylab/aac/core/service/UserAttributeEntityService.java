package it.smartcommunitylab.aac.core.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.core.persistence.UserAttributeEntity;
import it.smartcommunitylab.aac.core.persistence.UserAttributeEntityRepository;

@Service
@Transactional
public class UserAttributeEntityService {

    private final UserAttributeEntityRepository attributeRepository;

    public UserAttributeEntityService(UserAttributeEntityRepository attributeRepository) {
        Assert.notNull(attributeRepository, "attribute repository is mandatory");
        this.attributeRepository = attributeRepository;
    }

    /*
     * crud
     */

    public List<UserAttributeEntity> setAttributes(
            String authority, String provider,
            String userId,
            Set<Map.Entry<String, String>> attributesMap) {
        List<UserAttributeEntity> attributes = new ArrayList<>();

        // we sync attributes with those received by deleting missing
        List<UserAttributeEntity> oldAttributes = attributeRepository.findByAuthorityAndProviderAndUserId(authority,
                provider, userId);

        List<UserAttributeEntity> toRemove = new ArrayList<>();
        toRemove.addAll(oldAttributes);

        for (Map.Entry<String, String> attr : attributesMap) {
            UserAttributeEntity a = addOrUpdateAttribute(authority, provider,
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

    public UserAttributeEntity addOrUpdateAttribute(
            String authority, String provider,
            String userId,
            String key, String value) {
        UserAttributeEntity a = attributeRepository.findByAuthorityAndProviderAndUserIdAndKey(authority, provider, userId,
                key);
        if (a == null) {
            a = new UserAttributeEntity();
            a.setAuthority(authority);
            a.setProvider(provider);
            a.setUserId(userId);
            a.setKey(key);
        }

        a.setValue(value);
        a = attributeRepository.saveAndFlush(a);

        return a;
    }

    public UserAttributeEntity addAttribute(
            String authority, String provider,
            String userId,
            String key, String value) {

        UserAttributeEntity a = attributeRepository.findByAuthorityAndProviderAndUserIdAndKey(authority, provider, userId,
                key);
        if (a != null) {
            throw new IllegalArgumentException("duplicate key");
        }

        a = new UserAttributeEntity();
        a.setAuthority(authority);
        a.setProvider(provider);
        a.setUserId(userId);
        a.setKey(key);
        a.setValue(value);

        a = attributeRepository.save(a);

        return a;

    }

    public UserAttributeEntity updateAttribute(
            String authority, String provider,
            String userId,
            String key, String value) {
        UserAttributeEntity a = attributeRepository.findByAuthorityAndProviderAndUserIdAndKey(authority, provider, userId,
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

        UserAttributeEntity a = attributeRepository.findByAuthorityAndProviderAndUserIdAndKey(authority, provider, userId,
                key);
        if (a != null) {
            attributeRepository.delete(a);
        }

    }

    @Transactional(readOnly = true)
    public UserAttributeEntity getAttribute(String authority, String provider, String userId, String key) {
        return attributeRepository.findByAuthorityAndProviderAndUserIdAndKey(authority, provider, userId,
                key);
    }

    @Transactional(readOnly = true)
    public List<UserAttributeEntity> findAttributes(String authority, String provider) {
        return findByAuthorityAndProvider(authority, provider);
    }

    @Transactional(readOnly = true)
    public List<UserAttributeEntity> findAttributes(String authority, String provider, String userId) {
        return findByAuthorityAndProviderAndUserId(authority, provider, userId);
    }

    /*
     * expose finders
     * 
     * we don't enforce access patterns or security here
     */

    @Transactional(readOnly = true)
    public List<UserAttributeEntity> findByAuthority(String authority) {
        return attributeRepository.findByAuthority(authority);
    }

    @Transactional(readOnly = true)
    public List<UserAttributeEntity> findByAuthorityAndUserId(String authority, String userId) {
        return attributeRepository.findByAuthorityAndUserId(authority, userId);
    }

    @Transactional(readOnly = true)
    public List<UserAttributeEntity> findByAuthorityAndProvider(String authority, String provider) {
        return attributeRepository.findByAuthorityAndProvider(authority, provider);
    }

    @Transactional(readOnly = true)
    public List<UserAttributeEntity> findByAuthorityAndProviderAndUserId(String authority, String provider, String userId) {
        return attributeRepository.findByAuthorityAndProviderAndUserId(authority, provider, userId);
    }

    @Transactional(readOnly = true)
    public UserAttributeEntity findByAuthorityAndProviderAndUserIdAndKey(String authority, String provider, String userId,
            String key) {
        return attributeRepository.findByAuthorityAndProviderAndUserIdAndKey(authority, provider, userId, key);
    }

}
