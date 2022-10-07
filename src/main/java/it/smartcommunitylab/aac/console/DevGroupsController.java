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
import it.smartcommunitylab.aac.groups.BaseGroupController;
import it.smartcommunitylab.aac.model.Group;

@RestController
@Hidden
@RequestMapping("/console/dev")
public class DevGroupsController extends BaseGroupController {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final TypeReference<Map<String, List<Group>>> typeRef = new TypeReference<Map<String, List<Group>>>() {
    };
    private final String LIST_KEY = "groups";

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
            @RequestPart(name = "yaml", required = false) @Valid String yaml,
            @RequestPart(name = "file", required = false) @Valid MultipartFile file)
            throws NoSuchRealmException, RegistrationException {
        logger.debug("import group(s) to realm {}", StringUtils.trimAllWhitespace(realm));

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

            List<Group> groups = new ArrayList<>();
            List<Group> regs = new ArrayList<>();

            // read as raw yaml to check if collection
            Yaml reader = new Yaml();
            Map<String, Object> obj = reader.load(yaml);
            boolean multiple = obj.containsKey(LIST_KEY);

            if (multiple) {
                Map<String, List<Group>> list = yamlObjectMapper.readValue(yaml, typeRef);
                for (Group reg : list.get(LIST_KEY)) {
                    regs.add(reg);
                }
            } else {
                // try single element
                Group reg = yamlObjectMapper.readValue(yaml, Group.class);
                regs.add(reg);
            }

            // register all
            for (Group reg : regs) {
                // align config
                reg.setRealm(realm);
                if (reset) {
                    // reset id
                    reg.setGroupId(null);
                }

                if (logger.isTraceEnabled()) {
                    logger.trace("group bean: {}", String.valueOf(reg));
                }

                Group group = groupManager.addGroup(realm, reg);
                groups.add(group);
            }

            return groups;
        } catch (RuntimeException | IOException e) {
            logger.error("error importing groups: " + e.getMessage());
            if (logger.isTraceEnabled()) {
                e.printStackTrace();
            }

            if (e instanceof ClassCastException) {
                throw new RegistrationException("invalid content or file");
            }

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
