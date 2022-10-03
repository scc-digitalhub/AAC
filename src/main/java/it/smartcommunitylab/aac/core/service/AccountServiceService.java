package it.smartcommunitylab.aac.core.service;

import java.util.Map;
import java.util.stream.Collectors;

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
            if (StringUtils.hasText(name)) {
                name = Jsoup.clean(name, Safelist.none());
            }
            pe.setName(name);

            Map<String, String> titleMap = null;
            if (reg.getTitleMap() != null) {
                // cleanup every field via safelist
                titleMap = reg.getTitleMap().entrySet().stream()
                        .filter(e -> e.getValue() != null)
                        .map(e -> Map.entry(e.getKey(), Jsoup.clean(e.getValue(), Safelist.none())))
                        .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
            }
            pe.setTitleMap(titleMap);

            Map<String, String> descriptionMap = null;
            if (reg.getDescriptionMap() != null) {
                // cleanup every field via safelist
                descriptionMap = reg.getDescriptionMap().entrySet().stream()
                        .filter(e -> e.getValue() != null)
                        .map(e -> Map.entry(e.getKey(), Jsoup.clean(e.getValue(), Safelist.none())))
                        .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
            }
            pe.setDescriptionMap(descriptionMap);

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
            cp.setTitleMap(pe.getTitleMap());
            cp.setDescriptionMap(pe.getDescriptionMap());

            cp.setRepositoryId(pe.getRepositoryId());

            cp.setEnabled(pe.isEnabled());
            cp.setConfiguration(pe.getConfigurationMap());

            return cp;
        }
    }
}
