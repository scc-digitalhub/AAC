package it.smartcommunitylab.aac.api;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.api.scopes.ApiProviderScope;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.core.ProviderManager;
import it.smartcommunitylab.aac.core.base.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.core.base.ConfigurableProvider;

@RestController
@RequestMapping("api")
public class ProviderController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ProviderManager providerManager;

    @Autowired
    @Qualifier("yamlObjectMapper")
    private ObjectMapper yamlObjectMapper;

    /*
     * Identity providers
     * 
     * Manage only realm providers, with config stored
     */
    @GetMapping("/idp_templates/{realm}")
    @PreAuthorize("(hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN')) and hasAuthority('SCOPE_" + ApiProviderScope.SCOPE + "')")
    public Collection<ConfigurableProvider> listTemplates(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm) throws NoSuchRealmException {
        logger.debug("list idp templates for realm " + String.valueOf(realm));
        return providerManager.listProviderConfigurationTemplates(realm, ConfigurableProvider.TYPE_IDENTITY);
    }

    @GetMapping("/idp/{realm}")
    @PreAuthorize("(hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN')) and hasAuthority('SCOPE_" + ApiProviderScope.SCOPE + "')")
    public Collection<ConfigurableIdentityProvider> listIdps(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm) throws NoSuchRealmException {
        logger.debug("list idp for realm " + String.valueOf(realm));

        return providerManager.listIdentityProviders(realm)
                .stream()
                .map(cp -> {
                    cp.setRegistered(providerManager.isProviderRegistered(cp));
                    return cp;
                }).collect(Collectors.toList());
    }

    @GetMapping("/idp/{realm}/{providerId}")
    @PreAuthorize("(hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN')) and hasAuthority('SCOPE_" + ApiProviderScope.SCOPE + "')")
    public ConfigurableIdentityProvider getIdp(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId)
            throws NoSuchProviderException, NoSuchRealmException {

        logger.debug("get idp " + String.valueOf(providerId) + " for realm " + String.valueOf(realm));

        ConfigurableIdentityProvider provider = providerManager.getIdentityProvider(realm, providerId);

        // check if registered
        boolean isRegistered = providerManager.isProviderRegistered(provider);
        provider.setRegistered(isRegistered);

//        // if registered fetch active configuration
//        if (isRegistered) {
//            IdentityProvider idp = providerManager.getIdentityProvider(providerId);
//            Map<String, Serializable> configMap = idp.getConfiguration().getConfiguration();
//            // we replace config instead of merging, when active config can not be
//            // modified anyway
//            provider.setConfiguration(configMap);
//        }

        return provider;
    }

    @PostMapping("/idp/{realm}")
    @PreAuthorize("(hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN')) and hasAuthority('SCOPE_" + ApiProviderScope.SCOPE + "')")
    public ConfigurableIdentityProvider addIdp(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @Valid @RequestBody ConfigurableIdentityProvider registration) throws NoSuchRealmException {

        logger.debug("add idp to realm " + String.valueOf(realm));

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
            logger.trace("idp bean: " + String.valueOf(provider));
        }

        provider = providerManager.addIdentityProvider(realm, provider);

        return provider;
    }

    @PutMapping("/idp/{realm}/{providerId}")
    @PreAuthorize("(hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN')) and hasAuthority('SCOPE_" + ApiProviderScope.SCOPE + "')")
    public ConfigurableIdentityProvider updateIdp(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId,
            @Valid @RequestBody ConfigurableIdentityProvider registration,
            @RequestParam(required = false, defaultValue = "false") Optional<Boolean> force)
            throws NoSuchRealmException, NoSuchProviderException {
        logger.debug("update idp " + String.valueOf(providerId) + " for realm " + String.valueOf(realm));

        ConfigurableIdentityProvider provider = providerManager.getIdentityProvider(realm, providerId);

        // if force disable provider
        boolean forceRegistration = force.orElse(false);
        if (forceRegistration && providerManager.isProviderRegistered(provider)) {
            provider = providerManager.unregisterIdentityProvider(realm, providerId);
        }

        // we update only configuration
        String name = registration.getName();
        String description = registration.getDescription();
        String persistence = registration.getPersistence();
        boolean enabled = registration.isEnabled();
        String events = registration.getEvents();
        boolean linkable = registration.isLinkable();

        Map<String, Serializable> configuration = registration.getConfiguration();
        Map<String, String> hookFunctions = registration.getHookFunctions();

        provider.setName(name);
        provider.setDescription(description);
        provider.setEnabled(enabled);
        provider.setPersistence(persistence);
        provider.setLinkable(linkable);
        provider.setEvents(events);
        provider.setHookFunctions(hookFunctions);
        provider.setConfiguration(configuration);

        if (logger.isTraceEnabled()) {
            logger.trace("idp bean: " + String.valueOf(provider));
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
        boolean isRegistered = providerManager.isProviderRegistered(provider);
        provider.setRegistered(isRegistered);

        return provider;
    }

    @DeleteMapping("/idp/{realm}/{providerId}")
    @PreAuthorize("(hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN')) and hasAuthority('SCOPE_" + ApiProviderScope.SCOPE + "')")
    public void deleteIdp(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId)
            throws NoSuchProviderException, NoSuchRealmException {
        logger.debug("delete idp " + String.valueOf(providerId) + " for realm " + String.valueOf(realm));
        providerManager.deleteIdentityProvider(realm, providerId);

    }

    @PutMapping("/idp/{realm}")
    @PreAuthorize("(hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN')) and hasAuthority('SCOPE_" + ApiProviderScope.SCOPE + "')")
    public ConfigurableIdentityProvider importProvider(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @RequestParam("file") @Valid @NotNull @NotBlank MultipartFile file) throws Exception {
        logger.debug("import idp to realm " + String.valueOf(realm));

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("empty file");
        }

        if (file.getContentType() != null &&
                (!file.getContentType().equals(SystemKeys.MEDIA_TYPE_YAML.toString()) &&
                        !file.getContentType().equals(SystemKeys.MEDIA_TYPE_YML.toString()))) {
            throw new IllegalArgumentException("invalid file");
        }
        try {
            ConfigurableIdentityProvider registration = yamlObjectMapper.readValue(file.getInputStream(),
                    ConfigurableIdentityProvider.class);

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
                logger.trace("idp bean: " + String.valueOf(provider));
            }

            provider = providerManager.addIdentityProvider(realm, provider);

            return provider;

        } catch (Exception e) {
            logger.error("import idp error: " + e.getMessage());
            throw e;
        }

    }

    /*
     * Registration with authorities
     */

    @PutMapping("/idp/{realm}/{providerId}/status")
    @PreAuthorize("(hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN')) and hasAuthority('SCOPE_" + ApiProviderScope.SCOPE + "')")
    public ConfigurableIdentityProvider registerIdp(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId)
            throws NoSuchProviderException, NoSuchRealmException {
        logger.debug("register idp " + String.valueOf(providerId) + " for realm " + String.valueOf(realm));

        ConfigurableIdentityProvider provider = providerManager.getIdentityProvider(realm, providerId);
        provider = providerManager.registerIdentityProvider(realm, providerId);

        // check if registered
        boolean isRegistered = providerManager.isProviderRegistered(provider);
        provider.setRegistered(isRegistered);

        return provider;

    }

    @DeleteMapping("/idp/{realm}/{providerId}/status")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "') or hasAuthority(#realm+':ROLE_ADMIN')")
    public ConfigurableIdentityProvider unregisterIdp(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId)
            throws NoSuchProviderException, NoSuchRealmException {
        logger.debug("unregister idp " + String.valueOf(providerId) + " for realm " + String.valueOf(realm));

        ConfigurableIdentityProvider provider = providerManager.getIdentityProvider(realm, providerId);
        provider = providerManager.unregisterIdentityProvider(realm, providerId);

        // check if registered
        boolean isRegistered = providerManager.isProviderRegistered(provider);
        provider.setRegistered(isRegistered);

        return provider;

    }

    /*
     * Configuration schema
     */
    @GetMapping("/idp_schema/{authority}")
    public JsonSchema getConfigurationSchema(
            @PathVariable(required = true) @Valid @NotBlank String authority)
            throws IllegalArgumentException {
        logger.debug("get idp config schema  for authority " + String.valueOf(authority));

        return providerManager.getConfigurationSchema(SystemKeys.RESOURCE_IDENTITY, authority);
    }

}
