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
import it.smartcommunitylab.aac.core.model.ConfigurableAccountService;
import it.smartcommunitylab.aac.core.persistence.AccountServiceEntity;
import it.smartcommunitylab.aac.core.provider.ConfigurationProvider;

@Service
@Transactional
public class AccountServiceService
        extends ConfigurableProviderService<ConfigurableAccountService, AccountServiceEntity> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private AccountServiceAuthorityService authorityService;

    public AccountServiceService(AccountServiceEntityService providerService) {
        super(providerService);

        // set converters
        setConfigConverter(new AccountServiceConfigConverter());
        setEntityConverter(new AccountServiceEntityConverter());

        // create system services
        // we expect no client/services in global+system realm!
        // note: we let registration with authorities to bootstrap

        // enable internal for system by default
        ConfigurableAccountService internalConfig = new ConfigurableAccountService(
                SystemKeys.AUTHORITY_INTERNAL, SystemKeys.AUTHORITY_INTERNAL,
                SystemKeys.REALM_SYSTEM);
        logger.debug("configure internal service for system realm");
        systemProviders.put(internalConfig.getProvider(), internalConfig);

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
    public void setAuthorityService(AccountServiceAuthorityService authorityService) {
        this.authorityService = authorityService;
    }

    @Override
    protected ConfigurationProvider<?, ?, ?> getConfigurationProvider(String authority)
            throws NoSuchAuthorityException {
        return authorityService.getAuthority(authority).getConfigurationProvider();
    }

    class AccountServiceConfigConverter implements Converter<ConfigurableAccountService, AccountServiceEntity> {

        @Override
        public AccountServiceEntity convert(ConfigurableAccountService reg) {
            AccountServiceEntity pe = new AccountServiceEntity();

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

            pe.setEnabled(reg.isEnabled());
            pe.setConfigurationMap(reg.getConfiguration());

            return pe;
        }

    }

    class AccountServiceEntityConverter implements Converter<AccountServiceEntity, ConfigurableAccountService> {

        @Override
        public ConfigurableAccountService convert(AccountServiceEntity pe) {
            ConfigurableAccountService cp = new ConfigurableAccountService(pe.getAuthority(), pe.getProviderId(),
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
