package it.smartcommunitylab.aac.dev;

import java.io.IOException;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.provider.approval.Approval;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchRoleException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.model.RealmRole;
import it.smartcommunitylab.aac.roles.RealmRoleManager;
import it.smartcommunitylab.aac.roles.SpaceRoleManager;
import springfox.documentation.annotations.ApiIgnore;

@RestController
@ApiIgnore
@RequestMapping("/console/dev")
public class DevRoleController {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final TypeReference<Map<String, List<RealmRole>>> typeRef = new TypeReference<Map<String, List<RealmRole>>>() {
    };

    @Autowired
    private RealmRoleManager roleManager;

    @Autowired
    private SpaceRoleManager spaceRoleManager;

    @Autowired
    @Qualifier("yamlObjectMapper")
    private ObjectMapper yamlObjectMapper;

    @GetMapping("/realms/{realm}/roles")
    public ResponseEntity<Collection<RealmRole>> getRealmRoles(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm)
            throws NoSuchRealmException {
        return ResponseEntity.ok(roleManager.getRealmRoles(realm));
    }

    @PostMapping("/realms/{realm}/roles")
    public ResponseEntity<RealmRole> createRealmRole(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @RequestBody @Valid RealmRole reg)
            throws NoSuchRealmException, NoSuchRoleException {
        return ResponseEntity.ok(roleManager.addRealmRole(realm, reg));
    }

    @GetMapping("/realms/{realm}/roles/{roleId}")
    public ResponseEntity<RealmRole> getRealmRole(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String roleId)
            throws NoSuchRealmException, NoSuchRoleException {
        return ResponseEntity.ok(roleManager.getRealmRole(realm, roleId));
    }

    @PutMapping("/realms/{realm}/roles/{roleId}")
    public ResponseEntity<RealmRole> getRealmRole(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String roleId,
            @RequestBody @Valid RealmRole reg)
            throws NoSuchRealmException, NoSuchRoleException {
        return ResponseEntity.ok(roleManager.updateRealmRole(realm, roleId, reg));
    }

    @DeleteMapping("/realms/{realm}/roles/{roleId}")
    public ResponseEntity<Void> removeRealmRole(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String roleId)
            throws NoSuchRealmException, NoSuchRoleException {
        roleManager.deleteRealmRole(realm, roleId);
        return ResponseEntity.ok(null);
    }

    @PutMapping("/realms/{realm}/roles")
    public ResponseEntity<Collection<RealmRole>> importRealmRoles(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @RequestParam(required = false, defaultValue = "false") boolean reset,
            @RequestPart("file") @Valid @NotNull @NotBlank MultipartFile file) throws Exception {
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

            return ResponseEntity.ok(roles);
        } catch (Exception e) {
            if (logger.isTraceEnabled()) {
                e.printStackTrace();
            }
            throw new RegistrationException(e.getMessage());
        }
    }

    @GetMapping("/realms/{realm}/roles/{roleId}/yaml")
    public void exportRealmRole(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String roleId,
            HttpServletResponse res)
            throws NoSuchRealmException, SystemException, IOException, NoSuchRoleException {

        RealmRole role = roleManager.getRealmRole(realm, roleId);
        String s = yamlObjectMapper.writeValueAsString(role);

        // write as file
        res.setContentType("text/yaml");
        res.setHeader("Content-Disposition", "attachment;filename=role-" + role.getRole() + ".yaml");
        ServletOutputStream out = res.getOutputStream();
        out.print(s);
        out.flush();
        out.close();
    }

    
    @GetMapping("/realms/{realm}/roles/{roleId}/approvals")
    public ResponseEntity<Collection<Approval>> getRealmRoleApprovals(
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String roleId)
            throws NoSuchRealmException, NoSuchRoleException {
        Collection<Approval> approvals = roleManager.getRealmRoleApprovals(realm, roleId);
        return ResponseEntity.ok(approvals);
    }

}
