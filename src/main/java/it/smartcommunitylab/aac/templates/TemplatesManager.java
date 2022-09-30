package it.smartcommunitylab.aac.templates;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchAuthorityException;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchTemplateException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.model.Template;
import it.smartcommunitylab.aac.core.service.TemplateProviderAuthorityService;
import it.smartcommunitylab.aac.templates.model.TemplateModel;
import it.smartcommunitylab.aac.templates.service.TemplateService;

@Service
@PreAuthorize("hasAuthority('" + Config.R_ADMIN + "')"
        + " or hasAuthority(#realm+':" + Config.R_ADMIN + "')")
public class TemplatesManager {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private TemplateService templateService;

    @Autowired
    private TemplateProviderAuthorityService authorityService;

    /*
     * Templates from authorities
     */

    public Collection<String> getAuthorities(String realm) {
        return authorityService.getAuthoritiesIds();
    }

    public Collection<Template> listTemplates(String realm, String authority)
            throws NoSuchProviderException, NoSuchAuthorityException {

        logger.debug("list templates for realm {} from authority {}", StringUtils.trimAllWhitespace(realm),
                StringUtils.trimAllWhitespace(authority));
        return authorityService.getAuthority(authority).getProviderByRealm(realm).getTemplates();
    }

    public Template getTemplate(String realm, String authority, String template)
            throws NoSuchTemplateException, NoSuchProviderException, NoSuchAuthorityException {

        logger.debug("get template {} for realm {} from authority {}",
                StringUtils.trimAllWhitespace(template), StringUtils.trimAllWhitespace(realm),
                StringUtils.trimAllWhitespace(authority));

        return authorityService.getAuthority(authority).getProviderByRealm(realm).getTemplate(template);
    }

    /*
     * Template models from service
     */
    public TemplateModel findTemplateModel(String realm, String authority, String template, String language) {
        logger.debug("find template model {} for realm {} from authority {} language {}",
                StringUtils.trimAllWhitespace(template), StringUtils.trimAllWhitespace(realm),
                StringUtils.trimAllWhitespace(authority), StringUtils.trimAllWhitespace(language));

        return templateService.findTemplate(authority, realm, template, language);
    }

    public TemplateModel getTemplateModel(String realm, String id) throws NoSuchTemplateException {
        logger.debug("get template model {} for realm {}", StringUtils.trimAllWhitespace(id),
                StringUtils.trimAllWhitespace(realm));

        TemplateModel m = templateService.getTemplate(id);
        if (!realm.equals(m.getRealm())) {
            throw new IllegalArgumentException("realm-mismatch");
        }

        return m;
    }

    public Collection<TemplateModel> listTemplateModels(String realm) {
        logger.debug("list template models for realm {}", StringUtils.trimAllWhitespace(realm));

        return templateService.listTemplatesByRealm(realm);
    }

    public Collection<TemplateModel> listTemplateModels(String realm, String authority) {
        logger.debug("list template models for realm {} for authority {}", StringUtils.trimAllWhitespace(realm),
                StringUtils.trimAllWhitespace(authority));

        return templateService.listTemplates(authority, realm);
    }

    public Collection<TemplateModel> listTemplateModels(String realm, String authority, String template) {
        logger.debug("list template {} models for realm {} for authority {}", StringUtils.trimAllWhitespace(template),
                StringUtils.trimAllWhitespace(realm), StringUtils.trimAllWhitespace(authority));

        return templateService.listTemplates(authority, realm, template);
    }

    public TemplateModel addTemplateModel(String realm, TemplateModel reg)
            throws NoSuchTemplateException, NoSuchProviderException, NoSuchAuthorityException {
        // check model
        String template = reg.getTemplate();
        String authority = reg.getAuthority();

        if (!StringUtils.hasText(authority) || !StringUtils.hasText(template)) {
            throw new RegistrationException();
        }

        logger.debug("add template {} with authority {} for realm {}", StringUtils.trimAllWhitespace(template),
                StringUtils.trimAllWhitespace(authority), StringUtils.trimAllWhitespace(realm));

        Template t = authorityService.getAuthority(authority).getProviderByRealm(realm).getTemplate(template);
        Collection<String> keys = t.keys();

        // check language
        // TODO check if supported
        String language = reg.getLanguage();
        if (!StringUtils.hasText(language)) {
            throw new RegistrationException();
        }

        // cleanup unregistered keys
        Map<String, String> content = null;
        if (reg.getContent() != null) {
            content = reg.getContent().entrySet().stream()
                    .filter(e -> keys.contains(e.getKey()))
                    .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
        }
        reg.setContent(content);

        // save
        TemplateModel m = templateService.addTemplate(null, authority, realm, reg);
        return m;
    }

    public TemplateModel updateTemplateModel(String realm, String id, TemplateModel reg)
            throws NoSuchTemplateException, NoSuchProviderException, NoSuchAuthorityException {
        TemplateModel m = templateService.getTemplate(id);
        if (!realm.equals(m.getRealm())) {
            throw new IllegalArgumentException("realm-mismatch");
        }

        // check model
        String template = reg.getTemplate();
        String authority = reg.getAuthority();

        if (!StringUtils.hasText(authority) || !StringUtils.hasText(template)) {
            throw new RegistrationException();
        }

        logger.debug("update template {}:{} with authority {} for realm {}", StringUtils.trimAllWhitespace(template),
                StringUtils.trimAllWhitespace(id), StringUtils.trimAllWhitespace(authority),
                StringUtils.trimAllWhitespace(realm));

        Template t = authorityService.getAuthority(authority).getProviderByRealm(realm).getTemplate(template);
        Collection<String> keys = t.keys();

        // check language
        // TODO check if supported
        String language = reg.getLanguage();
        if (!StringUtils.hasText(language)) {
            throw new RegistrationException();
        }

        // cleanup unregistered keys
        Map<String, String> content = null;
        if (reg.getContent() != null) {
            content = reg.getContent().entrySet().stream()
                    .filter(e -> keys.contains(e.getKey()))
                    .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
        }
        reg.setContent(content);

        // save
        m = templateService.updateTemplate(id, reg);
        return m;
    }

    public void deleteTemplateModel(String realm, String id) throws NoSuchTemplateException {

        TemplateModel m = templateService.getTemplate(id);
        if (!realm.equals(m.getRealm())) {
            throw new IllegalArgumentException("realm-mismatch");
        }

        logger.debug("delete template {}:{} with authority {} for realm {}", m.getTemplate(),
                StringUtils.trimAllWhitespace(id), m.getAuthority(), StringUtils.trimAllWhitespace(realm));
        templateService.deleteTemplate(id);
    }
}
