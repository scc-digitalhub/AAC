package it.smartcommunitylab.aac.templates.service;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.common.NoSuchTemplateException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.templates.model.TemplateModel;
import it.smartcommunitylab.aac.templates.persistence.TemplateEntity;

@Service
@Transactional
public class TemplateService {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final static Safelist DEFAULT_WHITELIST = Safelist.relaxed();

    private final TemplateEntityService templateService;

    public TemplateService(TemplateEntityService templateService) {
        Assert.notNull(templateService, "template service is mandatory");

        this.templateService = templateService;
    }

    public TemplateModel findTemplate(String id) {
        logger.debug("find template model {} ", StringUtils.trimAllWhitespace(id));

        TemplateEntity e = templateService.findTemplate(id);
        if (e == null) {
            return null;
        }

        return toModel(e);
    }

    public TemplateModel getTemplate(String id) throws NoSuchTemplateException {
        logger.debug("get template model {} ", StringUtils.trimAllWhitespace(id));

        TemplateEntity e = templateService.getTemplate(id);
        return toModel(e);
    }

    public TemplateModel findTemplate(String authority, String realm, String template, String language) {
        logger.debug("find template model {} for realm {} from authority {} language {}",
                StringUtils.trimAllWhitespace(template), StringUtils.trimAllWhitespace(realm),
                StringUtils.trimAllWhitespace(authority), StringUtils.trimAllWhitespace(language));

        TemplateEntity e = templateService.findTemplateBy(authority, realm, template, language);
        if (e == null) {
            return null;
        }

        return toModel(e);
    }

    public TemplateModel getTemplate(String authority, String realm, String template, String language)
            throws NoSuchTemplateException {
        logger.debug("get template model {} for realm {} from authority {} language {}",
                StringUtils.trimAllWhitespace(template), StringUtils.trimAllWhitespace(realm),
                StringUtils.trimAllWhitespace(authority), StringUtils.trimAllWhitespace(language));

        TemplateEntity e = templateService.findTemplateBy(authority, realm, template, language);
        if (e == null) {
            throw new NoSuchTemplateException();
        }

        return toModel(e);
    }

    public Collection<TemplateModel> listTemplatesByRealm(String realm) {
        logger.debug("list template models for realm {} ", StringUtils.trimAllWhitespace(realm));

        return templateService.findTemplatesByRealm(realm).stream().map(t -> toModel(t))
                .collect(Collectors.toList());
    }

    public Collection<TemplateModel> listTemplates(String authority) {
        logger.debug("list template models for authority {}", StringUtils.trimAllWhitespace(authority));

        return templateService.findTemplatesByAuthority(authority).stream().map(t -> toModel(t))
                .collect(Collectors.toList());
    }

    public Collection<TemplateModel> listTemplates(String authority, String realm) {
        logger.debug("list template models for authority {} realm {}", StringUtils.trimAllWhitespace(authority),
                StringUtils.trimAllWhitespace(realm));

        return templateService.findTemplatesByAuthorityAndRealm(authority, realm).stream().map(t -> toModel(t))
                .collect(Collectors.toList());
    }

    public Collection<TemplateModel> listTemplates(String authority, String realm, String template) {
        logger.debug("list template {} models for authority {} realm {}", StringUtils.trimAllWhitespace(template),
                StringUtils.trimAllWhitespace(authority), StringUtils.trimAllWhitespace(realm));

        return templateService.findTemplatesByAuthorityAndRealmAndTemplate(authority, realm, template).stream()
                .map(t -> toModel(t))
                .collect(Collectors.toList());
    }

    public TemplateModel addTemplate(String id, String authority, String realm, TemplateModel reg) {
        if (!StringUtils.hasText(authority) || !StringUtils.hasText(realm)) {
            throw new RegistrationException();
        }

        if (!StringUtils.hasText(id)) {
            id = reg.getId();
        }

        if (!StringUtils.hasText(id)) {
            id = templateService.createTemplate(authority, realm).getId();
        }

        String template = reg.getTemplate();
        String language = reg.getLanguage();

        if (!StringUtils.hasText(template) || !StringUtils.hasText(language)) {
            throw new RegistrationException();
        }

        Map<String, String> content = null;
        if (reg.getContent() != null) {
            content = reg.getContent().entrySet().stream().map(e -> {
                String v = e.getValue();
                if (v == null) {
                    return e;
                }

                return Map.entry(e.getKey(), Jsoup.clean(v, DEFAULT_WHITELIST));
            }).collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
        }

        logger.debug("add template {} for realm {}", StringUtils.trimAllWhitespace(id),
                StringUtils.trimAllWhitespace(realm));
        TemplateEntity e = templateService.addTemplate(id, authority, realm, template, language, content);
        return toModel(e);
    }

    public TemplateModel updateTemplate(String id, TemplateModel reg) throws NoSuchTemplateException {
        String template = reg.getTemplate();
        String language = reg.getLanguage();

        if (!StringUtils.hasText(template) || !StringUtils.hasText(language)) {
            throw new RegistrationException();
        }

        Map<String, String> content = null;
        if (reg.getContent() != null) {
            content = reg.getContent().entrySet().stream().map(e -> {
                String v = e.getValue();
                if (v == null) {
                    return e;
                }

                return Map.entry(e.getKey(), Jsoup.clean(v, DEFAULT_WHITELIST));
            }).collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
        }

        logger.debug("update template {}", StringUtils.trimAllWhitespace(id));
        TemplateEntity e = templateService.updateTemplate(id, language, content);
        return toModel(e);
    }

    public void deleteTemplate(String id) throws NoSuchTemplateException {
        TemplateEntity e = templateService.getTemplate(id);

        logger.debug("delete template {}", StringUtils.trimAllWhitespace(id));
        templateService.deleteTemplate(e.getId());
    }

    /*
     * converter
     */
    private TemplateModel toModel(TemplateEntity e) {
        TemplateModel m = new TemplateModel(e.getAuthority(), e.getRealm(), null, e.getTemplate());
        m.setId(e.getId());
        m.setLanguage(e.getLanguage());
        m.setContent(e.getContent());

        return m;
    }
}
