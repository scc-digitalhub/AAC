package it.smartcommunitylab.aac.controller;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;

import io.swagger.v3.oas.annotations.Operation;
import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchAuthorityException;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.IdentityProviderManager;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.core.model.ConfigurableProperties;
import it.smartcommunitylab.aac.core.service.IdentityProviderAuthorityService;

/*
 * Base controller for identity providers
 */

@PreAuthorize("hasAuthority(this.authority)")
public class BaseIdentityProviderController implements InitializingBean {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    protected IdentityProviderManager providerManager;

    // TODO evaluate replace with authorityManager
    protected IdentityProviderAuthorityService authorityService;

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(providerManager, "provider manager is required");
        Assert.notNull(authorityService, "authority service is required");
    }

    @Autowired
    public void setProviderManager(IdentityProviderManager providerManager) {
        this.providerManager = providerManager;
    }

    @Autowired
    public void setAuthorityService(IdentityProviderAuthorityService authorityService) {
        this.authorityService = authorityService;
    }

    public String getAuthority() {
        return Config.R_USER;
    }

    /*
     * Authorities
     * 
     * TODO evaluate returning a authority model as result
     */
    @GetMapping("/idps/{realm}/authorities")
    @Operation(summary = "list idp authorities from a given realm")
    public Collection<String> listAuthorities(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm)
            throws NoSuchRealmException {
        logger.debug("list idp authorities for realm {}",
                StringUtils.trimAllWhitespace(realm));
        return authorityService.getAuthorities().stream()
                .map(a -> a.getAuthorityId()).collect(Collectors.toList());
    }

    /*
     * Identity providers
     * 
     * Manage only realm providers, with config stored
     */
    @GetMapping("/idps/{realm}")
    @Operation(summary = "list identity providers from a given realm")
    public Collection<ConfigurableIdentityProvider> listIdps(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm)
            throws NoSuchRealmException {
        logger.debug("list idp for realm {}",
                StringUtils.trimAllWhitespace(realm));

        return providerManager.listProviders(realm)
                .stream()
                .map(cp -> {
                    cp.setRegistered(providerManager.isProviderRegistered(realm, cp));
                    return cp;
                }).collect(Collectors.toList());
    }

    @PostMapping("/idps/{realm}")
    @Operation(summary = "add a new identity provider to a given realm")
    public ConfigurableIdentityProvider addIdp(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @RequestBody @Valid @NotNull ConfigurableIdentityProvider registration)
            throws NoSuchRealmException, NoSuchProviderException, RegistrationException, SystemException,
            NoSuchAuthorityException {
        logger.debug("add idp to realm {}",
                StringUtils.trimAllWhitespace(realm));

        // unpack and build model
        String id = registration.getProvider();
        String authority = registration.getAuthority();
        String name = registration.getName();
        String description = registration.getDescription();
        String persistence = registration.getPersistence();
        String events = registration.getEvents();
        Integer position = registration.getPosition();
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
        provider.setPosition(position);

        provider.setConfiguration(configuration);
        provider.setHookFunctions(hookFunctions);

        if (logger.isTraceEnabled()) {
            logger.trace("idp bean: " + StringUtils.trimAllWhitespace(provider.toString()));
        }

        provider = providerManager.addProvider(realm, provider);

        return provider;
    }

    @GetMapping("/idps/{realm}/{providerId}")
    @Operation(summary = "get a specific identity provider from a given realm")
    public ConfigurableIdentityProvider getIdp(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId)
            throws NoSuchProviderException, NoSuchRealmException, NoSuchAuthorityException {
        logger.debug("get idp {} for realm {}",
                StringUtils.trimAllWhitespace(providerId), StringUtils.trimAllWhitespace(realm));

        ConfigurableIdentityProvider provider = providerManager.getProvider(realm, providerId);

        // merge default properties
        Map<String, Serializable> configuration = new HashMap<>();
        configuration.putAll(provider.getConfiguration());
        ConfigurableProperties cp = providerManager.getConfigurableProperties(realm, provider.getAuthority());
        Map<String, Serializable> dcp = cp.getConfiguration();
        dcp.entrySet().forEach(e -> {
            configuration.putIfAbsent(e.getKey(), e.getValue());
        });
        provider.setConfiguration(configuration);

        // check if registered
        boolean isRegistered = providerManager.isProviderRegistered(realm, provider);
        provider.setRegistered(isRegistered);

        return provider;
    }

    @PutMapping("/idps/{realm}/{providerId}")
    @Operation(summary = "update a specific identity provider in a given realm")
    public ConfigurableIdentityProvider updateIdp(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId,
            @RequestBody @Valid @NotNull ConfigurableIdentityProvider registration,
            @RequestParam(required = false, defaultValue = "false") Optional<Boolean> force)
            throws NoSuchRealmException, NoSuchProviderException, NoSuchAuthorityException {
        logger.debug("update idp {} for realm {}",
                StringUtils.trimAllWhitespace(providerId), StringUtils.trimAllWhitespace(realm));

        ConfigurableIdentityProvider provider = providerManager.getProvider(realm, providerId);

        // if force disable provider
        boolean forceRegistration = force.orElse(false);
        if (forceRegistration && providerManager.isProviderRegistered(realm, provider)) {
            try {
                provider = providerManager.unregisterProvider(realm, providerId);
            } catch (NoSuchAuthorityException e) {
                // skip
            }
        }

        // we update only configuration
        String name = registration.getName();
        String description = registration.getDescription();
        String persistence = registration.getPersistence();
        boolean enabled = registration.isEnabled();
        String events = registration.getEvents();
        Integer position = registration.getPosition();
        boolean linkable = registration.isLinkable();

        Map<String, Serializable> configuration = registration.getConfiguration();
        Map<String, String> hookFunctions = registration.getHookFunctions();

        provider.setName(name);
        provider.setDescription(description);

        provider.setEnabled(enabled);

        provider.setPersistence(persistence);
        provider.setLinkable(linkable);
        provider.setEvents(events);
        provider.setPosition(position);

        provider.setHookFunctions(hookFunctions);
        provider.setConfiguration(configuration);

        if (logger.isTraceEnabled()) {
            logger.trace("idp bean: " + StringUtils.trimAllWhitespace(provider.toString()));
        }

        provider = providerManager.updateProvider(realm, providerId, provider);

        // if force and enabled try to register
        if (forceRegistration && provider.isEnabled()) {
            try {
                provider = providerManager.registerProvider(realm, providerId);
            } catch (Exception e) {
                // ignore
            }
        }

        // check if registered
        boolean isRegistered = providerManager.isProviderRegistered(realm, provider);
        provider.setRegistered(isRegistered);

        return provider;
    }

    @DeleteMapping("/idps/{realm}/{providerId}")
    @Operation(summary = "delete a specific identity provider from a given realm")
    public void deleteIdp(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId)
            throws NoSuchProviderException, NoSuchRealmException, SystemException, NoSuchAuthorityException {
        logger.debug("delete idp {} for realm {}",
                StringUtils.trimAllWhitespace(providerId), StringUtils.trimAllWhitespace(realm));

        providerManager.deleteProvider(realm, providerId);

    }

    /*
     * Registration with authorities
     */

    @PutMapping("/idps/{realm}/{providerId}/status")
    @Operation(summary = "activate a specific identity provider from a given realm")
    public ConfigurableIdentityProvider registerIdp(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId)
            throws RegistrationException, NoSuchProviderException, NoSuchRealmException, NoSuchAuthorityException {
        logger.debug("register idp {} for realm {}",
                StringUtils.trimAllWhitespace(providerId), StringUtils.trimAllWhitespace(realm));

        ConfigurableIdentityProvider provider = providerManager.getProvider(realm, providerId);
        provider = providerManager.registerProvider(realm, providerId);

        // check if registered
        boolean isRegistered = providerManager.isProviderRegistered(realm, provider);
        provider.setRegistered(isRegistered);

        return provider;

    }

    @DeleteMapping("/idps/{realm}/{providerId}/status")
    @Operation(summary = "deactivate a specific identity provider from a given realm")
    public ConfigurableIdentityProvider unregisterIdp(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId)
            throws NoSuchProviderException, NoSuchRealmException, NoSuchAuthorityException {
        logger.debug("unregister idp {} for realm {}",
                StringUtils.trimAllWhitespace(providerId), StringUtils.trimAllWhitespace(realm));

        ConfigurableIdentityProvider provider = providerManager.getProvider(realm, providerId);
        provider = providerManager.unregisterProvider(realm, providerId);

        // check if registered
        boolean isRegistered = providerManager.isProviderRegistered(realm, provider);
        provider.setRegistered(isRegistered);

        return provider;

    }

    /*
     * Configuration schema
     */
    @GetMapping("/idps/{realm}/{providerId}/schema")
    @Operation(summary = "get an identity provider configuration schema")
    public JsonSchema getIdpConfigurationSchema(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId)
            throws NoSuchProviderException, NoSuchRealmException, NoSuchAuthorityException {
        logger.debug("get idp config schema for {} for realm {}",
                StringUtils.trimAllWhitespace(providerId), StringUtils.trimAllWhitespace(realm));

        ConfigurableIdentityProvider provider = providerManager.getProvider(realm, providerId);
        return providerManager.getConfigurationSchema(realm, provider.getAuthority());
    }

}
