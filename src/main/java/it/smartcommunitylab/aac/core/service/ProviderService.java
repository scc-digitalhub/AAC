package it.smartcommunitylab.aac.core.service;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.core.persistence.ProviderEntity;
import it.smartcommunitylab.aac.core.persistence.ProviderEntityRepository;

@Service
public class ProviderService {

    private final ProviderEntityRepository providerRepository;

    public ProviderService(ProviderEntityRepository providerRepository) {
        Assert.notNull(providerRepository, "provider repository is mandatory");
        this.providerRepository = providerRepository;
    }

    public ProviderEntity fetchProvider(String providerId) {
        return providerRepository.findByProviderId(providerId);
    }

    public ProviderEntity getProvider(String providerId) throws NoSuchProviderException {
        ProviderEntity p = providerRepository.findByProviderId(providerId);
        if (p == null) {
            throw new NoSuchProviderException();
        }

        return p;
    }

    public ProviderEntity addProvider(
            String authority,
            String providerId,
            String realm,
            String type,
            Map<String, String> configurationMap) {

        ProviderEntity p = new ProviderEntity();
        p.setAuthority(authority);
        p.setProviderId(providerId);
        p.setRealm(realm);
        p.setType(type);
        // disabled by default, need to be explicitely enabled
        p.setEnabled(false);
        p.setConfigurationMap(configurationMap);

        p = providerRepository.save(p);

        return p;
    }

    public ProviderEntity updateProvider(
            String providerId,
            boolean enabled,
            Map<String, String> configurationMap) throws NoSuchProviderException {

        ProviderEntity p = providerRepository.findByProviderId(providerId);
        if (p == null) {
            throw new NoSuchProviderException();
        }

        // we update both status and configuration at the same time
        p.setEnabled(enabled);
        p.setConfigurationMap(configurationMap);
        p = providerRepository.save(p);

        return p;

    }

    public void deleteProvider(String providerId) {
        ProviderEntity p = providerRepository.findByProviderId(providerId);
        if (p == null) {
            providerRepository.delete(p);
        }
    }

}
