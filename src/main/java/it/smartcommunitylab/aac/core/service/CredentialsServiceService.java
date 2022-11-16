package it.smartcommunitylab.aac.core.service;

import java.util.Map;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.core.authorities.CredentialsServiceAuthority;
import it.smartcommunitylab.aac.core.model.ConfigurableCredentialsProvider;
import it.smartcommunitylab.aac.core.persistence.CredentialsServiceEntity;

@Service
@Transactional
public class CredentialsServiceService
        extends
        ConfigurableProviderService<CredentialsServiceAuthority<?, ?, ?, ?>, ConfigurableCredentialsProvider, CredentialsServiceEntity> {

    public CredentialsServiceService(CredentialsServiceAuthorityService authorityService,
            ConfigurableProviderEntityService<CredentialsServiceEntity> providerService) {
        super(authorityService, providerService);

        // set converters
        this.setConfigConverter(new CredentialsServiceConfigConverter());
        this.setEntityConverter(new CredentialsServiceEntityConverter());

        // nothing to initialize for system because password service derives from
        // password idp
    }

    class CredentialsServiceConfigConverter
            implements Converter<ConfigurableCredentialsProvider, CredentialsServiceEntity> {

        @Override
        public CredentialsServiceEntity convert(ConfigurableCredentialsProvider reg) {
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
            implements Converter<CredentialsServiceEntity, ConfigurableCredentialsProvider> {

        @Override
        public ConfigurableCredentialsProvider convert(CredentialsServiceEntity pe) {
            ConfigurableCredentialsProvider cp = new ConfigurableCredentialsProvider(pe.getAuthority(),
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
