package it.smartcommunitylab.aac.core.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import it.smartcommunitylab.aac.common.NoSuchAuthorityException;
import it.smartcommunitylab.aac.core.model.ConfigurableAttributeProvider;
import it.smartcommunitylab.aac.core.persistence.AttributeProviderEntity;
import it.smartcommunitylab.aac.core.provider.ConfigurationProvider;

@Service
@Transactional
public class AttributeProviderService
        extends ConfigurableProviderService<ConfigurableAttributeProvider, AttributeProviderEntity> {

    private AttributeProviderAuthorityService authorityService;

    public AttributeProviderService(AttributeProviderEntityService providerService) {
        super(providerService);
    }

    @Autowired
    public void setAuthorityService(AttributeProviderAuthorityService authorityService) {
        this.authorityService = authorityService;
    }

    @Override
    protected ConfigurationProvider<?, ?, ?> getConfigurationProvider(String authority)
            throws NoSuchAuthorityException {
        return authorityService.getAuthority(authority).getConfigurationProvider();
    }

}
