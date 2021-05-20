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
import it.smartcommunitylab.aac.api.scopes.ApiAuditScope;
import it.smartcommunitylab.aac.api.scopes.ApiProviderScope;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.core.ProviderManager;
import it.smartcommunitylab.aac.core.base.ConfigurableProvider;
import it.smartcommunitylab.aac.core.provider.IdentityProvider;

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

        return providerManager.listProviderConfigurationTemplates(realm, ConfigurableProvider.TYPE_IDENTITY);
    }

    @GetMapping("/idp/{realm}")
    @PreAuthorize("(hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN')) and hasAuthority('SCOPE_" + ApiProviderScope.SCOPE + "')")
    public Collection<ConfigurableProvider> listIdps(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm) throws NoSuchRealmException {

        return providerManager.listProviders(realm, ConfigurableProvider.TYPE_IDENTITY)
                .stream()
                .map(cp -> {
                    cp.setRegistered(providerManager.isProviderRegistered(cp));
                    return cp;
                }).collect(Collectors.toList());
    }

    @GetMapping("/idp/{realm}/{providerId}")
    @PreAuthorize("(hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN')) and hasAuthority('SCOPE_" + ApiProviderScope.SCOPE + "')")
    public ConfigurableProvider getIdp(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId)
            throws NoSuchProviderException, NoSuchRealmException {
        ConfigurableProvider provider = providerManager.getProvider(realm, ConfigurableProvider.TYPE_IDENTITY,
                providerId);

        // check if registered
        boolean isRegistered = providerManager.isProviderRegistered(provider);
        provider.setRegistered(isRegistered);

        // if registered fetch active configuration
        if (isRegistered) {
            IdentityProvider idp = providerManager.getIdentityProvider(providerId);
            Map<String, Serializable> configMap = idp.getConfiguration().getConfiguration();
            // we replace config instead of merging, when active config can not be
            // modified anyway
            provider.setConfiguration(configMap);
        }

        return provider;
    }

    @PostMapping("/idp/{realm}")
    @PreAuthorize("(hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN')) and hasAuthority('SCOPE_" + ApiProviderScope.SCOPE + "')")
    public ConfigurableProvider addIdp(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @Valid @RequestBody ConfigurableProvider registration) throws NoSuchRealmException {

        // unpack and build model
        String id = registration.getProvider();
        String authority = registration.getAuthority();
        String type = registration.getType();
        String name = registration.getName();
        String description = registration.getDescription();
        String persistence = registration.getPersistence();
        Map<String, Serializable> configuration = registration.getConfiguration();

        ConfigurableProvider provider = new ConfigurableProvider(authority, id, realm);
        provider.setName(name);
        provider.setDescription(description);
        provider.setType(type);
        provider.setEnabled(false);
        provider.setPersistence(persistence);
        provider.setConfiguration(configuration);

        provider = providerManager.addProvider(realm, provider);

        return provider;
    }

    @PutMapping("/idp/{realm}/{providerId}")
    @PreAuthorize("(hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN')) and hasAuthority('SCOPE_" + ApiProviderScope.SCOPE + "')")
    public ConfigurableProvider updateIdp(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId,
            @Valid @RequestBody ConfigurableProvider registration,
            @RequestParam(required = false, defaultValue = "false") Optional<Boolean> force)
            throws NoSuchRealmException, NoSuchProviderException {

        ConfigurableProvider provider = providerManager.getProvider(realm, providerId);

        // if force disable provider
        boolean forceRegistration = force.orElse(false);
        if (forceRegistration && providerManager.isProviderRegistered(provider)) {
            provider = providerManager.unregisterProvider(realm, providerId);
        }

        // we update only configuration
        String name = registration.getName();
        String description = registration.getDescription();
        String persistence = registration.getPersistence();
        boolean enabled = registration.isEnabled();
        Map<String, Serializable> configuration = registration.getConfiguration();
        Map<String, String> hookFunctions = registration.getHookFunctions();

        provider.setName(name);
        provider.setDescription(description);
        provider.setPersistence(persistence);
        provider.setConfiguration(configuration);
        provider.setEnabled(enabled);
        provider.setHookFunctions(hookFunctions);

        provider = providerManager.updateProvider(realm, providerId, provider);

        // if force and flah enable try to register
        if (forceRegistration && provider.isEnabled()) {
            try {
                provider = providerManager.registerProvider(realm, providerId);
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

        providerManager.deleteProvider(realm, providerId);

    }

    @PutMapping("/idp/{realm}")
    @PreAuthorize("(hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN')) and hasAuthority('SCOPE_" + ApiProviderScope.SCOPE + "')")
    public ConfigurableProvider importProvider(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @RequestParam("file") @Valid @NotNull @NotBlank MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("empty file");
        }

        if (file.getContentType() != null &&
                (!file.getContentType().equals(SystemKeys.MEDIA_TYPE_YAML.toString()) &&
                        !file.getContentType().equals(SystemKeys.MEDIA_TYPE_YML.toString()))) {
            throw new IllegalArgumentException("invalid file");
        }
        try {
            ConfigurableProvider registration = yamlObjectMapper.readValue(file.getInputStream(),
                    ConfigurableProvider.class);

            // unpack and build model
            String id = registration.getProvider();
            String authority = registration.getAuthority();
            String type = registration.getType();
            String name = registration.getName();
            String description = registration.getDescription();
            String persistence = registration.getPersistence();
            Map<String, Serializable> configuration = registration.getConfiguration();
            Map<String, String> hookFunctions = registration.getHookFunctions();

            ConfigurableProvider provider = new ConfigurableProvider(authority, id, realm);
            provider.setName(name);
            provider.setDescription(description);
            provider.setType(type);
            provider.setEnabled(false);
            provider.setPersistence(persistence);
            provider.setConfiguration(configuration);
            provider.setHookFunctions(hookFunctions);

            provider = providerManager.addProvider(realm, provider);

            return provider;

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }

    }

    /*
     * Registration with authorities
     */

    @PutMapping("/idp/{realm}/{providerId}/status")
    @PreAuthorize("(hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN')) and hasAuthority('SCOPE_" + ApiProviderScope.SCOPE + "')")
    public ConfigurableProvider registerIdp(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId)
            throws NoSuchProviderException, NoSuchRealmException {

        ConfigurableProvider provider = providerManager.getProvider(realm, providerId);
        provider = providerManager.registerProvider(realm, providerId);

        // check if registered
        boolean isRegistered = providerManager.isProviderRegistered(provider);
        provider.setRegistered(isRegistered);

        return provider;

    }

    @DeleteMapping("/idp/{realm}/{providerId}/status")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "') or hasAuthority(#realm+':ROLE_ADMIN')")
    public ConfigurableProvider unregisterIdp(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId)
            throws NoSuchProviderException, NoSuchRealmException {

        ConfigurableProvider provider = providerManager.getProvider(realm, providerId);
        provider = providerManager.unregisterProvider(realm, providerId);

        // check if registered
        boolean isRegistered = providerManager.isProviderRegistered(provider);
        provider.setRegistered(isRegistered);

        return provider;

    }

    /*
     * Configuration schema
     */
    @GetMapping("/idp_schema/{type}/{authority}")
    public JsonSchema getConfigurationSchema(
            @PathVariable(required = true) String type, @PathVariable(required = true) String authority)
            throws IllegalArgumentException {

        return providerManager.getConfigurationSchema(type, authority);
    }

}
