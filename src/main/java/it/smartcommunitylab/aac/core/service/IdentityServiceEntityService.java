package it.smartcommunitylab.aac.core.service;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.core.persistence.IdentityServiceEntity;
import it.smartcommunitylab.aac.core.persistence.IdentityServiceEntityRepository;

@Service
@Transactional
public class IdentityServiceEntityService implements ProviderEntityService<IdentityServiceEntity> {

    private final IdentityServiceEntityRepository providerRepository;

    public IdentityServiceEntityService(IdentityServiceEntityRepository providerRepository) {
        Assert.notNull(providerRepository, "provider repository is mandatory");
        this.providerRepository = providerRepository;
    }

    @Transactional(readOnly = true)
    public List<IdentityServiceEntity> listProviders() {
        return providerRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<IdentityServiceEntity> listProvidersByAuthority(String authority) {
        return providerRepository.findByAuthority(authority);
    }

    @Transactional(readOnly = true)
    public List<IdentityServiceEntity> listProvidersByRealm(String realm) {
        return providerRepository.findByRealm(realm);
    }

    @Transactional(readOnly = true)
    public List<IdentityServiceEntity> listProvidersByAuthorityAndRealm(String authority, String realm) {
        return providerRepository.findByAuthorityAndRealm(authority, realm);
    }

    @Transactional(readOnly = true)
    public IdentityServiceEntity findProvider(String providerId) {
        return providerRepository.findByProviderId(providerId);
    }

    @Transactional(readOnly = true)
    public IdentityServiceEntity getProvider(String providerId) throws NoSuchProviderException {
        IdentityServiceEntity p = providerRepository.findByProviderId(providerId);
        if (p == null) {
            throw new NoSuchProviderException();
        }

        return p;
    }

    public IdentityServiceEntity createProvider() {
        // generate random
        String id = RandomStringUtils.randomAlphanumeric(8);

        IdentityServiceEntity pe = providerRepository.findByProviderId(id);
        if (pe != null) {
            // re generate longer
            id = RandomStringUtils.randomAlphanumeric(10);
        }

        IdentityServiceEntity p = new IdentityServiceEntity(id);
        return p;
    }

    public IdentityServiceEntity saveProvider(
            String providerId,
            IdentityServiceEntity reg, Map<String, Serializable> configurationMap) {

        IdentityServiceEntity p = providerRepository.findByProviderId(providerId);
        if (p == null) {
            p = new IdentityServiceEntity();
            p.setProviderId(providerId);
            p.setAuthority(reg.getAuthority());
            p.setRealm(reg.getRealm());
        }

        p.setEnabled(reg.isEnabled());
        p.setName(reg.getName());
        p.setDescription(reg.getDescription());
        p.setConfigurationMap(configurationMap);
        p.setRepositoryId(reg.getRepositoryId());

        p = providerRepository.saveAndFlush(p);

        return p;
    }

    public void deleteProvider(String providerId) {
        IdentityServiceEntity p = providerRepository.findByProviderId(providerId);
        if (p != null) {
            providerRepository.delete(p);
        }
    }

}
