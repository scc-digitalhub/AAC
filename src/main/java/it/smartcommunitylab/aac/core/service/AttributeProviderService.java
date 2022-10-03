package it.smartcommunitylab.aac.core.service;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchAuthorityException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.model.ConfigurableAttributeProvider;
import it.smartcommunitylab.aac.core.persistence.AttributeProviderEntity;
import it.smartcommunitylab.aac.core.provider.ConfigurationProvider;

@Service
@Transactional
public class AttributeProviderService
        extends ConfigurableProviderService<ConfigurableAttributeProvider, AttributeProviderEntity> {

    private AttributeProviderAuthorityService authorityService;

    public AttributeProviderService(ConfigurableProviderEntityService<AttributeProviderEntity> providerService) {
        super(providerService);
        setEntityConverter(new AttributeProviderEntityConverter());
        setConfigConverter(new AttributeProviderConfigConverter());
    }

    @Autowired
    public void setAuthorityService(AttributeProviderAuthorityService authorityService) {
        this.authorityService = authorityService;
    }

    @Override
    protected ConfigurationProvider<?, ?, ?> getConfigurationProvider(String authority)
            throws NoSuchAuthorityException {
        return authorityService.getAuthority(authority).getConfigurationProvider();
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
            if (!StringUtils.hasText(persistence)) {
                persistence = SystemKeys.PERSISTENCE_LEVEL_REPOSITORY;
            }

            if (!SystemKeys.PERSISTENCE_LEVEL_REPOSITORY.equals(persistence)
                    && !SystemKeys.PERSISTENCE_LEVEL_MEMORY.equals(persistence)
                    && !SystemKeys.PERSISTENCE_LEVEL_SESSION.equals(persistence)
                    && !SystemKeys.PERSISTENCE_LEVEL_NONE.equals(persistence)) {
                throw new RegistrationException("invalid persistence level");
            }

            String events = reg.getEvents();
            if (!StringUtils.hasText(events)) {
                events = SystemKeys.EVENTS_LEVEL_DETAILS;
            }

            if (!SystemKeys.EVENTS_LEVEL_DETAILS.equals(events)
                    && !SystemKeys.EVENTS_LEVEL_FULL.equals(events)
                    && !SystemKeys.EVENTS_LEVEL_MINIMAL.equals(events)
                    && !SystemKeys.EVENTS_LEVEL_NONE.equals(events)) {
                throw new RegistrationException("invalid events level");
            }

            pe.setPersistence(persistence);
            pe.setEvents(events);

            pe.setConfigurationMap(reg.getConfiguration());

            pe.setAttributeSets(StringUtils.collectionToCommaDelimitedString(reg.getAttributeSets()));

            return pe;
        }

    }

}
