package it.smartcommunitylab.aac.core.service;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.provider.MapperAttributeProviderConfigMap;
import it.smartcommunitylab.aac.attributes.provider.ScriptAttributeProviderConfigMap;
import it.smartcommunitylab.aac.attributes.provider.WebhookAttributeProviderConfigMap;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.base.ConfigurableAttributeProvider;
import it.smartcommunitylab.aac.core.base.ConfigurableProperties;
import it.smartcommunitylab.aac.core.persistence.AttributeProviderEntity;
import it.smartcommunitylab.aac.internal.provider.InternalAttributeProviderConfigMap;

@Service
public class AttributeProviderService {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private AttributeProviderEntityService providerService;

    public AttributeProviderService() {
    }

    public Collection<ConfigurableAttributeProvider> listProviders(String realm) {
        if (SystemKeys.REALM_GLOBAL.equals(realm) || SystemKeys.REALM_SYSTEM.equals(realm)) {
            // we do not persist in db global providers
            return Collections.emptyList();
        }

        List<AttributeProviderEntity> providers = providerService.listAttributeProvidersByRealm(realm);
        return providers.stream()
                .map(p -> fromEntity(p))
                .collect(Collectors.toList());
    }

    public ConfigurableAttributeProvider findProvider(String providerId) {
        AttributeProviderEntity pe = providerService.findAttributeProvider(providerId);

        if (pe == null) {
            return null;
        }

        return fromEntity(pe);

    }

    public ConfigurableAttributeProvider getProvider(String providerId)
            throws NoSuchProviderException {
        AttributeProviderEntity pe = providerService.getAttributeProvider(providerId);

        return fromEntity(pe);

    }

    public ConfigurableAttributeProvider addProvider(String realm,
            ConfigurableAttributeProvider provider) throws RegistrationException, SystemException {

        if (SystemKeys.REALM_GLOBAL.equals(realm) || SystemKeys.REALM_SYSTEM.equals(realm)) {
            // we do not persist in db global providers
            throw new RegistrationException("global providers are immutable");
        }

        // check if id provided
        String providerId = provider.getProvider();
        if (StringUtils.hasText(providerId)) {
            AttributeProviderEntity pe = providerService.findAttributeProvider(providerId);
            if (pe != null) {
                throw new RegistrationException("id already in use");
            }

            // validate
            if (providerId.length() < 6 || !Pattern.matches(SystemKeys.SLUG_PATTERN, providerId)) {
                throw new RegistrationException("invalid id");
            }

        } else {
            // generate a valid id
            AttributeProviderEntity pe = providerService.createAttributeProvider();
            providerId = pe.getProviderId();
        }

        // unpack props and validate
        // TODO handle enum of authorities for validation
        String authority = provider.getAuthority();

        // we support only idp for now
        String type = provider.getType();
        if (!StringUtils.hasText(type) || !SystemKeys.RESOURCE_ATTRIBUTES.equals(type)) {
            throw new RegistrationException("invalid provider type");
        }

        String name = provider.getName();
        String description = provider.getDescription();
        if (StringUtils.hasText(name)) {
            name = Jsoup.clean(name, Safelist.none());
        }
        if (StringUtils.hasText(description)) {
            description = Jsoup.clean(description, Safelist.none());
        }

        // TODO add enum
        String persistence = provider.getPersistence();
        if (!StringUtils.hasText(persistence)) {
            persistence = SystemKeys.PERSISTENCE_LEVEL_REPOSITORY;
        }

        if (!SystemKeys.PERSISTENCE_LEVEL_REPOSITORY.equals(persistence)
                && !SystemKeys.PERSISTENCE_LEVEL_MEMORY.equals(persistence)
                && !SystemKeys.PERSISTENCE_LEVEL_SESSION.equals(persistence)
                && !SystemKeys.PERSISTENCE_LEVEL_NONE.equals(persistence)) {
            throw new RegistrationException("invalid persistence level");
        }

        String events = provider.getEvents();
        if (!StringUtils.hasText(events)) {
            events = SystemKeys.EVENTS_LEVEL_DETAILS;
        }

        if (!SystemKeys.EVENTS_LEVEL_DETAILS.equals(events)
                && !SystemKeys.EVENTS_LEVEL_FULL.equals(events)
                && !SystemKeys.EVENTS_LEVEL_MINIMAL.equals(events)
                && !SystemKeys.EVENTS_LEVEL_NONE.equals(events)) {
            throw new RegistrationException("invalid events level");
        }

        Collection<String> attributeSets = provider.getAttributeSets();

        Map<String, Serializable> configuration = null;
        if (SystemKeys.RESOURCE_ATTRIBUTES.equals(type)) {

            // we validate config by converting to specific configMap
            ConfigurableProperties configurable = null;
            if (SystemKeys.AUTHORITY_INTERNAL.equals(authority)) {
                configurable = new InternalAttributeProviderConfigMap();
            } else if (SystemKeys.AUTHORITY_MAPPER.equals(authority)) {
                configurable = new MapperAttributeProviderConfigMap();
            } else if (SystemKeys.AUTHORITY_SCRIPT.equals(authority)) {
                configurable = new ScriptAttributeProviderConfigMap();
            } else if (SystemKeys.AUTHORITY_WEBHOOK.equals(authority)) {
                configurable = new WebhookAttributeProviderConfigMap();
            }
            if (configurable == null) {
                throw new IllegalArgumentException("invalid configuration");
            }

            configurable.setConfiguration(provider.getConfiguration());
            configuration = configurable.getConfiguration();
        }

        AttributeProviderEntity pe = providerService.addAttributeProvider(
                authority, providerId, realm,
                name, description,
                persistence, events,
                attributeSets,
                configuration);

        return fromEntity(pe);

    }

    public ConfigurableAttributeProvider updateProvider(
            String providerId, ConfigurableAttributeProvider provider)
            throws NoSuchProviderException {
        AttributeProviderEntity pe = providerService.getAttributeProvider(providerId);

        if (StringUtils.hasText(provider.getProvider()) && !providerId.equals(provider.getProvider())) {
            throw new IllegalArgumentException("configuration does not match provider");
        }

        // we update only props and configuration
        String name = provider.getName();
        String description = provider.getDescription();
        if (StringUtils.hasText(name)) {
            name = Jsoup.clean(name, Safelist.none());
        }
        if (StringUtils.hasText(description)) {
            description = Jsoup.clean(description, Safelist.none());
        }

        // TODO add enum
        String persistence = provider.getPersistence();
        if (!StringUtils.hasText(persistence)) {
            persistence = SystemKeys.PERSISTENCE_LEVEL_REPOSITORY;
        }

        if (!SystemKeys.PERSISTENCE_LEVEL_REPOSITORY.equals(persistence)
                && !SystemKeys.PERSISTENCE_LEVEL_MEMORY.equals(persistence)
                && !SystemKeys.PERSISTENCE_LEVEL_SESSION.equals(persistence)
                && !SystemKeys.PERSISTENCE_LEVEL_NONE.equals(persistence)) {
            throw new RegistrationException("invalid persistence level");
        }

        String events = provider.getEvents();
        if (!StringUtils.hasText(events)) {
            events = SystemKeys.EVENTS_LEVEL_DETAILS;
        }

        if (!SystemKeys.EVENTS_LEVEL_DETAILS.equals(events)
                && !SystemKeys.EVENTS_LEVEL_FULL.equals(events)
                && !SystemKeys.EVENTS_LEVEL_MINIMAL.equals(events)
                && !SystemKeys.EVENTS_LEVEL_NONE.equals(events)) {
            throw new RegistrationException("invalid events level");
        }

        Collection<String> attributeSets = provider.getAttributeSets();

        boolean enabled = provider.isEnabled();

        Map<String, Serializable> configuration = null;

        String authority = pe.getAuthority();

        // we validate config by converting to specific configMap
        ConfigurableProperties configurable = null;
        if (SystemKeys.AUTHORITY_INTERNAL.equals(authority)) {
            configurable = new InternalAttributeProviderConfigMap();
        } else if (SystemKeys.AUTHORITY_MAPPER.equals(authority)) {
            configurable = new MapperAttributeProviderConfigMap();
        } else if (SystemKeys.AUTHORITY_SCRIPT.equals(authority)) {
            configurable = new ScriptAttributeProviderConfigMap();
        } else if (SystemKeys.AUTHORITY_WEBHOOK.equals(authority)) {
            configurable = new WebhookAttributeProviderConfigMap();
        }
        if (configurable == null) {
            throw new IllegalArgumentException("invalid configuration");
        }

        configurable.setConfiguration(provider.getConfiguration());
        configuration = configurable.getConfiguration();

        // update: even when enabled this provider won't be active until registration
        pe = providerService.updateAttributeProvider(providerId,
                enabled,
                name, description,
                persistence, events,
                attributeSets,
                configuration);

        return fromEntity(pe);
    }

    public void deleteProvider(String providerId)
            throws SystemException, NoSuchProviderException {

        AttributeProviderEntity pe = providerService.getAttributeProvider(providerId);
        providerService.deleteAttributeProvider(pe.getProviderId());

    }

    /*
     * Configuration schemas
     */

    public JsonSchema getConfigurationSchema(String authority) {
        try {
            if (SystemKeys.AUTHORITY_INTERNAL.equals(authority)) {
                return InternalAttributeProviderConfigMap.getConfigurationSchema();
            } else if (SystemKeys.AUTHORITY_MAPPER.equals(authority)) {
                return MapperAttributeProviderConfigMap.getConfigurationSchema();
            } else if (SystemKeys.AUTHORITY_SCRIPT.equals(authority)) {
                return ScriptAttributeProviderConfigMap.getConfigurationSchema();
            } else if (SystemKeys.AUTHORITY_WEBHOOK.equals(authority)) {
                return WebhookAttributeProviderConfigMap.getConfigurationSchema();
            }
        } catch (JsonMappingException e) {
            e.printStackTrace();
            return null;
        }

        throw new IllegalArgumentException("invalid authority");
    }

    /*
     * Helpers
     */

    private ConfigurableAttributeProvider fromEntity(AttributeProviderEntity pe) {
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
