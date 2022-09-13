package it.smartcommunitylab.aac.core.service;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.core.persistence.AttributeProviderEntity;
import it.smartcommunitylab.aac.core.persistence.AttributeProviderEntityRepository;

@Service
@Transactional
public class AttributeProviderEntityService implements ProviderEntityService<AttributeProviderEntity> {

    private final AttributeProviderEntityRepository providerRepository;

    public AttributeProviderEntityService(AttributeProviderEntityRepository providerRepository) {
        Assert.notNull(providerRepository, "provider repository is mandatory");
        this.providerRepository = providerRepository;
    }

    @Transactional(readOnly = true)
    public List<AttributeProviderEntity> listProviders() {
        return providerRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<AttributeProviderEntity> listProvidersByAuthority(String authority) {
        return providerRepository.findByAuthority(authority);
    }

    @Transactional(readOnly = true)
    public List<AttributeProviderEntity> listProvidersByRealm(String realm) {
        return providerRepository.findByRealm(realm);
    }

    @Transactional(readOnly = true)
    public List<AttributeProviderEntity> listProvidersByAuthorityAndRealm(String authority, String realm) {
        return providerRepository.findByAuthorityAndRealm(authority, realm);
    }

    @Transactional(readOnly = true)
    public AttributeProviderEntity findProvider(String providerId) {
        return providerRepository.findByProviderId(providerId);
    }

    @Transactional(readOnly = true)
    public AttributeProviderEntity getProvider(String providerId) throws NoSuchProviderException {
        AttributeProviderEntity p = providerRepository.findByProviderId(providerId);
        if (p == null) {
            throw new NoSuchProviderException();
        }

        return p;
    }

    public AttributeProviderEntity createProvider() {
        // TODO ensure unique on multi node deploy: replace with idGenerator
        // (given that UUID is derived from timestamp we consider this safe enough)
        String id = UUID.randomUUID().toString();
        AttributeProviderEntity p = new AttributeProviderEntity(id);
        return p;
    }

    public AttributeProviderEntity saveProvider(
            String providerId,
            AttributeProviderEntity reg, Map<String, Serializable> configurationMap) {

        AttributeProviderEntity p = providerRepository.findByProviderId(providerId);
        if (p == null) {
            p = new AttributeProviderEntity();
            p.setProviderId(providerId);
            p.setAuthority(reg.getAuthority());
            p.setRealm(reg.getRealm());
        }

        p.setEnabled(reg.isEnabled());
        p.setName(reg.getName());
        p.setDescription(reg.getDescription());
        p.setPersistence(reg.getPersistence());
        p.setEvents(reg.getEvents());

        p.setAttributeSets(reg.getAttributeSets());
        p.setConfigurationMap(configurationMap);

        p = providerRepository.saveAndFlush(p);

        return p;
    }

    public void deleteProvider(String providerId) {
        AttributeProviderEntity p = providerRepository.findByProviderId(providerId);
        if (p != null) {
            providerRepository.delete(p);
        }
    }

}
