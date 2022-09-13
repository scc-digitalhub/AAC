package it.smartcommunitylab.aac.core.service;

import java.util.Collections;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.core.persistence.IdentityProviderEntity;

@Component
public class IdentityProviderEntityConverter
        implements Converter<IdentityProviderEntity, ConfigurableIdentityProvider> {

    @Override
    public ConfigurableIdentityProvider convert(IdentityProviderEntity pe) {
        ConfigurableIdentityProvider cp = new ConfigurableIdentityProvider(pe.getAuthority(), pe.getProviderId(),
                pe.getRealm());
        cp.setConfiguration(pe.getConfigurationMap());
        cp.setEnabled(pe.isEnabled());
        cp.setPersistence(pe.getPersistence());
        cp.setEvents(pe.getEvents());
        cp.setPosition(pe.getPosition());
        cp.setHookFunctions(pe.getHookFunctions() != null ? pe.getHookFunctions() : Collections.emptyMap());

        cp.setName(pe.getName());
        cp.setDescription(pe.getDescription());

        return cp;
    }

}
