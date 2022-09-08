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
public class IdentityProviderEntityService {

    private final IdentityProviderEntityRepository providerRepository;

    public IdentityProviderEntityService(IdentityProviderEntityRepository providerRepository) {
        Assert.notNull(providerRepository, "provider repository is mandatory");
        this.providerRepository = providerRepository;
    }

    @Transactional(readOnly = true)
    public List<IdentityProviderEntity> listIdentityProviders() {
        return providerRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<IdentityProviderEntity> listIdentityProvidersByAuthority(String authority) {
        return providerRepository.findByAuthority(authority);
    }

    @Transactional(readOnly = true)
    public List<IdentityProviderEntity> listIdentityProvidersByRealm(String realm) {
        return providerRepository.findByRealm(realm);
    }

    @Transactional(readOnly = true)
    public List<IdentityProviderEntity> listIdentityProvidersByAuthorityAndRealm(String authority, String realm) {
        return providerRepository.findByAuthorityAndRealm(authority, realm);
    }

    @Transactional(readOnly = true)
    public IdentityProviderEntity findIdentityProvider(String providerId) {
        return providerRepository.findByProviderId(providerId);
    }

    @Transactional(readOnly = true)
    public IdentityProviderEntity getIdentityProvider(String providerId) throws NoSuchProviderException {
        IdentityProviderEntity p = providerRepository.findByProviderId(providerId);
        if (p == null) {
            throw new NoSuchProviderException();
        }

        return p;
    }

    public IdentityProviderEntity createIdentityProvider() {
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

    public IdentityProviderEntity addIdentityProvider(
            String authority,
            String providerId,
            String realm,
            String name, String description, String icon,
            String persistence, String events, Integer position,
            Map<String, Serializable> configurationMap,
            Map<String, String> hookFunctions) {

        IdentityProviderEntity p = new IdentityProviderEntity();
        p.setAuthority(authority);
        p.setProviderId(providerId);
        p.setRealm(realm);
        // disabled by default, need to be explicitly enabled
        p.setEnabled(false);
        p.setName(name);
        p.setDescription(description);
        p.setIcon(icon);
        p.setPersistence(persistence);
        p.setLinkable(true);
        p.setEvents(events);
        p.setPosition(position);
        p.setConfigurationMap(configurationMap);
        p.setHookFunctions(hookFunctions);

        p = providerRepository.saveAndFlush(p);

        return p;
    }

    public IdentityProviderEntity updateIdentityProvider(
            String providerId,
            boolean enabled, boolean linkable,
            String name, String description, String icon,
            String persistence, String events, Integer position,
            Map<String, Serializable> configurationMap,
            Map<String, String> hookFunctions) throws NoSuchProviderException {

        IdentityProviderEntity p = providerRepository.findByProviderId(providerId);
        if (p == null) {
            throw new NoSuchProviderException();
        }

        // update props
        p.setName(name);
        p.setDescription(description);
        p.setIcon(icon);
        p.setPersistence(persistence);
        p.setLinkable(linkable);
        p.setEvents(events);
        p.setPosition(position);
        p.setHookFunctions(hookFunctions);

        // we update both status and configuration at the same time
        p.setEnabled(enabled);
        p.setConfigurationMap(configurationMap);
        p = providerRepository.saveAndFlush(p);

        return p;

    }

    public void deleteIdentityProvider(String providerId) {
        IdentityProviderEntity p = providerRepository.findByProviderId(providerId);
        if (p != null) {
            providerRepository.delete(p);
        }
    }

}
