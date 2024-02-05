/*
 * Copyright 2023 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.console;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import io.swagger.v3.oas.annotations.Hidden;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.clients.ClientManager;
import it.smartcommunitylab.aac.common.NoSuchAuthorityException;
import it.smartcommunitylab.aac.common.NoSuchClientException;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.identity.controller.BaseIdentityProviderController;
import it.smartcommunitylab.aac.identity.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.model.ClientApp;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.MethodArgumentNotValidException;
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

@RestController
@Validated
@Hidden
@RequestMapping("/console/dev")
public class DevIdentityProviderController extends BaseIdentityProviderController {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final TypeReference<Map<String, List<ConfigurableIdentityProvider>>> typeRef =
        new TypeReference<Map<String, List<ConfigurableIdentityProvider>>>() {};
    private final String LIST_KEY = "providers";

    @Autowired
    private ClientManager clientManager;

    @Autowired
    @Qualifier("yamlObjectMapper")
    private ObjectMapper yamlObjectMapper;

    /*
     * Providers
     */
    @Override
    @GetMapping("/idps/{realm}/{providerId}")
    public ConfigurableIdentityProvider getIdp(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId
    ) throws NoSuchProviderException, NoSuchRealmException, NoSuchAuthorityException {
        ConfigurableIdentityProvider provider = super.getIdp(realm, providerId);

        // fetch also configuration schema
        JsonSchema schema = providerManager.getConfigurationSchema(realm, provider.getAuthority());
        provider.setSchema(schema);

        return provider;
    }

    @Override
    @PostMapping("/idps/{realm}")
    public ConfigurableIdentityProvider addIdp(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @RequestBody @Valid @NotNull ConfigurableIdentityProvider registration
    )
        throws NoSuchRealmException, NoSuchProviderException, RegistrationException, SystemException, NoSuchAuthorityException, MethodArgumentNotValidException {
        ConfigurableIdentityProvider provider = super.addIdp(realm, registration);

        // fetch also configuration schema
        JsonSchema schema = providerManager.getConfigurationSchema(realm, provider.getAuthority());
        provider.setSchema(schema);

        return provider;
    }

    @Override
    @PutMapping("/idps/{realm}/{providerId}")
    public ConfigurableIdentityProvider updateIdp(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId,
        @RequestBody @Valid @NotNull ConfigurableIdentityProvider registration,
        @RequestParam(required = false, defaultValue = "false") Optional<Boolean> force
    )
        throws NoSuchRealmException, NoSuchProviderException, NoSuchAuthorityException, RegistrationException, MethodArgumentNotValidException {
        ConfigurableIdentityProvider provider = super.updateIdp(realm, providerId, registration, Optional.of(false));

        // fetch also configuration schema
        JsonSchema schema = providerManager.getConfigurationSchema(realm, provider.getAuthority());
        provider.setSchema(schema);

        return provider;
    }

    /*
     * Import/export for console
     */
    @PutMapping("/idps/{realm}")
    public Collection<ConfigurableIdentityProvider> importRealmProvider(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @RequestParam(required = false, defaultValue = "false") boolean reset,
        @RequestPart(name = "yaml", required = false) @Valid String yaml,
        @RequestPart(name = "file", required = false) @Valid MultipartFile file
    )
        throws NoSuchRealmException, RegistrationException, NoSuchProviderException, NoSuchAuthorityException, MethodArgumentNotValidException {
        logger.debug("import idp(s) to realm {}", StringUtils.trimAllWhitespace(realm));

        if (!StringUtils.hasText(yaml) && (file == null || file.isEmpty())) {
            throw new IllegalArgumentException("empty file or yaml");
        }

        try {
            // read string, fallback to yaml
            if (!StringUtils.hasText(yaml)) {
                if (file.getContentType() == null) {
                    throw new IllegalArgumentException("invalid file");
                }

                if (
                    !SystemKeys.MEDIA_TYPE_APPLICATION_YAML.toString().equals(file.getContentType()) &&
                    !SystemKeys.MEDIA_TYPE_TEXT_YAML.toString().equals(file.getContentType()) &&
                    !SystemKeys.MEDIA_TYPE_APPLICATION_XYAML.toString().equals(file.getContentType())
                ) {
                    throw new IllegalArgumentException("invalid file");
                }

                // read whole file as string
                yaml = new String(file.getBytes(), StandardCharsets.UTF_8);
            }

            List<ConfigurableIdentityProvider> providers = new ArrayList<>();
            List<ConfigurableIdentityProvider> regs = new ArrayList<>();

            // read as raw yaml to check if collection
            Yaml reader = new Yaml();
            Map<String, Object> obj = reader.load(yaml);
            boolean multiple = obj.containsKey(LIST_KEY);

            if (multiple) {
                Map<String, List<ConfigurableIdentityProvider>> list = yamlObjectMapper.readValue(yaml, typeRef);
                for (ConfigurableIdentityProvider reg : list.get(LIST_KEY)) {
                    regs.add(reg);
                }
            } else {
                // try single element
                ConfigurableIdentityProvider reg = yamlObjectMapper.readValue(yaml, ConfigurableIdentityProvider.class);
                regs.add(reg);
            }

            // register all
            for (ConfigurableIdentityProvider reg : regs) {
                // align config
                reg.setRealm(realm);
                if (reset) {
                    // reset id
                    reg.setProvider(null);
                }

                if (logger.isTraceEnabled()) {
                    logger.trace("provider bean: {}", String.valueOf(reg));
                }
                // register
                ConfigurableIdentityProvider provider = providerManager.addProvider(realm, reg);

                // fetch also configuration schema
                JsonSchema schema = providerManager.getConfigurationSchema(realm, provider.getAuthority());
                provider.setSchema(schema);
                providers.add(provider);
            }

            return providers;
        } catch (RuntimeException | IOException e) {
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

    @GetMapping("/idps/{realm}/{providerId}/export")
    public void exportRealmProvider(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId,
        HttpServletResponse res
    ) throws NoSuchProviderException, NoSuchRealmException, SystemException, IOException, NoSuchAuthorityException {
        logger.debug(
            "export idp {} for realm {}",
            StringUtils.trimAllWhitespace(providerId),
            StringUtils.trimAllWhitespace(realm)
        );

        ConfigurableIdentityProvider provider = providerManager.getProvider(realm, providerId);
        String s = yamlObjectMapper.writeValueAsString(provider);

        // write as file
        res.setContentType(SystemKeys.MEDIA_TYPE_APPLICATION_YAML_VALUE);
        res.setHeader("Content-Disposition", "attachment;filename=idp-" + provider.getName() + ".yaml");
        ServletOutputStream out = res.getOutputStream();
        out.write(s.getBytes(StandardCharsets.UTF_8));
        out.flush();
        out.close();
    }

    /*
     * Clients
     */
    @PutMapping("/idps/{realm}/{providerId}/apps/{clientId}")
    public ResponseEntity<ClientApp> updateRealmProviderClientApp(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String providerId,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String clientId,
        @RequestBody @Valid @NotNull ClientApp app
    )
        throws NoSuchRealmException, NoSuchUserException, NoSuchClientException, SystemException, NoSuchProviderException {
        ClientApp clientApp = clientManager.getClientApp(realm, clientId);
        // update providers only for this id
        Set<String> providers = new HashSet<>(Arrays.asList(clientApp.getProviders()));
        boolean enabled = Arrays.stream(app.getProviders()).anyMatch(p -> providerId.equals(p));
        if (enabled) {
            if (!providers.contains(providerId)) {
                providers.add(providerId);
                clientApp.setProviders(providers.toArray(new String[0]));
                clientApp = clientManager.updateClientApp(realm, clientId, clientApp);
            }
        } else {
            if (providers.contains(providerId)) {
                providers.remove(providerId);
                clientApp.setProviders(providers.toArray(new String[0]));
                clientApp = clientManager.updateClientApp(realm, clientId, clientApp);
            }
        }

        return ResponseEntity.ok(clientApp);
    }
}
