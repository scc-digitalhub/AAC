package it.smartcommunitylab.aac.core.service;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.validation.DataBinder;
import org.springframework.validation.SmartValidator;

import com.fasterxml.jackson.module.jsonSchema.JsonSchema;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchAuthorityException;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.config.AuthoritiesProperties;
import it.smartcommunitylab.aac.config.ProvidersProperties;
import it.smartcommunitylab.aac.config.ProvidersProperties.ProviderConfiguration;
import it.smartcommunitylab.aac.core.base.AbstractProviderConfig;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.core.model.ConfigurableProperties;
import it.smartcommunitylab.aac.core.model.ConfigurableProvider;
import it.smartcommunitylab.aac.core.persistence.IdentityProviderEntity;
import it.smartcommunitylab.aac.core.provider.ConfigurationProvider;

@Service
public class IdentityProviderService {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private IdentityProviderEntityService providerService;

    @Autowired
    private IdentityProviderAuthorityService authorityService;

    @Autowired
    private SmartValidator validator;

    // keep a local map for system providers since these are not in db
    // key is providerId
    private Map<String, ConfigurableIdentityProvider> systemIdps;

    public IdentityProviderService(AuthoritiesProperties authoritiesProperties, ProvidersProperties providers) {
        this.systemIdps = new HashMap<>();
        // create system idps
        // these users access administrative contexts, they will have realm="system"
        // we expect no client/services in global+system realm!
        // note: we let registration with authorities to bootstrap

        // always internal idp for system, required by admin account
        ConfigurableIdentityProvider internalIdpConfig = new ConfigurableIdentityProvider(
                SystemKeys.AUTHORITY_INTERNAL, SystemKeys.AUTHORITY_INTERNAL,
                SystemKeys.REALM_SYSTEM);
        logger.debug("configure internal idp for system realm");
        systemIdps.put(internalIdpConfig.getProvider(), internalIdpConfig);

        // always configure internal password idp for system, required by admin account
        // TODO make configurable
        ConfigurableIdentityProvider internalPasswordIdpConfig = new ConfigurableIdentityProvider(
                SystemKeys.AUTHORITY_PASSWORD, SystemKeys.AUTHORITY_INTERNAL + "_" + SystemKeys.AUTHORITY_PASSWORD,
                SystemKeys.REALM_SYSTEM);
        logger.debug("configure internal password idp for system realm");
        systemIdps.put(internalPasswordIdpConfig.getProvider(), internalPasswordIdpConfig);

        // system providers
        if (providers != null) {
            // identity providers
            for (ProviderConfiguration providerConfig : providers.getIdentity()) {
                try {
                    // check match, we import only idps
                    if (!SystemKeys.RESOURCE_IDENTITY.equals(providerConfig.getType())) {
                        continue;
                    }

                    if (providerConfig.getEnable() != null && !providerConfig.isEnabled()) {
                        continue;
                    }

                    // we handle only system providers, add others via bootstrap
                    if (SystemKeys.REALM_SYSTEM.equals(providerConfig.getRealm())
                            || !StringUtils.hasText(providerConfig.getRealm())) {
                        logger.debug(
                                "configure provider for " + providerConfig.getType() + " for system realm: "
                                        + providerConfig.toString());

                        // translate config
                        ConfigurableIdentityProvider provider = new ConfigurableIdentityProvider(
                                providerConfig.getAuthority(),
                                providerConfig.getProvider(), SystemKeys.REALM_SYSTEM);
                        provider.setName(providerConfig.getName());
                        provider.setDescription(providerConfig.getDescription());
                        provider.setEnabled(true);
                        for (Map.Entry<String, String> entry : providerConfig.getConfiguration().entrySet()) {
                            provider.setConfigurationProperty(entry.getKey(), entry.getValue());
                        }

                        // by default global providers persist account + attributes
                        String persistenceLevel = SystemKeys.PERSISTENCE_LEVEL_REPOSITORY;
                        if (StringUtils.hasText(providerConfig.getPersistence())) {
                            // set persistence level
                            persistenceLevel = providerConfig.getPersistence();
                        }
                        provider.setPersistence(persistenceLevel);

                        // set default event level
                        String eventsLevel = SystemKeys.EVENTS_LEVEL_DETAILS;
                        if (StringUtils.hasText(providerConfig.getEvents())) {
                            // set persistence level
                            eventsLevel = providerConfig.getEvents();
                        }
                        provider.setEvents(eventsLevel);

                        // register
                        systemIdps.put(provider.getProvider(), provider);
                    }
                } catch (RegistrationException | SystemException | IllegalArgumentException ex) {
                    logger.error("error configuring provider :" + ex.getMessage(), ex);
                }
            }
        }
    }

    public Collection<ConfigurableIdentityProvider> listProviders(String realm) {
        if (SystemKeys.REALM_GLOBAL.equals(realm)) {
            // we do not persist in db global providers
            return Collections.emptyList();
        }

        if (SystemKeys.REALM_SYSTEM.equals(realm)) {
            return systemIdps.values();
        }

        List<IdentityProviderEntity> providers = providerService.listIdentityProvidersByRealm(realm);
        return providers.stream()
                .map(p -> fromEntity(p))
                .collect(Collectors.toList());
    }

    public ConfigurableIdentityProvider findProvider(String providerId) {
        // lookup in global map first
        if (systemIdps.containsKey(providerId)) {
            return systemIdps.get(providerId);
        }

        IdentityProviderEntity pe = providerService.findIdentityProvider(providerId);

        if (pe == null) {
            return null;
        }

        return fromEntity(pe);

    }

    public ConfigurableIdentityProvider getProvider(String providerId)
            throws NoSuchProviderException {
        // lookup in global map first
        if (systemIdps.containsKey(providerId)) {
            return systemIdps.get(providerId);
        }

        IdentityProviderEntity pe = providerService.getIdentityProvider(providerId);

        return fromEntity(pe);

    }

    public ConfigurableIdentityProvider addProvider(String realm,
            ConfigurableIdentityProvider provider)
            throws RegistrationException, SystemException, NoSuchProviderException, NoSuchAuthorityException {

        if (SystemKeys.REALM_GLOBAL.equals(realm) || SystemKeys.REALM_SYSTEM.equals(realm)) {
            // we do not persist in db global providers
            throw new RegistrationException("global providers are immutable");
        }

        // check if id provided
        String providerId = provider.getProvider();
        if (StringUtils.hasText(providerId)) {
            IdentityProviderEntity pe = providerService.findIdentityProvider(providerId);
            if (pe != null) {
                throw new RegistrationException("id already in use");
            }

            // validate
            if (providerId.length() < 6 || !Pattern.matches(SystemKeys.SLUG_PATTERN, providerId)) {
                throw new RegistrationException("invalid id");
            }

        } else {
            // generate a valid id
            IdentityProviderEntity pe = providerService.createIdentityProvider();
            providerId = pe.getProviderId();
        }

        // unpack props and validate
        // TODO handle enum of authorities for validation
        String authority = provider.getAuthority();

        // we support only idp for now
        String type = provider.getType();
        if (!StringUtils.hasText(type) || !SystemKeys.RESOURCE_IDENTITY.equals(type)) {
            throw new RegistrationException("invalid provider type");
        }

        String name = provider.getName();
        String description = provider.getDescription();
        String icon = provider.getIcon();
        if (StringUtils.hasText(name)) {
            name = Jsoup.clean(name, Safelist.none());
        }
        if (StringUtils.hasText(description)) {
            description = Jsoup.clean(description, Safelist.none());
        }
        if (StringUtils.hasText(icon)) {
            icon = Jsoup.clean(icon, Safelist.none());
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

        Integer position = provider.getPosition();

        // we validate config by converting to specific configMap
        ConfigurationProvider<? extends ConfigurableProvider, ? extends AbstractProviderConfig, ? extends ConfigurableProperties> configProvider = authorityService
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

        // fetch hooks
        Map<String, String> hookFunctions = provider.getHookFunctions();

        IdentityProviderEntity pe = providerService.addIdentityProvider(
                authority, providerId, realm,
                name, description, icon,
                persistence, events, position,
                configuration, hookFunctions);

        return fromEntity(pe);

    }

    public ConfigurableIdentityProvider updateProvider(
            String providerId, ConfigurableIdentityProvider provider)
            throws NoSuchProviderException, NoSuchAuthorityException {
        IdentityProviderEntity pe = providerService.getIdentityProvider(providerId);

        if (StringUtils.hasText(provider.getProvider()) && !providerId.equals(provider.getProvider())) {
            throw new IllegalArgumentException("configuration does not match provider");
        }

        // we update only props and configuration
        String name = provider.getName();
        String description = provider.getDescription();
        String icon = provider.getIcon();
        if (StringUtils.hasText(name)) {
            name = Jsoup.clean(name, Safelist.none());
        }
        if (StringUtils.hasText(description)) {
            description = Jsoup.clean(description, Safelist.none());
        }
        if (StringUtils.hasText(icon)) {
            icon = Jsoup.clean(icon, Safelist.none());
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

        Integer position = provider.getPosition();

        boolean enabled = provider.isEnabled();
        boolean linkable = provider.isLinkable();

        String authority = pe.getAuthority();

        // we validate config by converting to specific configMap
        ConfigurationProvider<? extends ConfigurableProvider, ? extends AbstractProviderConfig, ? extends ConfigurableProperties> configProvider = authorityService
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

        // fetch hooks
        Map<String, String> hookFunctions = provider.getHookFunctions();

        // update: even when enabled this provider won't be active until registration
        pe = providerService.updateIdentityProvider(providerId,
                enabled, linkable,
                name, description, icon,
                persistence, events, position,
                configuration, hookFunctions);

        return fromEntity(pe);
    }

    public void deleteProvider(String providerId)
            throws SystemException, NoSuchProviderException {

        IdentityProviderEntity pe = providerService.getIdentityProvider(providerId);
        providerService.deleteIdentityProvider(pe.getProviderId());

    }

    /*
     * Configuration schemas
     */

    public ConfigurableProperties getConfigurableProperties(String authority) throws NoSuchAuthorityException {
        ConfigurationProvider<? extends ConfigurableProvider, ? extends AbstractProviderConfig, ? extends ConfigurableProperties> configProvider = authorityService
                .getAuthority(authority).getConfigurationProvider();
        return configProvider.getDefaultConfigMap();
    }

    public JsonSchema getConfigurationSchema(String authority) throws NoSuchAuthorityException {
        ConfigurationProvider<? extends ConfigurableProvider, ? extends AbstractProviderConfig, ? extends ConfigurableProperties> configProvider = authorityService
                .getAuthority(authority).getConfigurationProvider();
        return configProvider.getSchema();
    }

    /*
     * Helpers
     */

    private ConfigurableIdentityProvider fromEntity(IdentityProviderEntity pe) {
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
