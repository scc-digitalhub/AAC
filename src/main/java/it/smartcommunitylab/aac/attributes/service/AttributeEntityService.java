/*
 * Copyright 2023 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.attributes.service;

import it.smartcommunitylab.aac.attributes.persistence.AttributeEntity;
import it.smartcommunitylab.aac.attributes.persistence.AttributeEntityRepository;
import it.smartcommunitylab.aac.attributes.persistence.AttributeSetEntity;
import it.smartcommunitylab.aac.attributes.persistence.AttributeSetEntityRepository;
import it.smartcommunitylab.aac.common.NoSuchAttributeException;
import it.smartcommunitylab.aac.common.NoSuchAttributeSetException;
import it.smartcommunitylab.aac.model.AttributeType;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class AttributeEntityService {

    private final AttributeEntityRepository attributeRepository;
    private final AttributeSetEntityRepository attributeSetRepository;

    public AttributeEntityService(
        AttributeSetEntityRepository attributeSetRepository,
        AttributeEntityRepository attributeRepository
    ) {
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
    public List<AttributeSetEntity> listAttributeSets(String realm) {
        return attributeSetRepository.findByRealm(realm);
    }

    @Transactional(readOnly = true)
    public AttributeSetEntity findAttributeSet(String identifier) {
        return attributeSetRepository.findByIdentifier(identifier);
    }

    @Transactional(readOnly = true)
    public AttributeSetEntity getAttributeSet(String identifier) throws NoSuchAttributeSetException {
        AttributeSetEntity set = attributeSetRepository.findByIdentifier(identifier);
        if (set == null) {
            throw new NoSuchAttributeSetException();
        }

        return set;
    }

    public AttributeSetEntity addAttributeSet(String realm, String identifier, String name, String description) {
        if (!StringUtils.hasText(identifier)) {
            throw new IllegalArgumentException("empty set identifier");
        }

        AttributeSetEntity set = attributeSetRepository.findByIdentifier(identifier);
        if (set != null) {
            throw new IllegalArgumentException("set identifier not unique");
        }

        set = new AttributeSetEntity();
        set.setRealm(realm);
        set.setIdentifier(identifier);
        set.setName(name);
        set.setDescription(description);

        set = attributeSetRepository.save(set);
        return set;
    }

    public AttributeSetEntity updateAttributeSet(String identifier, String name, String description)
        throws NoSuchAttributeSetException {
        if (!StringUtils.hasText(identifier)) {
            throw new IllegalArgumentException("empty set identifier");
        }

        AttributeSetEntity set = attributeSetRepository.findByIdentifier(identifier);
        if (set == null) {
            throw new NoSuchAttributeSetException();
        }

        set.setName(name);
        set.setDescription(description);

        set = attributeSetRepository.save(set);
        return set;
    }

    public AttributeSetEntity deleteAttributeSet(String identifier) {
        AttributeSetEntity set = attributeSetRepository.findByIdentifier(identifier);
        if (set != null) {
            List<AttributeEntity> attributes = attributeRepository.findBySetOrderById(identifier);
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
        return attributeRepository.findBySetOrderById(set);
    }

    public AttributeEntity addAttribute(
        String set,
        String key,
        AttributeType type,
        Boolean isMultiple,
        String name,
        String description
    ) throws NoSuchAttributeSetException {
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
        a.setSet(as.getIdentifier());
        a.setKey(key);
        a.setType(attrType);
        a.setMultiple(multiple);

        a.setName(name);
        a.setDescription(description);

        a = attributeRepository.save(a);
        return a;
    }

    public AttributeEntity updateAttribute(
        String set,
        String key,
        AttributeType type,
        Boolean isMultiple,
        String name,
        String description
    ) throws NoSuchAttributeException, NoSuchAttributeSetException {
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
