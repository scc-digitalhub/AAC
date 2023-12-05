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
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchRoleException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.model.RealmRole;
import it.smartcommunitylab.aac.roles.BaseRealmRolesController;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
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

@RestController
@Hidden
@RequestMapping("/console/dev")
public class DevRealmRolesController extends BaseRealmRolesController {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final TypeReference<Map<String, List<RealmRole>>> typeRef =
        new TypeReference<Map<String, List<RealmRole>>>() {};
    private final String LIST_KEY = "roles";

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
        @RequestPart(name = "yaml", required = false) @Valid String yaml,
        @RequestPart(name = "file", required = false) @Valid MultipartFile file
    ) throws NoSuchRealmException, RegistrationException {
        logger.debug("import role(s) to realm {}", StringUtils.trimAllWhitespace(realm));

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

            List<RealmRole> roles = new ArrayList<>();
            List<RealmRole> regs = new ArrayList<>();

            // read as raw yaml to check if collection
            Yaml reader = new Yaml();
            Map<String, Object> obj = reader.load(yaml);
            boolean multiple = obj.containsKey(LIST_KEY);

            if (multiple) {
                Map<String, List<RealmRole>> list = yamlObjectMapper.readValue(yaml, typeRef);
                for (RealmRole reg : list.get(LIST_KEY)) {
                    regs.add(reg);
                }
            } else {
                // try single element
                RealmRole reg = yamlObjectMapper.readValue(yaml, RealmRole.class);
                regs.add(reg);
            }

            // register all
            for (RealmRole reg : regs) {
                // align config
                reg.setRealm(realm);
                if (reset) {
                    // reset id
                    reg.setRoleId(null);
                }

                if (logger.isTraceEnabled()) {
                    logger.trace("role bean: {}", String.valueOf(reg));
                }

                RealmRole role = roleManager.addRealmRole(realm, reg);
                roles.add(role);
            }

            return roles;
        } catch (RuntimeException | IOException e) {
            logger.error("error importing roles: " + e.getMessage());
            if (logger.isTraceEnabled()) {
                e.printStackTrace();
            }

            if (e instanceof ClassCastException) {
                throw new RegistrationException("invalid content or file");
            }

            throw new RegistrationException(e.getMessage());
        }
    }

    @GetMapping("/roles/{realm}/{roleId}/export")
    public void exportRealmRole(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String roleId,
        HttpServletResponse res
    ) throws NoSuchRealmException, SystemException, IOException, NoSuchRoleException {
        logger.debug(
            "export role {} for realm {}",
            StringUtils.trimAllWhitespace(roleId),
            StringUtils.trimAllWhitespace(realm)
        );

        RealmRole role = roleManager.getRealmRole(realm, roleId, true);
        String s = yamlObjectMapper.writeValueAsString(role);

        // write as file
        res.setContentType(SystemKeys.MEDIA_TYPE_APPLICATION_YAML_VALUE);
        res.setHeader("Content-Disposition", "attachment;filename=role-" + role.getRole() + ".yaml");
        ServletOutputStream out = res.getOutputStream();
        out.write(s.getBytes(StandardCharsets.UTF_8));
        out.flush();
        out.close();
    }
}
