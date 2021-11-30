package it.smartcommunitylab.aac.dev;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.yaml.snakeyaml.Yaml;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;

import io.swagger.v3.oas.annotations.Hidden;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.controller.BaseAttributeProviderController;
import it.smartcommunitylab.aac.core.AuthorityManager;
import it.smartcommunitylab.aac.core.auth.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.core.auth.UserAuthentication;
import it.smartcommunitylab.aac.core.base.ConfigurableAttributeProvider;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.provider.AttributeProvider;
import it.smartcommunitylab.aac.dto.FunctionValidationBean;

@RestController
@Hidden
@RequestMapping("/console/dev")
public class DevAttributeProviderController extends BaseAttributeProviderController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final TypeReference<Map<String, List<ConfigurableAttributeProvider>>> typeRef = new TypeReference<Map<String, List<ConfigurableAttributeProvider>>>() {
    };

    @Autowired
    private AuthorityManager authorityManager;

    @Autowired
    @Qualifier("yamlObjectMapper")
    private ObjectMapper yamlObjectMapper;

    /*
     * Providers
     */

    @Override
    @GetMapping("/ap/{realm}/{providerId}")
    public ConfigurableAttributeProvider getAp(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId)
            throws NoSuchProviderException, NoSuchRealmException {
        ConfigurableAttributeProvider provider = super.getAp(realm, providerId);

        // fetch also configuration schema
        JsonSchema schema = providerManager.getConfigurationSchema(realm, provider.getType(), provider.getAuthority());
        provider.setSchema(schema);

        return provider;
    }

    @Override
    @PostMapping("/ap/{realm}")
    public ConfigurableAttributeProvider addAp(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @RequestBody @Valid @NotNull ConfigurableAttributeProvider registration) throws NoSuchRealmException {
        ConfigurableAttributeProvider provider = super.addAp(realm, registration);

        // fetch also configuration schema
        JsonSchema schema = providerManager.getConfigurationSchema(realm, provider.getType(), provider.getAuthority());
        provider.setSchema(schema);

        return provider;
    }

    @Override
    @PutMapping("/ap/{realm}/{providerId}")
    public ConfigurableAttributeProvider updateAp(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId,
            @RequestBody @Valid @NotNull ConfigurableAttributeProvider registration,
            @RequestParam(required = false, defaultValue = "false") Optional<Boolean> force)
            throws NoSuchRealmException, NoSuchProviderException {
        ConfigurableAttributeProvider provider = super.updateAp(realm, providerId, registration, Optional.of(false));

        // fetch also configuration schema
        JsonSchema schema = providerManager.getConfigurationSchema(realm, provider.getType(), provider.getAuthority());
        provider.setSchema(schema);

        return provider;
    }

//    @PutMapping("/realms/{realm}/aps/{providerId}")
//    public ResponseEntity<ConfigurableAttributeProvider> updateRealmProvider(
//            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
//            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId,
//            @RequestBody @Valid @NotNull ConfigurableAttributeProvider registration)
//            throws NoSuchRealmException, NoSuchUserException, SystemException, NoSuchProviderException {
//
//        ConfigurableAttributeProvider provider = providerManager.getAttributeProvider(realm, providerId);
//
//        // we update only configuration
//        String name = registration.getName();
//        String description = registration.getDescription();
//        String persistence = registration.getPersistence();
//        String events = registration.getEvents();
//        Set<String> attributeSets = registration.getAttributeSets();
//        Map<String, Serializable> configuration = registration.getConfiguration();
//
//        provider.setName(name);
//        provider.setDescription(description);
//        provider.setPersistence(persistence);
//        provider.setEvents(events);
//        provider.setAttributeSets(attributeSets);
//        provider.setConfiguration(configuration);
//
//        provider = providerManager.updateAttributeProvider(realm, providerId, provider);
//
//        // check if registered
//        boolean isRegistered = providerManager.isProviderRegistered(realm, provider);
//        provider.setRegistered(isRegistered);
//
//        // fetch also configuration schema
//        JsonSchema schema = providerManager.getConfigurationSchema(realm, provider.getType(), provider.getAuthority());
//        provider.setSchema(schema);
//
//        return ResponseEntity.ok(provider);
//    }

//    @PutMapping("/realms/{realm}/aps/{providerId}/state")
//    public ResponseEntity<ConfigurableAttributeProvider> updateRealmProviderState(
//            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
//            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId,
//            @RequestBody @Valid @NotNull ConfigurableAttributeProvider registration)
//            throws NoSuchRealmException, NoSuchUserException, SystemException, NoSuchProviderException {
//
//        ConfigurableAttributeProvider provider = providerManager.getAttributeProvider(realm, providerId);
//        boolean enabled = registration.isEnabled();
//
//        if (enabled) {
//            provider = providerManager.registerAttributeProvider(realm, providerId);
//        } else {
//            provider = providerManager.unregisterAttributeProvider(realm, providerId);
//        }
//
//        // check if registered
//        boolean isRegistered = providerManager.isProviderRegistered(realm, provider);
//        provider.setRegistered(isRegistered);
//
//        return ResponseEntity.ok(provider);
//    }

    /*
     * Test
     */
    @GetMapping("/ap/{realm}/{providerId}/test")
    public ResponseEntity<FunctionValidationBean> testRealmProvider(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId,
            Authentication auth, HttpServletResponse res)
            throws NoSuchProviderException, NoSuchRealmException, SystemException, IOException {
        ConfigurableAttributeProvider provider = providerManager.getAttributeProvider(realm, providerId);

        // check if registered
        boolean isRegistered = providerManager.isProviderRegistered(realm, provider);
        if (!isRegistered) {
            throw new IllegalArgumentException("provider is not active");
        }

        AttributeProvider ap = authorityManager.getAttributeProvider(providerId);

        // authentication should be a user authentication
        if (!(auth instanceof UserAuthentication)) {
            throw new InsufficientAuthenticationException("not a user authentication");
        }

        UserAuthentication userAuth = (UserAuthentication) auth;
        UserAuthenticatedPrincipal principal = userAuth.getAuthentications().iterator().next().getPrincipal();
        FunctionValidationBean function = new FunctionValidationBean();
        function.setName("attributes");
        function.setCode(providerId);

        // mock mapping done by provider
        Map<String, Serializable> principalAttributes = new HashMap<>();
        // get all attributes from principal
        Map<String, String> attributes = principal.getAttributes();
        // TODO handle all attributes not only strings.
        principalAttributes.putAll(attributes.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue())));

        // we use also name from principal
        String name = principal.getName();
        principalAttributes.put("name", name);

        // add auth info
        principalAttributes.put("authority", principal.getAuthority());
        principalAttributes.put("provider", principal.getProvider());
        principalAttributes.put("realm", principal.getRealm());
        function.setContext(principalAttributes);

        try {
            Collection<UserAttributes> userAttributes = ap.convertAttributes(principal, userAuth.getSubjectId());
            if (userAttributes == null) {
                userAttributes = Collections.emptyList();
            }

            Map<String, Serializable> result = new HashMap<>();
            for (UserAttributes attr : userAttributes) {
                result.put(attr.getAttributesId(), new ArrayList<>(attr.getAttributes()));
            }
            function.setResult(result);
        } catch (RuntimeException e) {
            // translate error
            function.addError(e.getMessage());

        }

        return ResponseEntity.ok(function);

    }

    /*
     * Import/export for console
     */
    @GetMapping("/ap/{realm}/{providerId}/export")
    public void exportRealmProvider(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId,
            HttpServletResponse res)
            throws NoSuchProviderException, NoSuchRealmException, SystemException, IOException {
        ConfigurableAttributeProvider provider = providerManager.getAttributeProvider(realm, providerId);

//      String s = yaml.dump(clientApp);
        String s = yamlObjectMapper.writeValueAsString(provider);

        // write as file
        res.setContentType("text/yaml");
        res.setHeader("Content-Disposition", "attachment;filename=ap-" + provider.getName() + ".yaml");
        ServletOutputStream out = res.getOutputStream();
        out.write(s.getBytes(StandardCharsets.UTF_8));
        out.flush();
        out.close();

    }

    @PutMapping("/ap/{realm}")
    public Collection<ConfigurableAttributeProvider> importRealmProvider(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @RequestParam("file") @Valid @NotNull @NotBlank MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("empty file");
        }

        if (file.getContentType() == null) {
            throw new IllegalArgumentException("invalid file");
        }

        if (!SystemKeys.MEDIA_TYPE_YAML.toString().equals(file.getContentType())
                && !SystemKeys.MEDIA_TYPE_YML.toString().equals(file.getContentType())
                && !SystemKeys.MEDIA_TYPE_XYAML.toString().equals(file.getContentType())) {
            throw new IllegalArgumentException("invalid file");
        }

        try {
            List<ConfigurableAttributeProvider> providers = new ArrayList<>();
            boolean multiple = false;

            // read as raw yaml to check if collection
            Yaml yaml = new Yaml();
            Map<String, Object> obj = yaml.load(file.getInputStream());
            multiple = obj.containsKey("providers");

            if (multiple) {
                Map<String, List<ConfigurableAttributeProvider>> list = yamlObjectMapper.readValue(
                        file.getInputStream(),
                        typeRef);

                for (ConfigurableAttributeProvider registration : list.get("providers")) {
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

                    provider = providerManager.addAttributeProvider(realm, provider);

                    // fetch also configuration schema
                    JsonSchema schema = providerManager.getConfigurationSchema(realm, provider.getType(),
                            provider.getAuthority());
                    provider.setSchema(schema);
                    providers.add(provider);
                }
            } else {
                // try single element
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

                provider = providerManager.addAttributeProvider(realm, provider);

                // fetch also configuration schema
                JsonSchema schema = providerManager.getConfigurationSchema(realm, provider.getType(),
                        provider.getAuthority());
                provider.setSchema(schema);
                providers.add(provider);
            }
            return providers;

        } catch (Exception e) {
            logger.error("error importing providers: " + e.getMessage());
            throw e;
        }

    }

}
