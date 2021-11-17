package it.smartcommunitylab.aac.internal.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.core.model.Attribute;
import it.smartcommunitylab.aac.internal.persistence.InternalAttributeEntity;
import it.smartcommunitylab.aac.internal.persistence.InternalAttributeEntityRepository;

@Service
@Transactional
public class InternalAttributeEntityService {

    private final InternalAttributeEntityRepository attributeRepository;

    public InternalAttributeEntityService(InternalAttributeEntityRepository attributeRepository) {
        Assert.notNull(attributeRepository, "attribute repository is mandatory");
        this.attributeRepository = attributeRepository;
    }

    /*
     * crud
     */

    public List<InternalAttributeEntity> setAttributes(
            String provider,
            String subjectId,
            String setId,
            Collection<Attribute> attributes) {
        List<InternalAttributeEntity> result = new ArrayList<>();

        // we sync attributes with those received by deleting missing
        List<InternalAttributeEntity> oldAttributes = attributeRepository.findByProviderAndSubjectIdAndSet(provider,
                subjectId, setId);

        List<InternalAttributeEntity> toRemove = new ArrayList<>();
        toRemove.addAll(oldAttributes);

        for (Attribute attr : attributes) {
            InternalAttributeEntity a = addOrUpdateAttribute(provider, subjectId, setId, attr);
            if (a != null) {
                result.add(a);
                if (toRemove.contains(a)) {
                    toRemove.remove(a);
                }
            }
        }

        // remove orphans
        attributeRepository.deleteAll(toRemove);

        return result;

    }

    public InternalAttributeEntity addOrUpdateAttribute(
            String provider,
            String subjectId, String setId,
            Attribute attr) {
        return addOrUpdateAttribute(provider, subjectId, setId,
                attr.getKey(), attr.getType().getValue(), attr.getValue());
    }

    public InternalAttributeEntity addOrUpdateAttribute(
            String provider,
            String subjectId, String setId,
            String key, String type, Serializable value) {
        InternalAttributeEntity a = attributeRepository.findByProviderAndSubjectIdAndSetAndKey(provider,
                subjectId, setId, key);
        if (a == null) {
            a = new InternalAttributeEntity();
            a.setProvider(provider);
            a.setSubjectId(subjectId);

            a.setSet(setId);
            a.setKey(key);
        }

        a.setType(type);
        a.setValue(value);
        a = attributeRepository.saveAndFlush(a);

        return a;
    }

    public InternalAttributeEntity addAttribute(
            String provider, String subjectId,
            String setId, String key, String type, String value) {

        InternalAttributeEntity a = attributeRepository.findByProviderAndSubjectIdAndSetAndKey(
                provider, subjectId,
                setId, key);
        if (a != null) {
            throw new IllegalArgumentException("duplicate key");
        }

        a = new InternalAttributeEntity();

        a = new InternalAttributeEntity();
        a.setProvider(provider);
        a.setSubjectId(subjectId);

        a.setSet(setId);
        a.setKey(key);

        a.setType(type);
        a.setValue(value);

        a = attributeRepository.save(a);

        return a;

    }

    public InternalAttributeEntity updateAttribute(
            String provider, String subjectId,
            String setId, String key, String type, String value) {
        InternalAttributeEntity a = attributeRepository.findByProviderAndSubjectIdAndSetAndKey(
                provider, subjectId,
                setId, key);
        if (a == null) {
            throw new NoSuchElementException();
        }

        a.setType(type);
        a.setValue(value);
        a = attributeRepository.saveAndFlush(a);

        return a;
    }

    public void deleteAttribute(
            String provider, String subjectId,
            String setId, String key) {

        InternalAttributeEntity a = attributeRepository.findByProviderAndSubjectIdAndSetAndKey(provider,
                subjectId, setId, key);
        if (a != null) {
            attributeRepository.delete(a);
        }

    }

    public void deleteAttribute(
            String provider, String subjectId,
            String setId) {

        List<InternalAttributeEntity> attributes = attributeRepository.findByProviderAndSubjectIdAndSet(provider,
                subjectId, setId);
        if (!attributes.isEmpty()) {
            attributeRepository.deleteAll(attributes);
        }

    }

    public void deleteAttributes(String provider, String subjectId) {
        List<InternalAttributeEntity> attributes = attributeRepository.findByProviderAndSubjectId(provider, subjectId);
        if (!attributes.isEmpty()) {
            attributeRepository.deleteAll(attributes);
        }

    }

    @Transactional(readOnly = true)
    public List<InternalAttributeEntity> findAttributes(String provider) {
        return attributeRepository.findByProvider(provider);
    }

    @Transactional(readOnly = true)
    public List<InternalAttributeEntity> findAttributes(String provider, String subjectId) {
        return attributeRepository.findByProviderAndSubjectId(provider, subjectId);
    }

    @Transactional(readOnly = true)
    public List<InternalAttributeEntity> findAttributes(String provider, String subjectId, String setId) {
        return attributeRepository.findByProviderAndSubjectIdAndSet(provider, subjectId, setId);
    }

    @Transactional(readOnly = true)
    public InternalAttributeEntity findAttribute(String provider, String subjectId, String setId, String key) {
        return attributeRepository.findByProviderAndSubjectIdAndSetAndKey(provider, subjectId, setId, key);
    }

}
