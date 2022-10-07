package it.smartcommunitylab.aac.console;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
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
import org.thymeleaf.context.WebContext;
import org.yaml.snakeyaml.Yaml;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Hidden;
import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchAuthorityException;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchTemplateException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.templates.BaseTemplatesController;
import it.smartcommunitylab.aac.templates.model.TemplateModel;

@RestController
@Hidden
@RequestMapping("/console/dev")
public class DevTemplatesController extends BaseTemplatesController {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final TypeReference<Map<String, List<TemplateModel>>> typeRef = new TypeReference<Map<String, List<TemplateModel>>>() {
    };
    private final String LIST_KEY = "templates";

    @Autowired
    @Qualifier("yamlObjectMapper")
    private ObjectMapper yamlObjectMapper;

    @Autowired
    private ServletContext servletContext;

    @Autowired
    private DevManager devManager;

    /*
     * Search
     */
    @GetMapping("/templates/{realm}/models/search")
    public Page<TemplateModel> searchTemplateModels(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @RequestParam(required = false) String q, Pageable pageRequest)
            throws NoSuchRealmException {
        return templatesManager.searchTemplateModels(realm, q, pageRequest);
    }

    /*
     * Import/export for console
     */
    @PutMapping("/templates/{realm}/models")
    public Collection<TemplateModel> importTemplateModels(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @RequestParam(required = false, defaultValue = "false") boolean reset,
            @RequestPart(name = "yaml", required = false) @Valid String yaml,
            @RequestPart(name = "file", required = false) @Valid MultipartFile file)
            throws NoSuchRealmException, RegistrationException, NoSuchTemplateException, NoSuchProviderException,
            NoSuchAuthorityException {
        logger.debug("import template(s) to realm {}", StringUtils.trimAllWhitespace(realm));

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

            List<TemplateModel> templates = new ArrayList<>();
            List<TemplateModel> regs = new ArrayList<>();

            // read as raw yaml to check if collection
            Yaml reader = new Yaml();
            Map<String, Object> obj = reader.load(yaml);
            boolean multiple = obj.containsKey(LIST_KEY);

            if (multiple) {
                Map<String, List<TemplateModel>> list = yamlObjectMapper.readValue(yaml, typeRef);
                for (TemplateModel reg : list.get(LIST_KEY)) {
                    regs.add(reg);
                }
            } else {
                // try single element
                TemplateModel reg = yamlObjectMapper.readValue(yaml, TemplateModel.class);
                regs.add(reg);
            }

            // register all
            for (TemplateModel reg : regs) {
                // align config
                reg.setRealm(realm);
                if (reset) {
                    // reset id
                    reg.setId(null);
                }

                if (logger.isTraceEnabled()) {
                    logger.trace("template bean: {}", String.valueOf(reg));
                }
                // register
                TemplateModel template = templatesManager.addTemplateModel(realm, reg);
                templates.add(template);
            }

            return templates;
        } catch (RuntimeException | IOException e) {
            logger.error("error importing templates: " + e.getMessage());
            if (logger.isTraceEnabled()) {
                e.printStackTrace();
            }

            if (e instanceof ClassCastException) {
                throw new RegistrationException("invalid content or file");
            }

            throw new RegistrationException(e.getMessage());
        }

    }

    @GetMapping("/templates/{realm}/models/{id}/export")
    public void exportTemplateModel(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String id,
            HttpServletResponse res)
            throws NoSuchTemplateException, NoSuchRealmException, SystemException, IOException,
            NoSuchAuthorityException {
        logger.debug("export template {} for realm {}",
                StringUtils.trimAllWhitespace(id), StringUtils.trimAllWhitespace(realm));

        TemplateModel template = templatesManager.getTemplateModel(realm, id);
        String s = yamlObjectMapper.writeValueAsString(template);

        // write as file
        res.setContentType("text/yaml");
        res.setHeader("Content-Disposition", "attachment;filename=template-" + template.buildKey() + ".yaml");
        ServletOutputStream out = res.getOutputStream();
        out.write(s.getBytes(StandardCharsets.UTF_8));
        out.flush();
        out.close();

    }

    @PostMapping("/templates/{realm}/models/{id}/preview")
    @PreAuthorize("hasAuthority('" + Config.R_ADMIN + "') or hasAuthority(#realm+':ROLE_ADMIN')")
    public void previewRealm(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String id,
            @RequestBody @Valid @NotNull TemplateModel reg,
            HttpServletRequest req, HttpServletResponse res)
            throws Exception {

        // load template
        TemplateModel t = templatesManager.getTemplateModel(realm, id);
        String authority = t.getAuthority();
        String template = t.getTemplate();

        // build web context and render view
        WebContext ctx = new WebContext(req, res, servletContext, req.getLocale());
        String s = devManager.previewRealmTemplate(realm, authority, template, id, reg, ctx);

        // write as file
        res.setContentType("text/html");
        ServletOutputStream out = res.getOutputStream();
        out.write(s.getBytes(StandardCharsets.UTF_8));
        out.flush();
        out.close();

    }
}
