package it.smartcommunitylab.aac.dev;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.BaseAttributeSetsController;
import it.smartcommunitylab.aac.common.NoSuchAttributeSetException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.core.model.AttributeSet;
import springfox.documentation.annotations.ApiIgnore;

@RestController
@ApiIgnore
@RequestMapping("/console/dev")
public class DevAttributesController extends BaseAttributeSetsController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    /*
     * Attributes sets
     */

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
