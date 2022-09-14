package it.smartcommunitylab.aac.core.service;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.core.persistence.IdentityProviderEntity;
import it.smartcommunitylab.aac.core.persistence.IdentityProviderEntityRepository;

@Service
@Transactional
public class IdentityProviderEntityService implements ProviderEntityService<IdentityProviderEntity> {

    private final IdentityProviderEntityRepository providerRepository;

    public IdentityProviderEntityService(IdentityProviderEntityRepository providerRepository) {
        Assert.notNull(providerRepository, "provider repository is mandatory");
        this.providerRepository = providerRepository;
    }

    @Transactional(readOnly = true)
    public List<IdentityProviderEntity> listProviders() {
        return providerRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<IdentityProviderEntity> listProvidersByAuthority(String authority) {
        return providerRepository.findByAuthority(authority);
    }

    @Transactional(readOnly = true)
    public List<IdentityProviderEntity> listProvidersByRealm(String realm) {
        return providerRepository.findByRealm(realm);
    }

    @Transactional(readOnly = true)
    public List<IdentityProviderEntity> listProvidersByAuthorityAndRealm(String authority, String realm) {
        return providerRepository.findByAuthorityAndRealm(authority, realm);
    }

    @Transactional(readOnly = true)
    public IdentityProviderEntity findProvider(String providerId) {
        return providerRepository.findByProviderId(providerId);
    }

    @Transactional(readOnly = true)
    public IdentityProviderEntity getProvider(String providerId) throws NoSuchProviderException {
        IdentityProviderEntity p = providerRepository.findByProviderId(providerId);
        if (p == null) {
            throw new NoSuchProviderException();
        }

        return p;
    }

    public IdentityProviderEntity createProvider() {
        // generate random
        String id = RandomStringUtils.randomAlphanumeric(8);

        IdentityProviderEntity pe = providerRepository.findByProviderId(id);
        if (pe != null) {
            // re generate longer
            id = RandomStringUtils.randomAlphanumeric(10);
        }

        IdentityProviderEntity p = new IdentityProviderEntity(id);
        return p;
    }

    public IdentityProviderEntity saveProvider(
            String providerId,
            IdentityProviderEntity reg, Map<String, Serializable> configurationMap) {

        IdentityProviderEntity p = providerRepository.findByProviderId(providerId);
        if (p == null) {
            p = new IdentityProviderEntity();
            p.setProviderId(providerId);
            p.setAuthority(reg.getAuthority());
            p.setRealm(reg.getRealm());
        }

        p.setEnabled(reg.isEnabled());
        p.setName(reg.getName());
        p.setDescription(reg.getDescription());
        p.setIcon(reg.getIcon());
        p.setPersistence(reg.getPersistence());
        p.setLinkable(reg.isLinkable());
        p.setEvents(reg.getEvents());
        p.setPosition(reg.getPosition());
        p.setConfigurationMap(configurationMap);
        p.setHookFunctions(reg.getHookFunctions());

        p = providerRepository.saveAndFlush(p);

        return p;
    }

    public void deleteProvider(String providerId) {
        IdentityProviderEntity p = providerRepository.findByProviderId(providerId);
        if (p != null) {
            providerRepository.delete(p);
        }
    }

}
