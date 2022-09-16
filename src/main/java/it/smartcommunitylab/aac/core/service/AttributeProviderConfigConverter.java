package it.smartcommunitylab.aac.core.service;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.model.ConfigurableAttributeProvider;
import it.smartcommunitylab.aac.core.persistence.AttributeProviderEntity;

@Component
public class AttributeProviderConfigConverter
        implements Converter<ConfigurableAttributeProvider, AttributeProviderEntity> {

    @Override
    public AttributeProviderEntity convert(ConfigurableAttributeProvider reg) {
        AttributeProviderEntity pe = new AttributeProviderEntity();

        pe.setAuthority(reg.getAuthority());
        pe.setProviderId(reg.getProvider());
        pe.setRealm(reg.getRealm());
        pe.setEnabled(reg.isEnabled());

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
