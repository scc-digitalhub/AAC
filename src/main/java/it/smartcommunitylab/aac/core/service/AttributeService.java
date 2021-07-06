package it.smartcommunitylab.aac.core.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.common.NoSuchAttributeException;
import it.smartcommunitylab.aac.common.NoSuchAttributeSetException;
import it.smartcommunitylab.aac.core.persistence.AttributeEntity;
import it.smartcommunitylab.aac.core.persistence.AttributeEntityRepository;
import it.smartcommunitylab.aac.core.persistence.AttributeSetEntity;
import it.smartcommunitylab.aac.core.persistence.AttributeSetEntityRepository;
import it.smartcommunitylab.aac.model.AttributeType;

@Service
@Transactional
public class AttributeService {

    private final AttributeEntityRepository attributeRepository;
    private final AttributeSetEntityRepository attributeSetRepository;

    public AttributeService(AttributeSetEntityRepository attributeSetRepository,
            AttributeEntityRepository attributeRepository) {
        Assert.notNull(attributeSetRepository, "attributeSet repository is mandatory");
        Assert.notNull(attributeRepository, "attributes repository is mandatory");

        this.attributeSetRepository = attributeSetRepository;
        this.attributeRepository = attributeRepository;

    }

    /*
     * Attribute sets
     */

    @Transactional(readOnly = true)
    public List<AttributeSetEntity> listAttributeSets() {
        return attributeSetRepository.findAll();
    }

    @Transactional(readOnly = true)
    public AttributeSetEntity findAttributeSet(String identifier) {
        return attributeSetRepository.findBySet(identifier);
    }

    @Transactional(readOnly = true)
    public AttributeSetEntity getAttributeSet(String identifier) throws NoSuchAttributeSetException {
        AttributeSetEntity set = attributeSetRepository.findBySet(identifier);
        if (set == null) {
            throw new NoSuchAttributeSetException();
        }

        return set;
    }

    public AttributeSetEntity addAttributeSet(
            String identifier,
            String name, String description) {
        if (!StringUtils.hasText(identifier)) {
            throw new IllegalArgumentException("empty set identifier");
        }

        AttributeSetEntity set = attributeSetRepository.findBySet(identifier);
        if (set != null) {
            throw new IllegalArgumentException("set identifier not unique");
        }

        set = new AttributeSetEntity();
        set.setSet(identifier);
        set.setName(name);
        set.setDescription(description);

        set = attributeSetRepository.save(set);
        return set;
    }

    public AttributeSetEntity updateAttributeSet(
            String identifier,
            String name, String description) throws NoSuchAttributeSetException {
        if (!StringUtils.hasText(identifier)) {
            throw new IllegalArgumentException("empty set identifier");
        }

        AttributeSetEntity set = attributeSetRepository.findBySet(identifier);
        if (set == null) {
            throw new NoSuchAttributeSetException();
        }

        set.setName(name);
        set.setDescription(description);

        set = attributeSetRepository.save(set);
        return set;
    }

    public AttributeSetEntity deleteAttributeSet(String identifier) {
        AttributeSetEntity set = attributeSetRepository.findBySet(identifier);
        if (set != null) {
            List<AttributeEntity> attributes = attributeRepository.findBySet(identifier);
            attributeRepository.deleteAll(attributes);

            attributeSetRepository.delete(set);
        }

        return set;
    }

    /*
     * Attribute entities
     */

    @Transactional(readOnly = true)
    public AttributeEntity findAttribute(String set, String key) {
        return attributeRepository.findBySetAndKey(set, key);
    }

    @Transactional(readOnly = true)
    public AttributeEntity getAttribute(String set, String key) throws NoSuchAttributeException {
        AttributeEntity a = attributeRepository.findBySetAndKey(set, key);
        if (a == null) {
            throw new NoSuchAttributeException();
        }

        return a;
    }

    @Transactional(readOnly = true)
    public List<AttributeEntity> listAttributes(String set) throws NoSuchAttributeSetException {
        return attributeRepository.findBySet(set);
    }

    public AttributeEntity addAttribute(
            String set, String key,
            AttributeType type, Boolean isMultiple,
            String name, String description) throws NoSuchAttributeSetException {
        if (!StringUtils.hasText(key)) {
            throw new IllegalArgumentException("empty attribute key");
        }

        AttributeSetEntity as = getAttributeSet(set);
        boolean multiple = isMultiple != null ? isMultiple.booleanValue() : false;
        String attrType = type != null ? type.getValue() : AttributeType.STRING.getValue();

        AttributeEntity a = attributeRepository.findBySetAndKey(set, key);
        if (a != null) {
            throw new IllegalArgumentException("attribute key not unique");
        }

        a = new AttributeEntity();
        a.setSet(as.getSet());
        a.setKey(key);
        a.setType(attrType);
        a.setMultiple(multiple);

        a.setName(name);
        a.setDescription(description);

        a = attributeRepository.save(a);
        return a;
    }

    public AttributeEntity updateAttribute(
            String set, String key,
            AttributeType type, Boolean isMultiple,
            String name, String description) throws NoSuchAttributeException, NoSuchAttributeSetException {
        if (!StringUtils.hasText(key)) {
            throw new IllegalArgumentException("empty attribute key");
        }

        boolean multiple = isMultiple != null ? isMultiple.booleanValue() : false;
        String attrType = type != null ? type.getValue() : AttributeType.STRING.getValue();

        AttributeEntity a = attributeRepository.findBySetAndKey(set, key);
        if (a == null) {
            throw new NoSuchAttributeException();
        }

        a.setType(attrType);
        a.setMultiple(multiple);

        a.setName(name);
        a.setDescription(description);

        a = attributeRepository.save(a);
        return a;
    }

    public AttributeEntity deleteAttribute(String set, String key) throws NoSuchAttributeSetException {

        AttributeEntity a = attributeRepository.findBySetAndKey(set, key);
        if (a != null) {
            attributeRepository.delete(a);
        }

        return a;
    }
}
