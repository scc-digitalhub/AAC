package it.smartcommunitylab.aac.dev;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
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
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.ProviderManager;
import it.smartcommunitylab.aac.core.base.ConfigurableProvider;
import it.smartcommunitylab.aac.core.provider.IdentityProvider;
import springfox.documentation.annotations.ApiIgnore;

@RestController
@ApiIgnore
@RequestMapping("/console/dev")
public class DevProviderController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ProviderManager providerManager;

    @Autowired
    @Qualifier("yamlObjectMapper")
    private ObjectMapper yamlObjectMapper;

    /*
     * Providers
     */

    @GetMapping("/realms/{realm}/providers")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<Collection<ConfigurableProvider>> getRealmProviders(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm)
            throws NoSuchRealmException {

        List<ConfigurableProvider> providers = providerManager
                .listProviders(realm, ConfigurableProvider.TYPE_IDENTITY)
                .stream()
                .map(cp -> {
                    cp.setRegistered(providerManager.isProviderRegistered(cp));
                    return cp;
                }).collect(Collectors.toList());

        return ResponseEntity.ok(providers);
    }

    @GetMapping("/realms/{realm}/providertemplates")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "') or hasAuthority(#realm+':ROLE_ADMIN')")
    public ResponseEntity<Collection<ConfigurableProvider>> getRealmProviderTemplates(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm)
            throws NoSuchRealmException {

        List<ConfigurableProvider> providers = providerManager
                .listProviderConfigurationTemplates(realm, ConfigurableProvider.TYPE_IDENTITY)
                .stream()
                .map(cp -> {
                    cp.setRegistered(providerManager.isProviderRegistered(cp));
                    return cp;
                }).collect(Collectors.toList());

        return ResponseEntity.ok(providers);
    }

    @GetMapping("/console/dev/realms/{realm}/providers/{providerId:.*}")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "') or hasAuthority(#realm+':ROLE_ADMIN')")
    public ResponseEntity<ConfigurableProvider> getRealmProvider(
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

        // fetch also configuration schema
        JsonSchema schema = providerManager.getConfigurationSchema(provider.getType(), provider.getAuthority());
        provider.setSchema(schema);

        return ResponseEntity.ok(provider);
    }

    @DeleteMapping("/console/dev/realms/{realm}/providers/{providerId:.*}")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "') or hasAuthority(#realm+':ROLE_ADMIN')")
    public ResponseEntity<Void> deleteRealmProvider(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId)
            throws NoSuchRealmException, NoSuchUserException, SystemException, NoSuchProviderException {
        providerManager.deleteProvider(realm, providerId);
        return ResponseEntity.ok(null);
    }

    @PostMapping("/console/dev/realms/{realm}/providers")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "') or hasAuthority(#realm+':ROLE_ADMIN')")
    public ResponseEntity<ConfigurableProvider> createRealmProvider(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @Valid @RequestBody ConfigurableProvider registration)
            throws NoSuchRealmException, NoSuchUserException, SystemException, NoSuchProviderException {
        // unpack and build model
        String authority = registration.getAuthority();
        String type = registration.getType();
        String name = registration.getName();
        String description = registration.getDescription();
        String persistence = registration.getPersistence();
        String events = registration.getEvents();
        boolean linkable = registration.isLinkable();

        Map<String, Serializable> configuration = registration.getConfiguration();

        ConfigurableProvider provider = new ConfigurableProvider(authority, null, realm);
        provider.setName(name);
        provider.setDescription(description);
        provider.setType(type);
        provider.setEnabled(false);
        provider.setPersistence(persistence);
        provider.setLinkable(linkable);
        provider.setEvents(events);
        provider.setConfiguration(configuration);

        provider = providerManager.addProvider(realm, provider);

        // fetch also configuration schema
        JsonSchema schema = providerManager.getConfigurationSchema(provider.getType(), provider.getAuthority());
        provider.setSchema(schema);

        return ResponseEntity.ok(provider);
    }

    @PutMapping("/console/dev/realms/{realm}/providers/{providerId:.*}")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "') or hasAuthority(#realm+':ROLE_ADMIN')")
    public ResponseEntity<ConfigurableProvider> updateRealmProvider(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId,
            @Valid @RequestBody ConfigurableProvider registration)
            throws NoSuchRealmException, NoSuchUserException, SystemException, NoSuchProviderException {

        ConfigurableProvider provider = providerManager.getProvider(realm, providerId);

        // we update only configuration
        String name = registration.getName();
        String description = registration.getDescription();
        String persistence = registration.getPersistence();
        String events = registration.getEvents();
        boolean linkable = registration.isLinkable();

        Map<String, Serializable> configuration = registration.getConfiguration();
        Map<String, String> hookFunctions = registration.getHookFunctions();

        provider.setName(name);
        provider.setDescription(description);
        provider.setPersistence(persistence);
        provider.setLinkable(linkable);

        provider.setEvents(events);
        provider.setConfiguration(configuration);
        provider.setHookFunctions(hookFunctions);

        provider = providerManager.updateProvider(realm, providerId, provider);

        // check if registered
        boolean isRegistered = providerManager.isProviderRegistered(provider);
        provider.setRegistered(isRegistered);

        // fetch also configuration schema
        JsonSchema schema = providerManager.getConfigurationSchema(provider.getType(), provider.getAuthority());
        provider.setSchema(schema);

        return ResponseEntity.ok(provider);
    }

    @PutMapping("/console/dev/realms/{realm}/providers/{providerId}/state")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "') or hasAuthority(#realm+':ROLE_ADMIN')")
    public ResponseEntity<ConfigurableProvider> updateRealmProviderState(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId,
            @RequestBody ConfigurableProvider registration)
            throws NoSuchRealmException, NoSuchUserException, SystemException, NoSuchProviderException {

        ConfigurableProvider provider = providerManager.getProvider(realm, providerId);
        boolean enabled = registration.isEnabled();

        if (enabled) {
            provider = providerManager.registerProvider(realm, providerId);
        } else {
            provider = providerManager.unregisterProvider(realm, providerId);
        }

        // check if registered
        boolean isRegistered = providerManager.isProviderRegistered(provider);
        provider.setRegistered(isRegistered);

        return ResponseEntity.ok(provider);
    }

    @GetMapping("/console/dev/realms/{realm}/providers/{providerId:.*}/export")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "') or hasAuthority(#realm+':ROLE_ADMIN')")
    public void exportRealmProvider(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId,
            HttpServletResponse res)
            throws NoSuchProviderException, NoSuchRealmException, SystemException, IOException {
        ConfigurableProvider provider = providerManager.getProvider(realm, ConfigurableProvider.TYPE_IDENTITY,
                providerId);

//      String s = yaml.dump(clientApp);
        String s = yamlObjectMapper.writeValueAsString(provider);

        // write as file
        res.setContentType("text/yaml");
        res.setHeader("Content-Disposition", "attachment;filename=idp-" + provider.getName() + ".yaml");
        ServletOutputStream out = res.getOutputStream();
        out.print(s);
        out.flush();
        out.close();

    }

    @PutMapping("/console/dev/realms/{realm}/providers")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN
            + "') or hasAuthority(#realm+':ROLE_ADMIN') or hasAuthority(#realm+':ROLE_DEVELOPER')")
    public ResponseEntity<ConfigurableProvider> importRealmProvider(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @RequestParam("file") @Valid @NotNull @NotBlank MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("empty file");
        }

        if (file.getContentType() != null &&
                (!file.getContentType().equals(SystemKeys.MEDIA_TYPE_YAML.toString())
                        && !file.getContentType().equals(SystemKeys.MEDIA_TYPE_YML.toString())
                        && !file.getContentType().equals(SystemKeys.MEDIA_TYPE_XYAML.toString()))) {
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
            String events = registration.getEvents();
            Map<String, Serializable> configuration = registration.getConfiguration();
            Map<String, String> hookFunctions = registration.getHookFunctions();

            ConfigurableProvider provider = new ConfigurableProvider(authority, id, realm);
            provider.setName(name);
            provider.setDescription(description);
            provider.setType(type);
            provider.setEnabled(false);
            provider.setPersistence(persistence);
            provider.setEvents(events);
            provider.setConfiguration(configuration);
            provider.setHookFunctions(hookFunctions);

            provider = providerManager.addProvider(realm, provider);

            // fetch also configuration schema
            JsonSchema schema = providerManager.getConfigurationSchema(provider.getType(), provider.getAuthority());
            provider.setSchema(schema);

            return ResponseEntity.ok(provider);

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }

    }
}
