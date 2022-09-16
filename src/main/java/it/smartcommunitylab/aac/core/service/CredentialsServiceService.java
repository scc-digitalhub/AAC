package it.smartcommunitylab.aac.core.service;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchAuthorityException;
import it.smartcommunitylab.aac.core.model.ConfigurableCredentialsService;
import it.smartcommunitylab.aac.core.persistence.CredentialsServiceEntity;
import it.smartcommunitylab.aac.core.provider.ConfigurationProvider;

@Service
@Transactional
public class CredentialsServiceService
        extends ConfigurableProviderService<ConfigurableCredentialsService, CredentialsServiceEntity> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private CredentialsServiceAuthorityService authorityService;

    public CredentialsServiceService(CredentialsServiceEntityService providerService) {
        super(providerService);

        // set converters
        this.setConfigConverter(new CredentialsServiceConfigConverter());
        this.setEntityConverter(new CredentialsServiceEntityConverter());

        // create system services
        // we expect no client/services in global+system realm!
        // note: we let registration with authorities to bootstrap

        // enable password for system by default
        ConfigurableCredentialsService passwordConfig = new ConfigurableCredentialsService(
                SystemKeys.AUTHORITY_PASSWORD, SystemKeys.AUTHORITY_PASSWORD,
                SystemKeys.REALM_SYSTEM);
        logger.debug("configure password service for system realm");
        systemProviders.put(passwordConfig.getProvider(), passwordConfig);
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
            pe.setProviderId(reg.getProvider());
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
                    pe.getProviderId(),
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
