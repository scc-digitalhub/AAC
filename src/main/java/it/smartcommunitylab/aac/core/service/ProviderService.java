package it.smartcommunitylab.aac.core.service;

import java.util.List;
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

    public List<ProviderEntity> listProviders() {
        return providerRepository.findAll();
    }

    public List<ProviderEntity> listProvidersByAuthority(String authority) {
        return providerRepository.findByAuthority(authority);
    }

    public List<ProviderEntity> listProvidersByRealm(String realm) {
        return providerRepository.findByRealm(realm);
    }

    public List<ProviderEntity> listProvidersByRealmAndType(String realm, String type) {
        return providerRepository.findByRealmAndType(realm, type);
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
            String name,
            Map<String, Object> configurationMap) {

        ProviderEntity p = new ProviderEntity();
        p.setAuthority(authority);
        p.setProviderId(providerId);
        p.setRealm(realm);
        p.setType(type);
        // disabled by default, need to be explicitely enabled
        p.setEnabled(false);
        p.setName(name);
        p.setConfigurationMap(configurationMap);

        p = providerRepository.save(p);

        return p;
    }

    public ProviderEntity updateProvider(
            String providerId,
            boolean enabled, String name,
            Map<String, Object> configurationMap) throws NoSuchProviderException {

        ProviderEntity p = providerRepository.findByProviderId(providerId);
        if (p == null) {
            throw new NoSuchProviderException();
        }

        // we update both status and configuration at the same time
        p.setEnabled(enabled);
        p.setName(name);
        p.setConfigurationMap(configurationMap);
        p = providerRepository.save(p);

        return p;

    }

    public void deleteProvider(String providerId) {
        ProviderEntity p = providerRepository.findByProviderId(providerId);
        if (p != null) {
            providerRepository.delete(p);
        }
    }

}
