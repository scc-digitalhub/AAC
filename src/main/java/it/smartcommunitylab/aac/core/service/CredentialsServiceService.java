package it.smartcommunitylab.aac.core.service;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.common.NoSuchAuthorityException;
import it.smartcommunitylab.aac.core.model.ConfigurableCredentialsService;
import it.smartcommunitylab.aac.core.persistence.CredentialsServiceEntity;
import it.smartcommunitylab.aac.core.provider.ConfigurationProvider;

@Service
@Transactional
public class CredentialsServiceService
        extends ConfigurableProviderService<ConfigurableCredentialsService, CredentialsServiceEntity> {

    private CredentialsServiceAuthorityService authorityService;

    public CredentialsServiceService(ConfigurableProviderEntityService<CredentialsServiceEntity> providerService) {
        super(providerService);

        // set converters
        this.setConfigConverter(new CredentialsServiceConfigConverter());
        this.setEntityConverter(new CredentialsServiceEntityConverter());

        // nothing to initialize for system because password service derives from
        // password idp
    }

    @Autowired
    public void setAuthorityService(CredentialsServiceAuthorityService authorityService) {
        this.authorityService = authorityService;
    }

    @Override
    protected ConfigurationProvider<?, ?, ?> getConfigurationProvider(String authority)
            throws NoSuchAuthorityException {
        return authorityService.getAuthority(authority).getConfigurationProvider();
    }

    class CredentialsServiceConfigConverter
            implements Converter<ConfigurableCredentialsService, CredentialsServiceEntity> {

        @Override
        public CredentialsServiceEntity convert(ConfigurableCredentialsService reg) {
            CredentialsServiceEntity pe = new CredentialsServiceEntity();

            pe.setAuthority(reg.getAuthority());
            pe.setProvider(reg.getProvider());
            pe.setRealm(reg.getRealm());

            String name = reg.getName();
            String description = reg.getDescription();
            if (StringUtils.hasText(name)) {
                name = Jsoup.clean(name, Safelist.none());
            }
            if (StringUtils.hasText(description)) {
                description = Jsoup.clean(description, Safelist.none());
            }

            pe.setName(name);
            pe.setDescription(description);

            pe.setRepositoryId(reg.getRepositoryId());
            pe.setConfigurationMap(reg.getConfiguration());
            pe.setEnabled(reg.isEnabled());

            return pe;
        }

    }

    class CredentialsServiceEntityConverter
            implements Converter<CredentialsServiceEntity, ConfigurableCredentialsService> {

        @Override
        public ConfigurableCredentialsService convert(CredentialsServiceEntity pe) {
            ConfigurableCredentialsService cp = new ConfigurableCredentialsService(pe.getAuthority(),
                    pe.getProvider(),
                    pe.getRealm());

            cp.setName(pe.getName());
            cp.setDescription(pe.getDescription());

            cp.setRepositoryId(pe.getRepositoryId());
            cp.setConfiguration(pe.getConfigurationMap());
            cp.setEnabled(pe.isEnabled());

            return cp;
        }
    }
}
