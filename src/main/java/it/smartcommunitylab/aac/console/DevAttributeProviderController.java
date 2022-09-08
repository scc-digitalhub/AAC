package it.smartcommunitylab.aac.console;

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
import java.util.stream.Collectors;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.yaml.snakeyaml.Yaml;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;

import io.swagger.v3.oas.annotations.Hidden;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchAuthorityException;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.controller.BaseAttributeProviderController;
import it.smartcommunitylab.aac.core.AuthorityManager;
import it.smartcommunitylab.aac.core.auth.UserAuthentication;
import it.smartcommunitylab.aac.core.model.ConfigurableAttributeProvider;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.model.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.core.provider.AttributeProvider;
import it.smartcommunitylab.aac.dto.FunctionValidationBean;

@RestController
@Hidden
@RequestMapping("/console/dev")
public class DevAttributeProviderController extends BaseAttributeProviderController {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final TypeReference<Map<String, List<ConfigurableAttributeProvider>>> typeRef = new TypeReference<Map<String, List<ConfigurableAttributeProvider>>>() {
    };
    private final String LIST_KEY = "providers";

    @Autowired
    private AuthorityManager authorityManager;

    @Autowired
    @Qualifier("yamlObjectMapper")
    private ObjectMapper yamlObjectMapper;

    /*
     * Providers
     */

    @Override
    @GetMapping("/aps/{realm}/{providerId}")
    public ConfigurableAttributeProvider getAp(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId)
            throws NoSuchProviderException, NoSuchRealmException {
        ConfigurableAttributeProvider provider = super.getAp(realm, providerId);

        // fetch also configuration schema
        try {
            JsonSchema schema = providerManager.getConfigurationSchema(realm, provider.getType(),
                    provider.getAuthority());
            provider.setSchema(schema);
        } catch (NoSuchAuthorityException e) {
            throw new NoSuchProviderException();
        }

        return provider;
    }

    @Override
    @PostMapping("/aps/{realm}")
    public ConfigurableAttributeProvider addAp(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @RequestBody @Valid @NotNull ConfigurableAttributeProvider registration)
            throws NoSuchRealmException, NoSuchAuthorityException {
        ConfigurableAttributeProvider provider = super.addAp(realm, registration);

        // fetch also configuration schema
        JsonSchema schema = providerManager.getConfigurationSchema(realm, provider.getType(), provider.getAuthority());
        provider.setSchema(schema);

        return provider;
    }

    @Override
    @PutMapping("/aps/{realm}/{providerId}")
    public ConfigurableAttributeProvider updateAp(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId,
            @RequestBody @Valid @NotNull ConfigurableAttributeProvider registration,
            @RequestParam(required = false, defaultValue = "false") Optional<Boolean> force)
            throws NoSuchRealmException, NoSuchProviderException, NoSuchAuthorityException {
        ConfigurableAttributeProvider provider = super.updateAp(realm, providerId, registration, Optional.of(false));

        // fetch also configuration schema
        JsonSchema schema = providerManager.getConfigurationSchema(realm, provider.getType(), provider.getAuthority());
        provider.setSchema(schema);

        return provider;
    }

    /*
     * Test
     */
    @GetMapping("/aps/{realm}/{providerId}/test")
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
        Map<String, Serializable> attributes = principal.getAttributes();
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
            Collection<UserAttributes> userAttributes = ap.convertPrincipalAttributes(principal,
                    userAuth.getSubjectId());
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

    @PutMapping("/aps/{realm}")
    public Collection<ConfigurableAttributeProvider> importRealmProvider(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @RequestParam(required = false, defaultValue = "false") boolean reset,
            @RequestPart(name = "yaml", required = false) @Valid String yaml,
            @RequestPart(name = "file", required = false) @Valid MultipartFile file)
            throws NoSuchRealmException, RegistrationException {
        logger.debug("import ap(s) to realm {}", StringUtils.trimAllWhitespace(realm));

        if (!StringUtils.hasText(yaml) && (file == null || file.isEmpty())) {
            throw new IllegalArgumentException("empty file or yaml");
        }

        try {
            // read string, fallback to yaml
            if (!StringUtils.hasText(yaml)) {
                if (file.getContentType() == null) {
                    throw new IllegalArgumentException("invalid file");
                }

                if (!SystemKeys.MEDIA_TYPE_YAML.toString().equals(file.getContentType())
                        && !SystemKeys.MEDIA_TYPE_YML.toString().equals(file.getContentType())
                        && !SystemKeys.MEDIA_TYPE_XYAML.toString().equals(file.getContentType())) {
                    throw new IllegalArgumentException("invalid file");
                }

                // read whole file as string
                yaml = new String(file.getBytes(), StandardCharsets.UTF_8);
            }

            List<ConfigurableAttributeProvider> providers = new ArrayList<>();
            List<ConfigurableAttributeProvider> regs = new ArrayList<>();

            // read as raw yaml to check if collection
            Yaml reader = new Yaml();
            Map<String, Object> obj = reader.load(yaml);
            boolean multiple = obj.containsKey(LIST_KEY);

            if (multiple) {
                Map<String, List<ConfigurableAttributeProvider>> list = yamlObjectMapper.readValue(yaml, typeRef);
                for (ConfigurableAttributeProvider reg : list.get(LIST_KEY)) {
                    regs.add(reg);
                }
            } else {
                // try single element
                ConfigurableAttributeProvider reg = yamlObjectMapper.readValue(yaml,
                        ConfigurableAttributeProvider.class);
                regs.add(reg);
            }

            // register all
            for (ConfigurableAttributeProvider reg : regs) {
                // align config
                reg.setRealm(realm);
                if (reset) {
                    // reset id
                    reg.setProvider(null);
                }

                if (logger.isTraceEnabled()) {
                    logger.trace("provider bean: " + String.valueOf(reg));
                }

                // register
                ConfigurableAttributeProvider provider = providerManager.addAttributeProvider(realm, reg);

                // fetch also configuration schema
                JsonSchema schema = providerManager.getConfigurationSchema(realm, provider.getType(),
                        provider.getAuthority());
                provider.setSchema(schema);
                providers.add(provider);
            }

            return providers;
        } catch (Exception e) {
            logger.error("error importing providers: " + e.getMessage());
            if (logger.isTraceEnabled()) {
                e.printStackTrace();
            }

            if (e instanceof ClassCastException) {
                throw new RegistrationException("invalid content or file");
            }

            throw new RegistrationException(e.getMessage());
        }

    }

    @GetMapping("/aps/{realm}/{providerId}/export")
    public void exportRealmProvider(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId,
            HttpServletResponse res)
            throws NoSuchProviderException, NoSuchRealmException, SystemException, IOException {
        logger.debug("export ap {} for realm {}",
                StringUtils.trimAllWhitespace(providerId), StringUtils.trimAllWhitespace(realm));

        ConfigurableAttributeProvider provider = providerManager.getAttributeProvider(realm, providerId);
        String s = yamlObjectMapper.writeValueAsString(provider);

        // write as file
        res.setContentType("text/yaml");
        res.setHeader("Content-Disposition", "attachment;filename=ap-" + provider.getName() + ".yaml");
        ServletOutputStream out = res.getOutputStream();
        out.write(s.getBytes(StandardCharsets.UTF_8));
        out.flush();
        out.close();

    }

}
