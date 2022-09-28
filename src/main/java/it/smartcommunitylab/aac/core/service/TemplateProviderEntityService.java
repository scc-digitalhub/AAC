package it.smartcommunitylab.aac.core.service;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.core.persistence.TemplateProviderEntity;
import it.smartcommunitylab.aac.core.persistence.TemplateProviderEntityRepository;

@Service
@Transactional
public class TemplateProviderEntityService implements ProviderEntityService<TemplateProviderEntity> {

    private final TemplateProviderEntityRepository providerRepository;

    public TemplateProviderEntityService(TemplateProviderEntityRepository providerRepository) {
        Assert.notNull(providerRepository, "provider repository is mandatory");
        this.providerRepository = providerRepository;
    }

    @Transactional(readOnly = true)
    public List<TemplateProviderEntity> listProviders() {
        return providerRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<TemplateProviderEntity> listProvidersByAuthority(String authority) {
        return providerRepository.findByAuthority(authority);
    }

    @Transactional(readOnly = true)
    public List<TemplateProviderEntity> listProvidersByRealm(String realm) {
        TemplateProviderEntity e = providerRepository.findByRealm(realm);
        if (e == null) {
            return Collections.emptyList();
        }

        return Collections.singletonList(e);
    }

    @Transactional(readOnly = true)
    public List<TemplateProviderEntity> listProvidersByAuthorityAndRealm(String authority, String realm) {
        return listProvidersByRealm(realm);
    }

    @Transactional(readOnly = true)
    public TemplateProviderEntity findProvider(String providerId) {
        return providerRepository.findByRealm(providerId);
    }

    @Transactional(readOnly = true)
    public TemplateProviderEntity getProvider(String providerId) throws NoSuchProviderException {
        TemplateProviderEntity p = providerRepository.findByRealm(providerId);
        if (p == null) {
            throw new NoSuchProviderException();
        }

        return p;
    }

    public TemplateProviderEntity createProvider() {
        TemplateProviderEntity p = new TemplateProviderEntity();
        return p;
    }

    public TemplateProviderEntity saveProvider(
            String providerId,
            TemplateProviderEntity reg, Map<String, Serializable> configurationMap) {
        // providerId is realm by design
        String realm = providerId;

        TemplateProviderEntity p = providerRepository.findByRealm(realm);
        if (p == null) {
            p = new TemplateProviderEntity();
            p.setAuthority(reg.getAuthority());
            p.setRealm(realm);
        }

        p.setEnabled(reg.isEnabled());
        p.setName(reg.getName());
        p.setDescription(reg.getDescription());
        p.setConfigurationMap(configurationMap);
        p.setLanguages(reg.getLanguages());
        p = providerRepository.saveAndFlush(p);

        return p;
    }

    public void deleteProvider(String providerId) {
        TemplateProviderEntity p = providerRepository.findByRealm(providerId);
        if (p != null) {
            providerRepository.delete(p);
        }
    }

}
