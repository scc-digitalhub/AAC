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
import it.smartcommunitylab.aac.common.NoSuchGroupException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.group.BaseGroupController;
import it.smartcommunitylab.aac.model.Group;

@RestController
@Hidden
@RequestMapping("/console/dev")
public class DevGroupsController extends BaseGroupController {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final TypeReference<Map<String, List<Group>>> typeRef = new TypeReference<Map<String, List<Group>>>() {
    };

    @Autowired
    @Qualifier("yamlObjectMapper")
    private ObjectMapper yamlObjectMapper;

    /*
     * Import/export for console
     */
    @PutMapping("/groups/{realm}")
    public Collection<Group> importGroups(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @RequestParam(required = false, defaultValue = "false") boolean reset,
            @RequestPart("file") @Valid @NotNull @NotBlank MultipartFile file) throws RegistrationException {
        logger.debug("import group(s) to realm {}", StringUtils.trimAllWhitespace(realm));

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
            List<Group> groups = new ArrayList<>();
            boolean multiple = false;

            // read as raw yaml to check if collection
            Yaml yaml = new Yaml();
            Map<String, Object> obj = yaml.load(file.getInputStream());
            multiple = obj.containsKey("groups");

            if (multiple) {
                Map<String, List<Group>> list = yamlObjectMapper.readValue(file.getInputStream(), typeRef);
                for (Group g : list.get("groups")) {
                    g.setRealm(realm);
                    if (reset) {
                        // reset id
                        g.setGroupId(null);
                    }

                    Group role = groupManager.addGroup(realm, g);
                    groups.add(role);
                }

            } else {
                // try single element
                Group g = yamlObjectMapper.readValue(file.getInputStream(), Group.class);
                g.setRealm(realm);
                if (reset) {
                    // reset id
                    g.setGroupId(null);
                }

                Group group = groupManager.addGroup(realm, g);
                groups.add(group);
            }

            return groups;
        } catch (Exception e) {
            logger.error("error importing groups: " + e.getMessage());

            throw new RegistrationException(e.getMessage());
        }
    }

    @GetMapping("/groups/{realm}/{groupId}/export")
    public void exportGroup(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String groupId,
            HttpServletResponse res)
            throws NoSuchRealmException, SystemException, IOException, NoSuchGroupException {
        logger.debug("export group {} for realm {}",
                StringUtils.trimAllWhitespace(groupId), StringUtils.trimAllWhitespace(realm));

        Group group = groupManager.getGroup(realm, groupId, true);
        String s = yamlObjectMapper.writeValueAsString(group);

        // write as file
        res.setContentType("text/yaml");
        res.setHeader("Content-Disposition", "attachment;filename=group-" + group.getGroup() + ".yaml");
        ServletOutputStream out = res.getOutputStream();
        out.write(s.getBytes(StandardCharsets.UTF_8));
        out.flush();
        out.close();
    }

}
