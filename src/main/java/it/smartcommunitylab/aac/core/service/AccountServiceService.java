package it.smartcommunitylab.aac.core.service;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.common.NoSuchAuthorityException;
import it.smartcommunitylab.aac.core.model.ConfigurableAccountService;
import it.smartcommunitylab.aac.core.persistence.AccountServiceEntity;
import it.smartcommunitylab.aac.core.provider.ConfigurationProvider;

@Service
@Transactional
public class AccountServiceService
        extends ConfigurableProviderService<ConfigurableAccountService, AccountServiceEntity> {

    private AccountServiceAuthorityService authorityService;

    public AccountServiceService(ConfigurableProviderEntityService<AccountServiceEntity> providerService) {
        super(providerService);

        // set converters
        setConfigConverter(new AccountServiceConfigConverter());
        setEntityConverter(new AccountServiceEntityConverter());

        // create system services
        // internal for system is exposed by internal idp by default
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

    class AccountServiceEntityConverter implements Converter<AccountServiceEntity, ConfigurableAccountService> {

        @Override
        public ConfigurableAccountService convert(AccountServiceEntity pe) {
            ConfigurableAccountService cp = new ConfigurableAccountService(pe.getAuthority(), pe.getProvider(),
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
