package it.smartcommunitylab.aac.core.service;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.core.persistence.AccountServiceEntity;
import it.smartcommunitylab.aac.core.persistence.AccountServiceEntityRepository;

@Service
@Transactional
public class AccountServiceEntityService implements ProviderEntityService<AccountServiceEntity> {

    private final AccountServiceEntityRepository providerRepository;

    public AccountServiceEntityService(AccountServiceEntityRepository providerRepository) {
        Assert.notNull(providerRepository, "provider repository is mandatory");
        this.providerRepository = providerRepository;
    }

    @Transactional(readOnly = true)
    public List<AccountServiceEntity> listProviders() {
        return providerRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<AccountServiceEntity> listProvidersByAuthority(String authority) {
        return providerRepository.findByAuthority(authority);
    }

    @Transactional(readOnly = true)
    public List<AccountServiceEntity> listProvidersByRealm(String realm) {
        return providerRepository.findByRealm(realm);
    }

    @Transactional(readOnly = true)
    public List<AccountServiceEntity> listProvidersByAuthorityAndRealm(String authority, String realm) {
        return providerRepository.findByAuthorityAndRealm(authority, realm);
    }

    @Transactional(readOnly = true)
    public AccountServiceEntity findProvider(String providerId) {
        return providerRepository.findByProviderId(providerId);
    }

    @Transactional(readOnly = true)
    public AccountServiceEntity getProvider(String providerId) throws NoSuchProviderException {
        AccountServiceEntity p = providerRepository.findByProviderId(providerId);
        if (p == null) {
            throw new NoSuchProviderException();
        }

        return p;
    }

    public AccountServiceEntity createProvider() {
        // generate random
        String id = RandomStringUtils.randomAlphanumeric(8);

        AccountServiceEntity pe = providerRepository.findByProviderId(id);
        if (pe != null) {
            // re generate longer
            id = RandomStringUtils.randomAlphanumeric(10);
        }

        AccountServiceEntity p = new AccountServiceEntity(id);
        return p;
    }

    public AccountServiceEntity saveProvider(
            String providerId,
            AccountServiceEntity reg, Map<String, Serializable> configurationMap) {

        AccountServiceEntity p = providerRepository.findByProviderId(providerId);
        if (p == null) {
            p = new AccountServiceEntity();
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
        AccountServiceEntity p = providerRepository.findByProviderId(providerId);
        if (p != null) {
            providerRepository.delete(p);
        }
    }

}
