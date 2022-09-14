package it.smartcommunitylab.aac.core.service;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.core.persistence.IdentityProviderEntity;

@Component
public class IdentityProviderConfigConverter
        implements Converter<ConfigurableIdentityProvider, IdentityProviderEntity> {

    @Override
    public IdentityProviderEntity convert(ConfigurableIdentityProvider reg) {
        IdentityProviderEntity pe = new IdentityProviderEntity();

        pe.setAuthority(reg.getAuthority());
        pe.setRealm(reg.getRealm());
        pe.setEnabled(reg.isEnabled());
        pe.setLinkable(reg.isLinkable());

        String name = reg.getName();
        String description = reg.getDescription();
        String icon = reg.getIcon();
        if (StringUtils.hasText(name)) {
            name = Jsoup.clean(name, Safelist.none());
        }
        if (StringUtils.hasText(description)) {
            description = Jsoup.clean(description, Safelist.none());
        }
        if (StringUtils.hasText(icon)) {
            icon = Jsoup.clean(icon, Safelist.none());
        }

        pe.setName(name);
        pe.setDescription(description);
        pe.setIcon(icon);

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

        Integer position = (reg.getPosition() != null && reg.getPosition().intValue() > 0) ? reg.getPosition() : null;

        pe.setPersistence(persistence);
        pe.setEvents(events);
        pe.setPosition(position);

        pe.setConfigurationMap(reg.getConfiguration());
        pe.setHookFunctions(reg.getHookFunctions());

        return pe;
    }

}
