package it.smartcommunitylab.aac.core.service;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.core.persistence.AttributeProviderEntity;
import it.smartcommunitylab.aac.core.persistence.AttributeProviderEntityRepository;

@Service
@Transactional
public class AttributeProviderEntityService {

    private final AttributeProviderEntityRepository providerRepository;

    public AttributeProviderEntityService(AttributeProviderEntityRepository providerRepository) {
        Assert.notNull(providerRepository, "provider repository is mandatory");
        this.providerRepository = providerRepository;
    }

    @Transactional(readOnly = true)
    public List<AttributeProviderEntity> listAttributeProviders() {
        return providerRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<AttributeProviderEntity> listAttributeProvidersByAuthority(String authority) {
        return providerRepository.findByAuthority(authority);
    }

    @Transactional(readOnly = true)
    public List<AttributeProviderEntity> listAttributeProvidersByRealm(String realm) {
        return providerRepository.findByRealm(realm);
    }

    @Transactional(readOnly = true)
    public List<AttributeProviderEntity> listAttributeProvidersByAuthorityAndRealm(String authority, String realm) {
        return providerRepository.findByAuthorityAndRealm(authority, realm);
    }

    @Transactional(readOnly = true)
    public AttributeProviderEntity findAttributeProvider(String providerId) {
        return providerRepository.findByProviderId(providerId);
    }

    @Transactional(readOnly = true)
    public AttributeProviderEntity getAttributeProvider(String providerId) throws NoSuchProviderException {
        AttributeProviderEntity p = providerRepository.findByProviderId(providerId);
        if (p == null) {
            throw new NoSuchProviderException();
        }

        return p;
    }

    public AttributeProviderEntity createAttributeProvider() {
        // TODO ensure unique on multi node deploy: replace with idGenerator
        // (given that UUID is derived from timestamp we consider this safe enough)
        String id = UUID.randomUUID().toString();
        AttributeProviderEntity p = new AttributeProviderEntity(id);
        return p;
    }

    public AttributeProviderEntity addAttributeProvider(
            String authority,
            String providerId,
            String realm,
            String name, String description,
            String persistence, String events,
            Collection<String> attributeSets,
            Map<String, Serializable> configurationMap) {

        AttributeProviderEntity p = new AttributeProviderEntity();
        p.setAuthority(authority);
        p.setProviderId(providerId);
        p.setRealm(realm);
        // disabled by default, need to be explicitly enabled
        p.setEnabled(false);
        p.setName(name);
        p.setDescription(description);
        p.setPersistence(persistence);
        p.setEvents(events);
        p.setAttributeSets(StringUtils.collectionToCommaDelimitedString(attributeSets));
        p.setConfigurationMap(configurationMap);

        p = providerRepository.saveAndFlush(p);

        return p;
    }

    public AttributeProviderEntity updateAttributeProvider(
            String providerId,
            boolean enabled,
            String name, String description,
            String persistence, String events,
            Collection<String> attributeSets,
            Map<String, Serializable> configurationMap) throws NoSuchProviderException {

        AttributeProviderEntity p = providerRepository.findByProviderId(providerId);
        if (p == null) {
            throw new NoSuchProviderException();
        }

        // update props
        p.setName(name);
        p.setDescription(description);
        p.setPersistence(persistence);
        p.setEvents(events);
        p.setAttributeSets(StringUtils.collectionToCommaDelimitedString(attributeSets));

        // we update both status and configuration at the same time
        p.setEnabled(enabled);
        p.setConfigurationMap(configurationMap);
        p = providerRepository.saveAndFlush(p);

        return p;

    }

    public void deleteAttributeProvider(String providerId) {
        AttributeProviderEntity p = providerRepository.findByProviderId(providerId);
        if (p != null) {
            providerRepository.delete(p);
        }
    }

}
