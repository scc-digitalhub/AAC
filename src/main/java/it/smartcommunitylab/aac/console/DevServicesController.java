package it.smartcommunitylab.aac.console;

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

import io.swagger.v3.oas.annotations.Hidden;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchServiceException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.dto.FunctionValidationBean;
import it.smartcommunitylab.aac.services.controller.BaseServicesController;
import it.smartcommunitylab.aac.services.model.ApiService;

@RestController
@Hidden
@RequestMapping("/console/dev")
public class DevServicesController extends BaseServicesController {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final TypeReference<Map<String, List<ApiService>>> typeRef = new TypeReference<Map<String, List<ApiService>>>() {
    };
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
    public Collection<ApiService> importRealmService(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @RequestParam(required = false, defaultValue = "false") boolean reset,
            @RequestPart(name = "yaml", required = false) @Valid String yaml,
            @RequestPart(name = "file", required = false) @Valid MultipartFile file)
            throws NoSuchRealmException, RegistrationException {
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

                if (!SystemKeys.MEDIA_TYPE_YAML.toString().equals(file.getContentType())
                        && !SystemKeys.MEDIA_TYPE_YML.toString().equals(file.getContentType())
                        && !SystemKeys.MEDIA_TYPE_XYAML.toString().equals(file.getContentType())) {
                    throw new IllegalArgumentException("invalid file");
                }

                // read whole file as string
                yaml = new String(file.getBytes(), StandardCharsets.UTF_8);
            }

            List<ApiService> services = new ArrayList<>();
            List<ApiService> regs = new ArrayList<>();

            // read as raw yaml to check if collection
            Yaml reader = new Yaml();
            Map<String, Object> obj = reader.load(yaml);
            boolean multiple = obj.containsKey(LIST_KEY);

            if (multiple) {
                Map<String, List<ApiService>> list = yamlObjectMapper.readValue(yaml, typeRef);
                for (ApiService reg : list.get(LIST_KEY)) {
                    regs.add(reg);
                }
            } else {
                // try single element
                ApiService reg = yamlObjectMapper.readValue(file.getInputStream(), ApiService.class);
                regs.add(reg);
            }

            // register all
            for (ApiService reg : regs) {
                // align config
                reg.setRealm(realm);
                if (reset) {
                    // reset id
                    reg.setServiceId(null);
                }

                if (logger.isTraceEnabled()) {
                    logger.trace("service bean: {}", String.valueOf(reg));
                }

                ApiService service = serviceManager.addService(realm, reg);
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
            HttpServletResponse res)
            throws NoSuchRealmException, NoSuchServiceException, IOException {
        logger.debug("export service {} for realm {}",
                StringUtils.trimAllWhitespace(serviceId), StringUtils.trimAllWhitespace(realm));

        ApiService service = serviceManager.getService(realm, serviceId);
        String s = yamlObjectMapper.writeValueAsString(service);

        // write as file
        res.setContentType("text/yaml");
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
            @RequestParam @Valid @NotBlank @Pattern(regexp = SystemKeys.NAMESPACE_PATTERN) String ns)
            throws NoSuchRealmException {
        return serviceManager.checkServiceNamespace(realm, ns);
    }

    /*
     * Resource URI
     */
    @GetMapping("/services/{realm}/nsexists")
    public Boolean checkRealmServiceResource(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @RequestParam @Valid @NotBlank @Pattern(regexp = SystemKeys.RESOURCE_PATTERN) String resource)
            throws NoSuchRealmException {
        return serviceManager.checkServiceResource(realm, resource);
    }

    /*
     * Claims
     */
    @PostMapping("/services/{realm}/{serviceId}/claims/validate")
    public FunctionValidationBean validateRealmServiceClaim(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String serviceId,
            @RequestBody @Valid @NotNull FunctionValidationBean function)
            throws NoSuchServiceException, NoSuchRealmException, SystemException, InvalidDefinitionException {

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
