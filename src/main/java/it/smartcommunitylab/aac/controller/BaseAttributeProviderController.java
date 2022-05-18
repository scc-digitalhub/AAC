package it.smartcommunitylab.aac.controller;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.core.ProviderManager;
import it.smartcommunitylab.aac.core.model.ConfigurableAttributeProvider;

/*
 * Base controller for attribute providers
 */

@PreAuthorize("hasAuthority(this.authority)")
public class BaseAttributeProviderController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    protected ProviderManager providerManager;

    public String getAuthority() {
        return Config.R_USER;
    }

    /*
     * Attribute providers
     * 
     * Manage only realm providers, with config stored
     */

    @GetMapping("/ap/{realm}")
    public Collection<ConfigurableAttributeProvider> listAps(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm)
            throws NoSuchRealmException {
        logger.debug("list ap for realm {}",
                StringUtils.trimAllWhitespace(realm));

        return providerManager.listAttributeProviders(realm)
                .stream()
                .map(cp -> {
                    cp.setRegistered(providerManager.isProviderRegistered(realm, cp));
                    return cp;
                }).collect(Collectors.toList());
    }

    @GetMapping("/ap/{realm}/{providerId}")
    public ConfigurableAttributeProvider getAp(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId)
            throws NoSuchProviderException, NoSuchRealmException {
        logger.debug("get ap {} for realm {}",
                StringUtils.trimAllWhitespace(providerId),
                StringUtils.trimAllWhitespace(realm));

        ConfigurableAttributeProvider provider = providerManager.getAttributeProvider(realm, providerId);

        // check if registered
        boolean isRegistered = providerManager.isProviderRegistered(realm, provider);
        provider.setRegistered(isRegistered);

        return provider;
    }

    @PostMapping("/ap/{realm}")
    public ConfigurableAttributeProvider addAp(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @RequestBody @Valid @NotNull ConfigurableAttributeProvider registration) throws NoSuchRealmException {
        logger.debug("add ap to realm {}",
                StringUtils.trimAllWhitespace(realm));

        // unpack and build model
        String id = registration.getProvider();
        String authority = registration.getAuthority();
        String name = registration.getName();
        String description = registration.getDescription();
        String persistence = registration.getPersistence();
        String events = registration.getEvents();
        Set<String> attributeSets = registration.getAttributeSets();
        Map<String, Serializable> configuration = registration.getConfiguration();

        ConfigurableAttributeProvider provider = new ConfigurableAttributeProvider(authority, id, realm);
        provider.setName(name);
        provider.setDescription(description);
        provider.setEnabled(false);
        provider.setPersistence(persistence);
        provider.setEvents(events);
        provider.setAttributeSets(attributeSets);
        provider.setConfiguration(configuration);

        if (logger.isTraceEnabled()) {
            logger.trace("ap bean: " + String.valueOf(provider));
        }

        provider = providerManager.addAttributeProvider(realm, provider);

        return provider;
    }

    @PutMapping("/ap/{realm}/{providerId}")
    public ConfigurableAttributeProvider updateAp(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId,
            @RequestBody @Valid @NotNull ConfigurableAttributeProvider registration,
            @RequestParam(required = false, defaultValue = "false") Optional<Boolean> force)
            throws NoSuchRealmException, NoSuchProviderException {
        logger.debug("update ap {} for realm {}",
                StringUtils.trimAllWhitespace(providerId), StringUtils.trimAllWhitespace(realm));

        ConfigurableAttributeProvider provider = providerManager.getAttributeProvider(realm, providerId);

        // if force disable provider
        boolean forceRegistration = force.orElse(false);
        if (forceRegistration && providerManager.isProviderRegistered(realm, provider)) {
            provider = providerManager.unregisterAttributeProvider(realm, providerId);
        }

        // we update only configuration
        String name = registration.getName();
        String description = registration.getDescription();
        String persistence = registration.getPersistence();
        boolean enabled = registration.isEnabled();
        String events = registration.getEvents();
        Set<String> attributeSets = registration.getAttributeSets();
        Map<String, Serializable> configuration = registration.getConfiguration();

        provider.setName(name);
        provider.setDescription(description);
        provider.setEnabled(enabled);
        provider.setPersistence(persistence);
        provider.setEvents(events);
        provider.setAttributeSets(attributeSets);
        provider.setConfiguration(configuration);

        if (logger.isTraceEnabled()) {
            logger.trace("ap bean: " + String.valueOf(provider));
        }

        provider = providerManager.updateAttributeProvider(realm, providerId, provider);

        // if force and enabled try to register
        if (forceRegistration && provider.isEnabled()) {
            try {
                provider = providerManager.registerAttributeProvider(realm, providerId);
            } catch (Exception e) {
                // ignore
            }
        }

        // check if registered
        boolean isRegistered = providerManager.isProviderRegistered(realm, provider);
        provider.setRegistered(isRegistered);

        return provider;
    }

    @DeleteMapping("/ap/{realm}/{providerId}")
    public void deleteAp(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId)
            throws NoSuchProviderException, NoSuchRealmException {
        logger.debug("delete ap {} for realm {}",
                StringUtils.trimAllWhitespace(providerId), StringUtils.trimAllWhitespace(realm));

        providerManager.deleteAttributeProvider(realm, providerId);

    }

    /*
     * Registration with authorities
     */

    @PutMapping("/ap/{realm}/{providerId}/status")
    public ConfigurableAttributeProvider registerAp(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId)
            throws NoSuchProviderException, NoSuchRealmException {
        logger.debug("register ap {} for realm {}",
                StringUtils.trimAllWhitespace(providerId), StringUtils.trimAllWhitespace(realm));

        ConfigurableAttributeProvider provider = providerManager.getAttributeProvider(realm, providerId);
        provider = providerManager.registerAttributeProvider(realm, providerId);

        // check if registered
        boolean isRegistered = providerManager.isProviderRegistered(realm, provider);
        provider.setRegistered(isRegistered);

        return provider;

    }

    @DeleteMapping("/ap/{realm}/{providerId}/status")
    public ConfigurableAttributeProvider unregisterAp(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId)
            throws NoSuchProviderException, NoSuchRealmException {
        logger.debug("unregister ap {} for realm {}",
                StringUtils.trimAllWhitespace(providerId), StringUtils.trimAllWhitespace(realm));

        ConfigurableAttributeProvider provider = providerManager.getAttributeProvider(realm, providerId);
        provider = providerManager.unregisterAttributeProvider(realm, providerId);

        // check if registered
        boolean isRegistered = providerManager.isProviderRegistered(realm, provider);
        provider.setRegistered(isRegistered);

        return provider;

    }

    /*
     * Configuration schema
     */
    @GetMapping("/ap/{realm}/{providerId}/schema")
    public JsonSchema getApConfigurationSchema(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId)
            throws NoSuchProviderException, NoSuchRealmException {
        logger.debug("get ap config schema for {} for realm {}",
                StringUtils.trimAllWhitespace(providerId), StringUtils.trimAllWhitespace(realm));

        ConfigurableAttributeProvider provider = providerManager.getAttributeProvider(realm, providerId);
        return providerManager.getConfigurationSchema(realm, SystemKeys.RESOURCE_ATTRIBUTES, provider.getAuthority());
    }

}
