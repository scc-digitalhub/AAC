package it.smartcommunitylab.aac.core.service;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.smartcommunitylab.aac.Constants;
import it.smartcommunitylab.aac.core.persistence.AttributeEntity;
import it.smartcommunitylab.aac.core.persistence.AttributeEntityRepository;

@Service
public class AttributeEntityService {

    @Autowired
    private AttributeEntityRepository attributeRepository;

    public List<AttributeEntity> setAttributes(
            String subject, String authority, String provider,
            long userId,
            Set<AbstractMap.SimpleEntry<String, String>> attributesMap) {
        List<AttributeEntity> attributes = new ArrayList<>();

        // we sync attributes with those received by deleting missing
        List<AttributeEntity> oldAttributes = attributeRepository.findByAuthorityAndUserId(authority, userId);

        List<AttributeEntity> toRemove = new ArrayList<>();
        toRemove.addAll(oldAttributes);

        for (AbstractMap.SimpleEntry<String, String> attr : attributesMap) {
            AttributeEntity a = addOrUpdateAttribute(subject, Constants.AUTHORITY_INTERNAL, provider,
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

    public AttributeEntity addOrUpdateAttribute(String subject,
            String authority, String provider,
            long userId,
            String key, String value) {
        AttributeEntity a = attributeRepository.findByAuthorityAndProviderAndUserIdAndKey(authority, provider, userId,
                key);
        if (a == null) {
            a = new AttributeEntity();
            a.setSubject(subject);
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
            String subject,
            String authority, String provider,
            long userId,
            String key, String value) {

        AttributeEntity a = attributeRepository.findByAuthorityAndProviderAndUserIdAndKey(authority, provider, userId,
                key);
        if (a != null) {
            throw new IllegalArgumentException("duplicate key");
        }

        a = new AttributeEntity();
        a.setSubject(subject);
        a.setAuthority(authority);
        a.setProvider(provider);
        a.setUserId(userId);
        a.setKey(key);
        a.setValue(value);

        a = attributeRepository.save(a);

        return a;

    }

    public AttributeEntity updateAttribute(
            String subject,
            String authority, String provider,
            long userId,
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

    public void deleteAttribute(String subject,
            String authority, String provider,
            long userId,
            String key) {

        AttributeEntity a = attributeRepository.findByAuthorityAndProviderAndUserIdAndKey(authority, provider, userId,
                key);
        if (a != null) {
            attributeRepository.delete(a);
        }

    }

    public List<AttributeEntity> findAttributes(
            String subject) {
        return attributeRepository.findBySubject(subject);
    }

    public List<AttributeEntity> findAttributes(
            String authority, long userId) {

        return attributeRepository.findByAuthorityAndUserId(authority, userId);
    }

}
