package it.smartcommunitylab.aac.controller;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
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
import it.smartcommunitylab.aac.core.base.ConfigurableIdentityProvider;

/*
 * Base controller for identity providers
 */

@PreAuthorize("hasAuthority(this.authority)")
public class BaseIdentityProviderController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    protected ProviderManager providerManager;

    public String getAuthority() {
        return Config.R_USER;
    }

    /*
     * Identity providers
     * 
     * Manage only realm providers, with config stored
     */
    @GetMapping("/idp/{realm}")
    public Collection<ConfigurableIdentityProvider> listIdps(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm)
            throws NoSuchRealmException {
        logger.debug("list idp for realm {}",
                StringUtils.trimAllWhitespace(realm));

        return providerManager.listIdentityProviders(realm)
                .stream()
                .map(cp -> {
                    cp.setRegistered(providerManager.isProviderRegistered(realm, cp));
                    return cp;
                }).collect(Collectors.toList());
    }

    @PostMapping("/idp/{realm}")
    public ConfigurableIdentityProvider addIdp(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @RequestBody @Valid @NotNull ConfigurableIdentityProvider registration) throws NoSuchRealmException {
        logger.debug("add idp to realm {}",
                StringUtils.trimAllWhitespace(realm));

        // unpack and build model
        String id = registration.getProvider();
        String authority = registration.getAuthority();
        String name = registration.getName();
        String description = registration.getDescription();
        String persistence = registration.getPersistence();
        String events = registration.getEvents();
        boolean linkable = registration.isLinkable();

        Map<String, Serializable> configuration = registration.getConfiguration();
        Map<String, String> hookFunctions = registration.getHookFunctions();

        ConfigurableIdentityProvider provider = new ConfigurableIdentityProvider(authority, id, realm);
        provider.setName(name);
        provider.setDescription(description);
        provider.setEnabled(false);
        provider.setPersistence(persistence);
        provider.setLinkable(linkable);
        provider.setEvents(events);

        provider.setConfiguration(configuration);
        provider.setHookFunctions(hookFunctions);

        if (logger.isTraceEnabled()) {
            logger.trace("idp bean: " + StringUtils.trimAllWhitespace(provider.toString()));
        }

        provider = providerManager.addIdentityProvider(realm, provider);

        return provider;
    }

    @GetMapping("/idp/{realm}/{providerId}")
    public ConfigurableIdentityProvider getIdp(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId)
            throws NoSuchProviderException, NoSuchRealmException {
        logger.debug("get idp {} for realm {}",
                StringUtils.trimAllWhitespace(providerId), StringUtils.trimAllWhitespace(realm));

        ConfigurableIdentityProvider provider = providerManager.getIdentityProvider(realm, providerId);

        // check if registered
        boolean isRegistered = providerManager.isProviderRegistered(realm, provider);
        provider.setRegistered(isRegistered);

        return provider;
    }

    @PutMapping("/idp/{realm}/{providerId}")
    public ConfigurableIdentityProvider updateIdp(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId,
            @RequestBody @Valid @NotNull ConfigurableIdentityProvider registration,
            @RequestParam(required = false, defaultValue = "false") Optional<Boolean> force)
            throws NoSuchRealmException, NoSuchProviderException {
        logger.debug("update idp {} for realm {}",
                StringUtils.trimAllWhitespace(providerId), StringUtils.trimAllWhitespace(realm));

        ConfigurableIdentityProvider provider = providerManager.getIdentityProvider(realm, providerId);

        // if force disable provider
        boolean forceRegistration = force.orElse(false);
        if (forceRegistration && providerManager.isProviderRegistered(realm, provider)) {
            provider = providerManager.unregisterIdentityProvider(realm, providerId);
        }

        // we update only configuration
        String name = registration.getName();
        String description = registration.getDescription();
        String displayMode = registration.getDisplayMode();
        String persistence = registration.getPersistence();
        boolean enabled = registration.isEnabled();
        String events = registration.getEvents();
        boolean linkable = registration.isLinkable();

        Map<String, Serializable> configuration = registration.getConfiguration();
        Map<String, String> hookFunctions = registration.getHookFunctions();

        provider.setName(name);
        provider.setDescription(description);
        provider.setDisplayMode(displayMode);

        provider.setEnabled(enabled);
        provider.setPersistence(persistence);
        provider.setLinkable(linkable);

        provider.setEvents(events);
        provider.setHookFunctions(hookFunctions);
        provider.setConfiguration(configuration);

        if (logger.isTraceEnabled()) {
            logger.trace("idp bean: " + StringUtils.trimAllWhitespace(provider.toString()));
        }

        provider = providerManager.updateIdentityProvider(realm, providerId, provider);

        // if force and enabled try to register
        if (forceRegistration && provider.isEnabled()) {
            try {
                provider = providerManager.registerIdentityProvider(realm, providerId);
            } catch (Exception e) {
                // ignore
            }
        }

        // check if registered
        boolean isRegistered = providerManager.isProviderRegistered(realm, provider);
        provider.setRegistered(isRegistered);

        return provider;
    }

    @DeleteMapping("/idp/{realm}/{providerId}")
    public void deleteIdp(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId)
            throws NoSuchProviderException, NoSuchRealmException {
        logger.debug("delete idp {} for realm {}",
                StringUtils.trimAllWhitespace(providerId), StringUtils.trimAllWhitespace(realm));

        providerManager.deleteIdentityProvider(realm, providerId);

    }

    /*
     * Registration with authorities
     */

    @PutMapping("/idp/{realm}/{providerId}/status")
    public ConfigurableIdentityProvider registerIdp(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId)
            throws NoSuchProviderException, NoSuchRealmException {
        logger.debug("register idp {} for realm {}",
                StringUtils.trimAllWhitespace(providerId), StringUtils.trimAllWhitespace(realm));

        ConfigurableIdentityProvider provider = providerManager.getIdentityProvider(realm, providerId);
        provider = providerManager.registerIdentityProvider(realm, providerId);

        // check if registered
        boolean isRegistered = providerManager.isProviderRegistered(realm, provider);
        provider.setRegistered(isRegistered);

        return provider;

    }

    @DeleteMapping("/idp/{realm}/{providerId}/status")
    public ConfigurableIdentityProvider unregisterIdp(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId)
            throws NoSuchProviderException, NoSuchRealmException {
        logger.debug("unregister idp {} for realm {}",
                StringUtils.trimAllWhitespace(providerId), StringUtils.trimAllWhitespace(realm));

        ConfigurableIdentityProvider provider = providerManager.getIdentityProvider(realm, providerId);
        provider = providerManager.unregisterIdentityProvider(realm, providerId);

        // check if registered
        boolean isRegistered = providerManager.isProviderRegistered(realm, provider);
        provider.setRegistered(isRegistered);

        return provider;

    }

    /*
     * Configuration schema
     */
    @GetMapping("/idp/{realm}/{providerId}/schema")
    public JsonSchema getIdpConfigurationSchema(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId)
            throws NoSuchProviderException, NoSuchRealmException {
        logger.debug("get idp config schema for {} for realm {}",
                StringUtils.trimAllWhitespace(providerId), StringUtils.trimAllWhitespace(realm));

        ConfigurableIdentityProvider provider = providerManager.getIdentityProvider(realm, providerId);
        return providerManager.getConfigurationSchema(realm, SystemKeys.RESOURCE_IDENTITY, provider.getAuthority());
    }

}
