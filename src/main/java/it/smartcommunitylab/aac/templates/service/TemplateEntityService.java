package it.smartcommunitylab.aac.templates.service;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.common.AlreadyRegisteredException;
import it.smartcommunitylab.aac.common.NoSuchTemplateException;
import it.smartcommunitylab.aac.templates.persistence.TemplateEntity;
import it.smartcommunitylab.aac.templates.persistence.TemplateEntityRepository;

@Service
@Transactional
public class TemplateEntityService {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final TemplateEntityRepository templateRepository;

    public TemplateEntityService(TemplateEntityRepository templateRepository) {
        Assert.notNull(templateRepository, "template repository is mandatory");

        this.templateRepository = templateRepository;
    }

    @Transactional(readOnly = true)
    public TemplateEntity findTemplate(String id) {
        return templateRepository.findOne(id);
    }

    @Transactional(readOnly = true)
    public TemplateEntity getTemplate(String id) throws NoSuchTemplateException {
        TemplateEntity t = templateRepository.findOne(id);
        if (t == null) {
            throw new NoSuchTemplateException();
        }

        return t;
    }

    @Transactional(readOnly = true)
    public TemplateEntity findTemplateBy(String authority, String realm, String template, String language) {
        return templateRepository.findByAuthorityAndRealmAndTemplateAndLanguage(authority, realm, template, language);
    }

    @Transactional(readOnly = true)
    public Collection<TemplateEntity> findTemplatesByRealm(String realm) {
        return templateRepository.findByRealm(realm);

    }

    @Transactional(readOnly = true)
    public Collection<TemplateEntity> findTemplatesByAuthority(String authority) {
        return templateRepository.findByAuthority(authority);
    }

    @Transactional(readOnly = true)
    public Collection<TemplateEntity> findTemplatesByAuthorityAndRealm(String authority, String realm) {
        return templateRepository.findByAuthorityAndRealm(authority, realm);
    }

    @Transactional(readOnly = true)
    public Collection<TemplateEntity> findTemplatesByAuthorityAndRealmAndTemplate(String authority, String realm,
            String template) {
        return templateRepository.findByAuthorityAndRealmAndTemplate(authority, realm, template);
    }

    public TemplateEntity createTemplate(String authority, String realm) {
        String id = UUID.randomUUID().toString();
        return new TemplateEntity(id, authority, realm);
    }

    public TemplateEntity addTemplate(
            String id,
            String authority, String realm,
            String template, String language,
            Map<String, String> content) {
        TemplateEntity t = templateRepository.findOne(id);
        if (t != null) {
            throw new AlreadyRegisteredException();
        }
        logger.debug("add template {}", StringUtils.trimAllWhitespace(id));

        t = new TemplateEntity(id, authority, realm);
        t.setTemplate(template);
        t.setLanguage(language);
        t.setContent(content);

        t = templateRepository.save(t);

        if (logger.isTraceEnabled()) {
            logger.trace("template entity: " + StringUtils.trimAllWhitespace(t.toString()));
        }
        return t;
    }

    public TemplateEntity updateTemplate(
            String id,
            String language,
            Map<String, String> content)
            throws NoSuchTemplateException {
        TemplateEntity t = templateRepository.findOne(id);
        if (t == null) {
            throw new NoSuchTemplateException();
        }
        logger.debug("update template {}", StringUtils.trimAllWhitespace(id));

        t.setLanguage(language);
        t.setContent(content);

        t = templateRepository.save(t);

        if (logger.isTraceEnabled()) {
            logger.trace("template entity: " + StringUtils.trimAllWhitespace(t.toString()));
        }
        return t;
    }

    public void deleteTemplate(String id) {
        TemplateEntity t = templateRepository.findOne(id);
        if (t != null) {
            logger.debug("delete template {}", StringUtils.trimAllWhitespace(id));
            templateRepository.delete(t);
        }

    }

}
