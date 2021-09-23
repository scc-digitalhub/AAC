package it.smartcommunitylab.aac.api;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
import it.smartcommunitylab.aac.core.base.ConfigurableAttributeProvider;

@RestController
@RequestMapping("api")
public class AttributeProviderController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ProviderManager providerManager;

    @Autowired
    @Qualifier("yamlObjectMapper")
    private ObjectMapper yamlObjectMapper;

    /*
     * Attribute providers
     * 
     * Manage only realm providers, with config stored
     */

    @GetMapping("/ap/{realm}")
    @PreAuthorize("(hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN')) and hasAuthority('SCOPE_" + ApiProviderScope.SCOPE + "')")
    public Collection<ConfigurableAttributeProvider> listAps(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm) throws NoSuchRealmException {
        logger.debug("list ap for realm " + String.valueOf(realm));

        return providerManager.listAttributeProviders(realm)
                .stream()
                .map(cp -> {
                    cp.setRegistered(providerManager.isProviderRegistered(cp));
                    return cp;
                }).collect(Collectors.toList());
    }

    @GetMapping("/ap/{realm}/{providerId}")
    @PreAuthorize("(hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN')) and hasAuthority('SCOPE_" + ApiProviderScope.SCOPE + "')")
    public ConfigurableAttributeProvider getAp(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId)
            throws NoSuchProviderException, NoSuchRealmException {

        logger.debug("get ap " + String.valueOf(providerId) + " for realm " + String.valueOf(realm));

        ConfigurableAttributeProvider provider = providerManager.getAttributeProvider(realm, providerId);

        // check if registered
        boolean isRegistered = providerManager.isProviderRegistered(provider);
        provider.setRegistered(isRegistered);

//        // if registered fetch active configuration
//        if (isRegistered) {
//            AttributeProvider ap = providerManager.getAttributeProvider(providerId);
//            Map<String, Serializable> configMap = ap.getConfiguration().getConfiguration();
//            // we replace config instead of merging, when active config can not be
//            // modified anyway
//            provider.setConfiguration(configMap);
//        }

        return provider;
    }

    @PostMapping("/ap/{realm}")
    @PreAuthorize("(hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN')) and hasAuthority('SCOPE_" + ApiProviderScope.SCOPE + "')")
    public ConfigurableAttributeProvider addAp(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @Valid @RequestBody ConfigurableAttributeProvider registration) throws NoSuchRealmException {

        logger.debug("add ap to realm " + String.valueOf(realm));

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
    @PreAuthorize("(hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN')) and hasAuthority('SCOPE_" + ApiProviderScope.SCOPE + "')")
    public ConfigurableAttributeProvider updateAp(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId,
            @Valid @RequestBody ConfigurableAttributeProvider registration,
            @RequestParam(required = false, defaultValue = "false") Optional<Boolean> force)
            throws NoSuchRealmException, NoSuchProviderException {
        logger.debug("update ap " + String.valueOf(providerId) + " for realm " + String.valueOf(realm));

        ConfigurableAttributeProvider provider = providerManager.getAttributeProvider(realm, providerId);

        // if force disable provider
        boolean forceRegistration = force.orElse(false);
        if (forceRegistration && providerManager.isProviderRegistered(provider)) {
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
        boolean isRegistered = providerManager.isProviderRegistered(provider);
        provider.setRegistered(isRegistered);

        return provider;
    }

    @DeleteMapping("/ap/{realm}/{providerId}")
    @PreAuthorize("(hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN')) and hasAuthority('SCOPE_" + ApiProviderScope.SCOPE + "')")
    public void deleteAp(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId)
            throws NoSuchProviderException, NoSuchRealmException {
        logger.debug("delete ap " + String.valueOf(providerId) + " for realm " + String.valueOf(realm));
        providerManager.deleteAttributeProvider(realm, providerId);

    }

    @PutMapping("/ap/{realm}")
    @PreAuthorize("(hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN')) and hasAuthority('SCOPE_" + ApiProviderScope.SCOPE + "')")
    public ConfigurableAttributeProvider importAp(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @RequestParam("file") @Valid @NotNull @NotBlank MultipartFile file) throws Exception {
        logger.debug("import ap to realm " + String.valueOf(realm));

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("empty file");
        }

        if (file.getContentType() != null &&
                (!file.getContentType().equals(SystemKeys.MEDIA_TYPE_YAML.toString()) &&
                        !file.getContentType().equals(SystemKeys.MEDIA_TYPE_YML.toString()))) {
            throw new IllegalArgumentException("invalid file");
        }
        try {
            ConfigurableAttributeProvider registration = yamlObjectMapper.readValue(file.getInputStream(),
                    ConfigurableAttributeProvider.class);

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

        } catch (Exception e) {
            logger.error("import ap error: " + e.getMessage());
            throw e;
        }

    }

    /*
     * Registration with authorities
     */

    @PutMapping("/ap/{realm}/{providerId}/status")
    @PreAuthorize("(hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN')) and hasAuthority('SCOPE_" + ApiProviderScope.SCOPE + "')")
    public ConfigurableAttributeProvider registerAp(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId)
            throws NoSuchProviderException, NoSuchRealmException {
        logger.debug("register ap " + String.valueOf(providerId) + " for realm " + String.valueOf(realm));

        ConfigurableAttributeProvider provider = providerManager.getAttributeProvider(realm, providerId);
        provider = providerManager.registerAttributeProvider(realm, providerId);

        // check if registered
        boolean isRegistered = providerManager.isProviderRegistered(provider);
        provider.setRegistered(isRegistered);

        return provider;

    }

    @DeleteMapping("/ap/{realm}/{providerId}/status")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "') or hasAuthority(#realm+':ROLE_ADMIN')")
    public ConfigurableAttributeProvider unregisterAp(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId)
            throws NoSuchProviderException, NoSuchRealmException {
        logger.debug("unregister ap " + String.valueOf(providerId) + " for realm " + String.valueOf(realm));

        ConfigurableAttributeProvider provider = providerManager.getAttributeProvider(realm, providerId);
        provider = providerManager.unregisterAttributeProvider(realm, providerId);

        // check if registered
        boolean isRegistered = providerManager.isProviderRegistered(provider);
        provider.setRegistered(isRegistered);

        return provider;

    }

    /*
     * Configuration schema
     */
    @GetMapping("/ap/{realm}/{providerId}/schema")
    @PreAuthorize("(hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN')) and hasAuthority('SCOPE_" + ApiProviderScope.SCOPE + "')")
    public JsonSchema getApConfigurationSchema(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId)
            throws NoSuchProviderException, NoSuchRealmException {

        logger.debug("get ap config schema for " + String.valueOf(providerId) + " for realm " + String.valueOf(realm));

        ConfigurableAttributeProvider provider = providerManager.getAttributeProvider(realm, providerId);
        return providerManager.getConfigurationSchema(SystemKeys.RESOURCE_ATTRIBUTES, provider.getAuthority());
    }

}
