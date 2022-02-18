package it.smartcommunitylab.aac.dev;

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
import it.smartcommunitylab.aac.controller.BaseServicesController;
import it.smartcommunitylab.aac.dto.FunctionValidationBean;
import it.smartcommunitylab.aac.services.Service;

@RestController
@Hidden
@RequestMapping("/console/dev")
public class DevServicesController extends BaseServicesController {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final TypeReference<Map<String, List<Service>>> typeRef = new TypeReference<Map<String, List<Service>>>() {
    };

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
            @RequestPart("file") @Valid @NotNull @NotBlank MultipartFile file) throws RegistrationException {
        logger.debug("import service(s) to realm {}", StringUtils.trimAllWhitespace(realm));

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
            List<Service> services = new ArrayList<>();
            boolean multiple = false;

            // read as raw yaml to check if collection
            Yaml yaml = new Yaml();
            Map<String, Object> obj = yaml.load(file.getInputStream());
            multiple = obj.containsKey("services");

            if (multiple) {
                Map<String, List<Service>> list = yamlObjectMapper.readValue(file.getInputStream(), typeRef);
                for (Service s : list.get("services")) {
                    s.setRealm(realm);
                    if (reset) {
                        // reset id
                        s.setServiceId(null);
                    }

                    Service service = serviceManager.addService(realm, s);
                    services.add(service);
                }

            } else {
                // try single element
                Service s = yamlObjectMapper.readValue(file.getInputStream(), Service.class);
                s.setRealm(realm);
                if (reset) {
                    // reset id
                    s.setServiceId(null);
                }

                Service service = serviceManager.addService(realm, s);
                services.add(service);
            }

            return services;
        } catch (Exception e) {
            logger.error("import service(s) error: " + e.getMessage());
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

        Service service = serviceManager.getService(realm, serviceId);
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
