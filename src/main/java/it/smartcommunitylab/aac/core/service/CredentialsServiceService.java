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
            cp.setTitleMap(pe.getTitleMap());
            cp.setDescriptionMap(pe.getDescriptionMap());
            
            cp.setRepositoryId(pe.getRepositoryId());
            cp.setConfiguration(pe.getConfigurationMap());
            cp.setEnabled(pe.isEnabled());

            return cp;
        }
    }
}
