package it.smartcommunitylab.aac.core.service;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.core.authorities.AttributeProviderAuthority;
import it.smartcommunitylab.aac.core.model.ConfigurableAttributeProvider;
import it.smartcommunitylab.aac.core.persistence.AttributeProviderEntity;

@Service
@Transactional
public class AttributeProviderService
        extends
        ConfigurableProviderService<AttributeProviderAuthority<?, ?, ?>, ConfigurableAttributeProvider, AttributeProviderEntity> {

    public AttributeProviderService(AttributeProviderAuthorityService authorityService,
            ConfigurableProviderEntityService<AttributeProviderEntity> providerService) {
        super(authorityService, providerService);

        // set converters
        setEntityConverter(new AttributeProviderEntityConverter());
        setConfigConverter(new AttributeProviderConfigConverter());
    }

    class AttributeProviderEntityConverter
            implements Converter<AttributeProviderEntity, ConfigurableAttributeProvider> {

        @Override
        public ConfigurableAttributeProvider convert(AttributeProviderEntity pe) {
            ConfigurableAttributeProvider cp = new ConfigurableAttributeProvider(pe.getAuthority(), pe.getProvider(),
                    pe.getRealm());
            cp.setConfiguration(pe.getConfigurationMap());
            cp.setEnabled(pe.isEnabled());
            cp.setPersistence(pe.getPersistence());
            cp.setEvents(pe.getEvents());

            cp.setName(pe.getName());
            cp.setTitleMap(pe.getTitleMap());
            cp.setDescriptionMap(pe.getDescriptionMap());

            Set<String> attributeSets = pe.getAttributeSets() != null
                    ? StringUtils.commaDelimitedListToSet(pe.getAttributeSets())
                    : Collections.emptySet();
            cp.setAttributeSets(attributeSets);

            return cp;
        }

    }

    class AttributeProviderConfigConverter
            implements Converter<ConfigurableAttributeProvider, AttributeProviderEntity> {

        @Override
        public AttributeProviderEntity convert(ConfigurableAttributeProvider reg) {
            AttributeProviderEntity pe = new AttributeProviderEntity();

            pe.setAuthority(reg.getAuthority());
            pe.setProvider(reg.getProvider());
            pe.setRealm(reg.getRealm());
            pe.setEnabled(reg.isEnabled());

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

            // TODO add enum
            String persistence = reg.getPersistence();
            String events = reg.getEvents();

            pe.setPersistence(persistence);
            pe.setEvents(events);

            pe.setConfigurationMap(reg.getConfiguration());

            pe.setAttributeSets(StringUtils.collectionToCommaDelimitedString(reg.getAttributeSets()));

            return pe;
        }

    }

}
