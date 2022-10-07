package it.smartcommunitylab.aac.templates;

import java.util.Collection;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import io.swagger.v3.oas.annotations.Operation;
import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchAuthorityException;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchTemplateException;
import it.smartcommunitylab.aac.core.model.Template;
import it.smartcommunitylab.aac.templates.model.TemplateModel;

/*
 * Base controller for realm templates
 */

@PreAuthorize("hasAuthority(this.authority)")
public class BaseTemplatesController implements InitializingBean {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    protected TemplatesManager templatesManager;

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(templatesManager, "templates manager is required");
    }

    @Autowired
    public void setTemplatesManager(TemplatesManager templatesManager) {
        this.templatesManager = templatesManager;
    }

    public String getAuthority() {
        return Config.R_USER;
    }

    /*
     * Realm templates
     */

    @GetMapping("/templates/{realm}/models")
    @Operation(summary = "list templates for realm")
    public Collection<TemplateModel> getTemplateModels(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm)
            throws NoSuchRealmException {
        logger.debug("list templates for realm {}", StringUtils.trimAllWhitespace(realm));

        return templatesManager.listTemplateModels(realm);
    }

    @PostMapping("/templates/{realm}/models")
    @Operation(summary = "add a new template for realm")
    public TemplateModel createTemplateModel(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @RequestBody @Valid @NotNull TemplateModel reg)
            throws NoSuchRealmException, NoSuchTemplateException, NoSuchProviderException, NoSuchAuthorityException {
        logger.debug("add template to realm {}", StringUtils.trimAllWhitespace(realm));

        // use model as is
        if (logger.isTraceEnabled()) {
            logger.trace("template bean: {}", StringUtils.trimAllWhitespace(reg.toString()));
        }

        TemplateModel m = templatesManager.addTemplateModel(realm, reg);
        return m;
    }

    @GetMapping("/templates/{realm}/models/{templateId}")
    @Operation(summary = "fetch a specific template from realm")
    public TemplateModel getTemplateModel(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String templateId)
            throws NoSuchRealmException, NoSuchTemplateException {
        logger.debug("get template {} for realm {}", StringUtils.trimAllWhitespace(templateId),
                StringUtils.trimAllWhitespace(realm));

        TemplateModel r = templatesManager.getTemplateModel(realm, templateId);

        return r;
    }

    @PutMapping("/templates/{realm}/models/{templateId}")
    @Operation(summary = "update a specific template in the realm")
    public TemplateModel updateTemplateModel(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String templateId,
            @RequestBody @Valid @NotNull TemplateModel reg)
            throws NoSuchRealmException, NoSuchTemplateException, NoSuchProviderException, NoSuchAuthorityException {
        logger.debug("update template {} for realm {}", StringUtils.trimAllWhitespace(templateId),
                StringUtils.trimAllWhitespace(realm));

        TemplateModel m = templatesManager.getTemplateModel(realm, templateId);

        // use model as is
        if (logger.isTraceEnabled()) {
            logger.trace("template bean: {}", StringUtils.trimAllWhitespace(reg.toString()));
        }

        m = templatesManager.updateTemplateModel(realm, templateId, reg);
        return m;
    }

    @DeleteMapping("/templates/{realm}/models/{templateId}")
    @Operation(summary = "remove a specific template from realm")
    public void removeTemplateModel(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String templateId)
            throws NoSuchRealmException, NoSuchTemplateException {
        logger.debug("delete template {} for realm {}",
                StringUtils.trimAllWhitespace(templateId), StringUtils.trimAllWhitespace(realm));

        templatesManager.deleteTemplateModel(realm, templateId);
    }

    /*
     * Authority templates
     */
    @GetMapping("/templates/{realm}")
    @Operation(summary = "list templates for realm")
    public Collection<String> getAuthorities(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm)
            throws NoSuchRealmException, NoSuchProviderException, NoSuchAuthorityException {
        logger.debug("list template authorities for realm {}", StringUtils.trimAllWhitespace(realm));

        return templatesManager.getAuthorities(realm);
    }

    @GetMapping("/templates/{realm}/{authority}")
    @Operation(summary = "list templates for realm")
    public Collection<Template> getTemplates(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String authority)
            throws NoSuchRealmException, NoSuchProviderException, NoSuchAuthorityException {
        logger.debug("list templates for realm {} authority {}", StringUtils.trimAllWhitespace(realm),
                StringUtils.trimAllWhitespace(authority));

        return templatesManager.listTemplates(realm, authority);
    }

    @GetMapping("/templates/{realm}/{authority}/{template}")
    @Operation(summary = "get template for realm")
    public Template getTemplate(
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String realm,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String authority,
            @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String template)
            throws NoSuchRealmException, NoSuchProviderException, NoSuchAuthorityException, NoSuchTemplateException {
        logger.debug("get template {} for realm {} authority {}", StringUtils.trimAllWhitespace(template),
                StringUtils.trimAllWhitespace(realm), StringUtils.trimAllWhitespace(authority));

        return templatesManager.getTemplate(realm, authority, template);
    }

}
