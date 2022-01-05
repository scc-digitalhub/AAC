package it.smartcommunitylab.aac.dev;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.smartcommunitylab.aac.Config;
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

    @Autowired
    @Qualifier("yamlObjectMapper")
    protected ObjectMapper yamlObjectMapper;

    /*
     * Import/export for console
     */
    @PutMapping("/attributeset/{realm}")
    public AttributeSet importAttributeSet(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @RequestParam("file") @Valid @NotNull @NotBlank MultipartFile file) throws RegistrationException {
        logger.debug("import attribute set to realm {}", StringUtils.trimAllWhitespace(realm));

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
            DefaultAttributesSet s = yamlObjectMapper.readValue(file.getInputStream(),
                    DefaultAttributesSet.class);

            if (logger.isTraceEnabled()) {
                logger.trace("attribute set bean: " + String.valueOf(s));
            }

            return attributeManager.addAttributeSet(realm, s);

        } catch (Exception e) {
            logger.error("import attribute set error: " + e.getMessage());
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

//        String s = yaml.dump(service);
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
