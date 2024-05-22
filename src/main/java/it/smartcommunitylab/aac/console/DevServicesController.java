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
import io.swagger.v3.oas.annotations.Hidden;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchServiceException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.dto.FunctionValidationBean;
import it.smartcommunitylab.aac.model.RealmRole;
import it.smartcommunitylab.aac.services.BaseServicesController;
import it.smartcommunitylab.aac.services.Service;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
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
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
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
public class DevServicesController extends BaseServicesController {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final TypeReference<Map<String, List<Service>>> typeRef =
        new TypeReference<Map<String, List<Service>>>() {};
    private final String LIST_KEY = "services";

    @Autowired
    private DevManager devManager;

    @Autowired
    @Qualifier("yamlObjectMapper")
    private ObjectMapper yamlObjectMapper;

    /*
     * Import/export for console
     */
    @PutMapping("/services/{realm}")
    public Collection<Service> importRealmService(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @RequestParam(required = false, defaultValue = "false") boolean reset,
        @RequestPart(name = "yaml", required = false) @Valid String yaml,
        @RequestPart(name = "file", required = false) @Valid MultipartFile file
    ) throws NoSuchRealmException, RegistrationException {
        logger.debug("import service(s) to realm {}", StringUtils.trimAllWhitespace(realm));

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

            List<Service> services = new ArrayList<>();
            List<Service> regs = new ArrayList<>();

            // read as raw yaml to check if collection
            Yaml reader = new Yaml();
            Map<String, Object> obj = reader.load(yaml);
            boolean multiple = obj.containsKey(LIST_KEY);

            if (multiple) {
                Map<String, List<Service>> list = yamlObjectMapper.readValue(yaml, typeRef);
                for (Service reg : list.get(LIST_KEY)) {
                    regs.add(reg);
                }
            } else {
                // try single element
                Service reg = yamlObjectMapper.readValue(yaml, Service.class);
                regs.add(reg);
            }

            // register all
            for (Service reg : regs) {
                // align config
                reg.setRealm(realm);
                if (reset) {
                    // reset id
                    reg.setServiceId(null);
                }

                if (logger.isTraceEnabled()) {
                    logger.trace("service bean: {}", String.valueOf(reg));
                }

                Service service = serviceManager.addService(realm, reg);
                services.add(service);
            }

            return services;
        } catch (RuntimeException | IOException e) {
            logger.error("import service(s) error: " + e.getMessage());
            if (logger.isTraceEnabled()) {
                e.printStackTrace();
            }

            if (e instanceof ClassCastException) {
                throw new RegistrationException("invalid content or file");
            }

            throw new RegistrationException(e.getMessage());
        }
    }

    @GetMapping("/services/{realm}/{serviceId}/export")
    public void exportRealmService(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String serviceId,
        HttpServletResponse res
    ) throws NoSuchRealmException, NoSuchServiceException, IOException {
        logger.debug(
            "export service {} for realm {}",
            StringUtils.trimAllWhitespace(serviceId),
            StringUtils.trimAllWhitespace(realm)
        );

        Service service = serviceManager.getService(realm, serviceId);
        String s = yamlObjectMapper.writeValueAsString(service);

        // write as file
        res.setContentType(SystemKeys.MEDIA_TYPE_APPLICATION_YAML_VALUE);
        res.setHeader("Content-Disposition", "attachment;filename=service-" + service.getName() + ".yaml");
        ServletOutputStream out = res.getOutputStream();
        out.write(s.getBytes(StandardCharsets.UTF_8));
        out.flush();
        out.close();
    }

    /*
     * Namespace
     */
    @GetMapping("/services/{realm}/nsexists")
    public Boolean checkRealmServiceNamespace(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @RequestParam @Valid @NotBlank @Pattern(regexp = SystemKeys.NAMESPACE_PATTERN) String ns
    ) throws NoSuchRealmException {
        return serviceManager.checkServiceNamespace(realm, ns);
    }

    /*
     * Claims
     */
    @PostMapping("/services/{realm}/{serviceId}/claims/validate")
    public FunctionValidationBean validateRealmServiceClaim(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String serviceId,
        @RequestBody @Valid @NotNull FunctionValidationBean function
    ) throws NoSuchServiceException, NoSuchRealmException, SystemException, InvalidDefinitionException {
        try {
            // TODO expose context personalization in UI
            function = devManager.testServiceClaimMapping(realm, serviceId, function);
        } catch (InvalidDefinitionException | RuntimeException e) {
            // translate error
            function.addError(e.getMessage());
        }

        return function;
    }
}
