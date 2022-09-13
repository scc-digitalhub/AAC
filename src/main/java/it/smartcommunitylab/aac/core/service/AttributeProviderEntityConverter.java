package it.smartcommunitylab.aac.core.service;

import java.util.Collections;
import java.util.Set;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.core.model.ConfigurableAttributeProvider;
import it.smartcommunitylab.aac.core.persistence.AttributeProviderEntity;

@Component
public class AttributeProviderEntityConverter
        implements Converter<AttributeProviderEntity, ConfigurableAttributeProvider> {

    @Override
    public ConfigurableAttributeProvider convert(AttributeProviderEntity pe) {
        ConfigurableAttributeProvider cp = new ConfigurableAttributeProvider(pe.getAuthority(), pe.getProviderId(),
                pe.getRealm());
        cp.setConfiguration(pe.getConfigurationMap());
        cp.setEnabled(pe.isEnabled());
        cp.setPersistence(pe.getPersistence());
        cp.setEvents(pe.getEvents());
        cp.setName(pe.getName());
        cp.setDescription(pe.getDescription());

        Set<String> attributeSets = pe.getAttributeSets() != null
                ? StringUtils.commaDelimitedListToSet(pe.getAttributeSets())
                : Collections.emptySet();
        cp.setAttributeSets(attributeSets);

        return cp;
    }

}
