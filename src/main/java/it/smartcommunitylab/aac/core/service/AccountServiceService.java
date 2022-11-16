package it.smartcommunitylab.aac.core.service;

import java.util.Map;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.core.authorities.AccountServiceAuthority;
import it.smartcommunitylab.aac.core.model.ConfigurableAccountProvider;
import it.smartcommunitylab.aac.core.persistence.AccountServiceEntity;

@Service
@Transactional
public class AccountServiceService
        extends
        ConfigurableProviderService<AccountServiceAuthority<?, ?, ?, ?>, ConfigurableAccountProvider, AccountServiceEntity> {

    public AccountServiceService(AccountServiceAuthorityService authorityService,
            ConfigurableProviderEntityService<AccountServiceEntity> providerService) {
        super(authorityService, providerService);

        // set converters
        setConfigConverter(new AccountServiceConfigConverter());
        setEntityConverter(new AccountServiceEntityConverter());
    }

    class AccountServiceConfigConverter implements Converter<ConfigurableAccountProvider, AccountServiceEntity> {

        @Override
        public AccountServiceEntity convert(ConfigurableAccountProvider reg) {
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

    class AccountServiceEntityConverter implements Converter<AccountServiceEntity, ConfigurableAccountProvider> {

        @Override
        public ConfigurableAccountProvider convert(AccountServiceEntity pe) {
            ConfigurableAccountProvider cp = new ConfigurableAccountProvider(pe.getAuthority(), pe.getProvider(),
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
