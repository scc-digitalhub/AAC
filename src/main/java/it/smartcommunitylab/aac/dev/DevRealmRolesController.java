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
import org.springframework.web.bind.annotation.PutMapping;
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
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchRoleException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.model.RealmRole;
import it.smartcommunitylab.aac.roles.BaseRealmRolesController;

@RestController
@Hidden
@RequestMapping("/console/dev")
public class DevRealmRolesController extends BaseRealmRolesController {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final TypeReference<Map<String, List<RealmRole>>> typeRef = new TypeReference<Map<String, List<RealmRole>>>() {
    };

    @Autowired
    @Qualifier("yamlObjectMapper")
    private ObjectMapper yamlObjectMapper;

    /*
     * Import/export for console
     */
    @PutMapping("/roles/{realm}")
    public Collection<RealmRole> importRealmRoles(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @RequestParam(required = false, defaultValue = "false") boolean reset,
            @RequestPart("file") @Valid @NotNull @NotBlank MultipartFile file) throws RegistrationException {
        logger.debug("import role(s) to realm {}", StringUtils.trimAllWhitespace(realm));

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
            List<RealmRole> roles = new ArrayList<>();
            boolean multiple = false;

            // read as raw yaml to check if collection
            Yaml yaml = new Yaml();
            Map<String, Object> obj = yaml.load(file.getInputStream());
            multiple = obj.containsKey("roles");

            if (multiple) {
                Map<String, List<RealmRole>> list = yamlObjectMapper.readValue(file.getInputStream(), typeRef);
                for (RealmRole r : list.get("roles")) {
                    r.setRealm(realm);
                    if (reset) {
                        // reset id
                        r.setRoleId(null);
                    }

                    RealmRole role = roleManager.addRealmRole(realm, r);
                    roles.add(role);
                }

            } else {
                // try single element
                RealmRole r = yamlObjectMapper.readValue(file.getInputStream(), RealmRole.class);
                r.setRealm(realm);
                if (reset) {
                    // reset id
                    r.setRoleId(null);
                }

                RealmRole role = roleManager.addRealmRole(realm, r);
                roles.add(role);
            }

            return roles;
        } catch (Exception e) {
            logger.error("error importing roles: " + e.getMessage());

            throw new RegistrationException(e.getMessage());
        }
    }

    @GetMapping("/roles/{realm}/{roleId}/export")
    public void exportRealmRole(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String roleId,
            HttpServletResponse res)
            throws NoSuchRealmException, SystemException, IOException, NoSuchRoleException {
        logger.debug("export role {} for realm {}",
                StringUtils.trimAllWhitespace(roleId), StringUtils.trimAllWhitespace(realm));

        RealmRole role = roleManager.getRealmRole(realm, roleId);
        String s = yamlObjectMapper.writeValueAsString(role);

        // write as file
        res.setContentType("text/yaml");
        res.setHeader("Content-Disposition", "attachment;filename=role-" + role.getRole() + ".yaml");
        ServletOutputStream out = res.getOutputStream();
        out.write(s.getBytes(StandardCharsets.UTF_8));
        out.flush();
        out.close();
    }

}
