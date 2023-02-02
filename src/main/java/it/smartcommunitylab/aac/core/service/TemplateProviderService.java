package it.smartcommunitylab.aac.core.service;

import java.util.Map;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.core.authorities.TemplateProviderAuthority;
import it.smartcommunitylab.aac.core.model.ConfigurableTemplateProvider;
import it.smartcommunitylab.aac.core.persistence.TemplateProviderEntity;

@Service
@Transactional
public class TemplateProviderService
        extends
        ConfigurableProviderService<TemplateProviderAuthority<?, ?, ?, ?>, ConfigurableTemplateProvider, TemplateProviderEntity> {

    public TemplateProviderService(TemplateProviderAuthorityService authorityService,
            ConfigurableProviderEntityService<TemplateProviderEntity> providerService) {
        super(authorityService, providerService);

        // set converters
        setConfigConverter(new TemplateProviderConfigConverter());
        setEntityConverter(new TemplateProviderEntityConverter());
    }

    class TemplateProviderConfigConverter implements Converter<ConfigurableTemplateProvider, TemplateProviderEntity> {

        @Override
        public TemplateProviderEntity convert(ConfigurableTemplateProvider reg) {
            TemplateProviderEntity pe = new TemplateProviderEntity();

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

            pe.setLanguages(StringUtils.collectionToCommaDelimitedString(reg.getLanguages()));
            pe.setCustomStyle(reg.getCustomStyle());

            pe.setEnabled(reg.isEnabled());
            pe.setConfigurationMap(reg.getConfiguration());
            pe.setVersion(reg.getVersion());

            return pe;
        }

    }

    class TemplateProviderEntityConverter implements Converter<TemplateProviderEntity, ConfigurableTemplateProvider> {

        @Override
        public ConfigurableTemplateProvider convert(TemplateProviderEntity pe) {
            ConfigurableTemplateProvider cp = new ConfigurableTemplateProvider(pe.getAuthority(), pe.getProvider(),
                    pe.getRealm());

            cp.setName(pe.getName());
            cp.setTitleMap(pe.getTitleMap());
            cp.setDescriptionMap(pe.getDescriptionMap());

            cp.setLanguages(StringUtils.commaDelimitedListToSet(pe.getLanguages()));
            cp.setCustomStyle(pe.getCustomStyle());

            cp.setEnabled(pe.isEnabled());
            
            cp.setConfiguration(pe.getConfigurationMap());
            cp.setVersion(pe.getVersion());

            return cp;
        }
    }
}
