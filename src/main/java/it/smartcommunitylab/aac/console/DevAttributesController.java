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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.yaml.snakeyaml.Yaml;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Hidden;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.BaseAttributeSetsController;
import it.smartcommunitylab.aac.attributes.DefaultAttributesSet;
import it.smartcommunitylab.aac.common.NoSuchAttributeSetException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.model.AttributeSet;

@RestController
@Hidden
@RequestMapping("/console/dev")
public class DevAttributesController extends BaseAttributeSetsController {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final TypeReference<Map<String, List<DefaultAttributesSet>>> typeRef = new TypeReference<Map<String, List<DefaultAttributesSet>>>() {
    };
    private final String LIST_KEY = "sets";

    @Autowired
    @Qualifier("yamlObjectMapper")
    protected ObjectMapper yamlObjectMapper;

    /*
     * Import/export for console
     */
    @PutMapping("/attributeset/{realm}")
    public Collection<AttributeSet> importAttributeSet(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @RequestPart(name = "yaml", required = false) @Valid String yaml,
            @RequestPart(name = "file", required = false) @Valid MultipartFile file)
            throws NoSuchRealmException, RegistrationException {
        logger.debug("import attribute set to realm {}", StringUtils.trimAllWhitespace(realm));

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

            List<AttributeSet> attributeSets = new ArrayList<>();
            List<DefaultAttributesSet> regs = new ArrayList<>();

            // read as raw yaml to check if collection
            Yaml reader = new Yaml();
            Map<String, Object> obj = reader.load(yaml);
            boolean multiple = obj.containsKey(LIST_KEY);

            if (multiple) {
                Map<String, List<DefaultAttributesSet>> list = yamlObjectMapper.readValue(yaml, typeRef);
                for (DefaultAttributesSet reg : list.get(LIST_KEY)) {
                    regs.add(reg);
                }
            } else {
                // try single element
                DefaultAttributesSet reg = yamlObjectMapper.readValue(yaml,
                        DefaultAttributesSet.class);
                regs.add(reg);
            }

            // register all
            for (DefaultAttributesSet reg : regs) {
                // align config
                reg.setRealm(realm);

                if (logger.isTraceEnabled()) {
                    logger.trace("attribute set bean: {}", String.valueOf(reg));
                }

                // register
                AttributeSet set = attributeManager.addAttributeSet(realm, reg);
                attributeSets.add(set);
            }

            return attributeSets;
        } catch (RuntimeException | IOException e) {
            logger.error("import attribute set error: " + e.getMessage());
            if (logger.isTraceEnabled()) {
                e.printStackTrace();
            }

            if (e instanceof ClassCastException) {
                throw new RegistrationException("invalid content or file");
            }

            throw new RegistrationException(e.getMessage());
        }

    }

    @GetMapping("/attributeset/{realm}/{setId}/export")
    public void exportRealmAttributeSet(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String setId,
            HttpServletResponse res)
            throws NoSuchRealmException, NoSuchAttributeSetException, IOException {
        logger.debug("export attribute set {} for realm {}",
                StringUtils.trimAllWhitespace(setId), StringUtils.trimAllWhitespace(realm));

        AttributeSet set = attributeManager.getAttributeSet(realm, setId);
        String s = yamlObjectMapper.writeValueAsString(set);

        // write as file
        res.setContentType("text/yaml");
        res.setHeader("Content-Disposition", "attachment;filename=attributeset-" + set.getIdentifier() + ".yaml");
        ServletOutputStream out = res.getOutputStream();
        out.write(s.getBytes(StandardCharsets.UTF_8));
        out.flush();
        out.close();
    }
}
