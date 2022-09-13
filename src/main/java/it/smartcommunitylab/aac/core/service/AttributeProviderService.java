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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.DataBinder;
import org.springframework.validation.SmartValidator;

import com.fasterxml.jackson.module.jsonSchema.JsonSchema;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchAuthorityException;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.core.model.ConfigurableAttributeProvider;
import it.smartcommunitylab.aac.core.model.ConfigurableProperties;
import it.smartcommunitylab.aac.core.persistence.AttributeProviderEntity;
import it.smartcommunitylab.aac.core.provider.ConfigurationProvider;

@Service
@Transactional
public class AttributeProviderService {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private AttributeProviderEntityService providerService;

    @Autowired
    private AttributeProviderAuthorityService authorityService;

    @Autowired
    private SmartValidator validator;

    public AttributeProviderService() {
    }

    @Transactional(readOnly = true)
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

    @Transactional(readOnly = true)
    public ConfigurableAttributeProvider findProvider(String providerId) {
        AttributeProviderEntity pe = providerService.findAttributeProvider(providerId);
        if (pe == null) {
            return null;
        }

        return fromEntity(pe);
    }

    @Transactional(readOnly = true)
    public ConfigurableAttributeProvider getProvider(String providerId)
            throws NoSuchProviderException {
        AttributeProviderEntity pe = providerService.getAttributeProvider(providerId);
        return fromEntity(pe);
    }

    public ConfigurableAttributeProvider addProvider(String realm,
            ConfigurableAttributeProvider provider)
            throws RegistrationException, SystemException, NoSuchAuthorityException {

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

        // we validate config by converting to specific configMap
        ConfigurationProvider<?, ConfigurableAttributeProvider, ?> configProvider = authorityService
                .getAuthority(authority).getConfigurationProvider();

        ConfigMap configurable = configProvider.getConfigMap(provider.getConfiguration());

        // check with validator
        DataBinder binder = new DataBinder(configurable);
        validator.validate(configurable, binder.getBindingResult());
        if (binder.getBindingResult().hasErrors()) {
            StringBuilder sb = new StringBuilder();
            binder.getBindingResult().getFieldErrors().forEach(e -> {
                sb.append(e.getField()).append(" ").append(e.getDefaultMessage());
            });
            String errorMsg = sb.toString();
            throw new RegistrationException(errorMsg);
        }

        Map<String, Serializable> configuration = configurable.getConfiguration();

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
            throws NoSuchProviderException, NoSuchAuthorityException {
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

        String authority = pe.getAuthority();

        // we validate config by converting to specific configMap
        ConfigurationProvider<?, ConfigurableAttributeProvider, ?> configProvider = authorityService
                .getAuthority(authority).getConfigurationProvider();

        ConfigurableProperties configurable = configProvider.getConfigMap(provider.getConfiguration());

        // check with validator
        DataBinder binder = new DataBinder(configurable);
        validator.validate(configurable, binder.getBindingResult());
        if (binder.getBindingResult().hasErrors()) {
            StringBuilder sb = new StringBuilder();
            binder.getBindingResult().getFieldErrors().forEach(e -> {
                sb.append(e.getField()).append(" ").append(e.getDefaultMessage());
            });
            String errorMsg = sb.toString();
            throw new RegistrationException(errorMsg);
        }

        Map<String, Serializable> configuration = configurable.getConfiguration();

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

    @Transactional(readOnly = true)
    public ConfigurableProperties getConfigurableProperties(String authority) throws NoSuchAuthorityException {
        ConfigurationProvider<?, ConfigurableAttributeProvider, ?> configProvider = authorityService
                .getAuthority(authority).getConfigurationProvider();
        return configProvider.getDefaultConfigMap();
    }

    @Transactional(readOnly = true)
    public JsonSchema getConfigurationSchema(String authority) throws NoSuchAuthorityException {
        ConfigurationProvider<?, ConfigurableAttributeProvider, ?> configProvider = authorityService
                .getAuthority(authority).getConfigurationProvider();
        return configProvider.getSchema();
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
