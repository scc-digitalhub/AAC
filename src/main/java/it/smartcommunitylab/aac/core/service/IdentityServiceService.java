package it.smartcommunitylab.aac.core.service;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.common.NoSuchAuthorityException;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityService;
import it.smartcommunitylab.aac.core.persistence.IdentityServiceEntity;
import it.smartcommunitylab.aac.core.provider.ConfigurationProvider;

@Service
@Transactional
public class IdentityServiceService
        extends ConfigurableProviderService<ConfigurableIdentityService, IdentityServiceEntity> {

    private IdentityServiceAuthorityService authorityService;

    public IdentityServiceService(ConfigurableProviderEntityService<IdentityServiceEntity> providerService) {
        super(providerService);

        // set converters
        setConfigConverter(new IdentityServiceConfigConverter());
        setEntityConverter(new IdentityServiceEntityConverter());

        // create system services
        // internal service is exposed by internal idp
//        // enable internal for system by default
//        ConfigurableIdentityService internalConfig = new ConfigurableIdentityService(
//                SystemKeys.AUTHORITY_INTERNAL, SystemKeys.AUTHORITY_INTERNAL,
//                SystemKeys.REALM_SYSTEM);
//        logger.debug("configure internal service for system realm");
//        systemProviders.put(internalConfig.getProvider(), internalConfig);

//        ConfigurableIdentityService oidcConfig = new ConfigurableIdentityService(
//                SystemKeys.AUTHORITY_OIDC, SystemKeys.AUTHORITY_OIDC,
//                SystemKeys.REALM_SYSTEM);
//        logger.debug("configure oidc service for system realm");
//        systemProviders.put(oidcConfig.getProvider(), oidcConfig);
//
//        ConfigurableIdentityService samlConfig = new ConfigurableIdentityService(
//                SystemKeys.AUTHORITY_SAML, SystemKeys.AUTHORITY_SAML,
//                SystemKeys.REALM_SYSTEM);
//        logger.debug("configure saml service for system realm");
//        systemProviders.put(samlConfig.getProvider(), samlConfig);
    }

    @Autowired
    public void setAuthorityService(IdentityServiceAuthorityService authorityService) {
        this.authorityService = authorityService;
    }

    @Override
    protected ConfigurationProvider<?, ?, ?> getConfigurationProvider(String authority)
            throws NoSuchAuthorityException {
        return authorityService.getAuthority(authority).getConfigurationProvider();
    }

    class IdentityServiceConfigConverter implements Converter<ConfigurableIdentityService, IdentityServiceEntity> {

        @Override
        public IdentityServiceEntity convert(ConfigurableIdentityService reg) {
            IdentityServiceEntity pe = new IdentityServiceEntity();

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

            pe.setEnabled(reg.isEnabled());
            pe.setConfigurationMap(reg.getConfiguration());

            return pe;
        }

    }

    class IdentityServiceEntityConverter implements Converter<IdentityServiceEntity, ConfigurableIdentityService> {

        @Override
        public ConfigurableIdentityService convert(IdentityServiceEntity pe) {
            ConfigurableIdentityService cp = new ConfigurableIdentityService(pe.getAuthority(), pe.getProvider(),
                    pe.getRealm());

            cp.setName(pe.getName());
            cp.setDescription(pe.getDescription());

            cp.setRepositoryId(pe.getRepositoryId());

            cp.setEnabled(pe.isEnabled());
            cp.setConfiguration(pe.getConfigurationMap());

            return cp;
        }
    }
}
