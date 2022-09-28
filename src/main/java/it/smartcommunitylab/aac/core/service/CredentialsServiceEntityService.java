package it.smartcommunitylab.aac.core.service;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.core.persistence.CredentialsServiceEntity;
import it.smartcommunitylab.aac.core.persistence.CredentialsServiceEntityRepository;

@Service
@Transactional
public class CredentialsServiceEntityService implements ProviderEntityService<CredentialsServiceEntity> {

    private final CredentialsServiceEntityRepository providerRepository;

    public CredentialsServiceEntityService(CredentialsServiceEntityRepository providerRepository) {
        Assert.notNull(providerRepository, "provider repository is mandatory");
        this.providerRepository = providerRepository;
    }

    @Transactional(readOnly = true)
    public List<CredentialsServiceEntity> listProviders() {
        return providerRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<CredentialsServiceEntity> listProvidersByAuthority(String authority) {
        return providerRepository.findByAuthority(authority);
    }

    @Transactional(readOnly = true)
    public List<CredentialsServiceEntity> listProvidersByRealm(String realm) {
        return providerRepository.findByRealm(realm);
    }

    @Transactional(readOnly = true)
    public List<CredentialsServiceEntity> listProvidersByAuthorityAndRealm(String authority, String realm) {
        return providerRepository.findByAuthorityAndRealm(authority, realm);
    }

    @Transactional(readOnly = true)
    public CredentialsServiceEntity findProvider(String providerId) {
        return providerRepository.findByProviderId(providerId);
    }

    @Transactional(readOnly = true)
    public CredentialsServiceEntity getProvider(String providerId) throws NoSuchProviderException {
        CredentialsServiceEntity p = providerRepository.findByProviderId(providerId);
        if (p == null) {
            throw new NoSuchProviderException();
        }

        return p;
    }

    public CredentialsServiceEntity createProvider() {
        // generate random
        String id = RandomStringUtils.randomAlphanumeric(8);

        CredentialsServiceEntity pe = providerRepository.findByProviderId(id);
        if (pe != null) {
            // re generate longer
            id = RandomStringUtils.randomAlphanumeric(10);
        }

        CredentialsServiceEntity p = new CredentialsServiceEntity(id);
        return p;
    }

    public CredentialsServiceEntity saveProvider(
            String providerId,
            CredentialsServiceEntity reg, Map<String, Serializable> configurationMap) {

        CredentialsServiceEntity p = providerRepository.findByProviderId(providerId);
        if (p == null) {
            p = new CredentialsServiceEntity();
            p.setProviderId(providerId);
            p.setAuthority(reg.getAuthority());
            p.setRealm(reg.getRealm());
        }

        p.setEnabled(reg.isEnabled());
        p.setName(reg.getName());
        p.setDescription(reg.getDescription());
        p.setConfigurationMap(configurationMap);

        p = providerRepository.saveAndFlush(p);

        return p;
    }

    public void deleteProvider(String providerId) {
        CredentialsServiceEntity p = providerRepository.findByProviderId(providerId);
        if (p != null) {
            providerRepository.delete(p);
        }
    }

}
