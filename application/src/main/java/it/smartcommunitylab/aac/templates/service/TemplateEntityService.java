/*
 * Copyright 2023 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.templates.service;

import it.smartcommunitylab.aac.common.AlreadyRegisteredException;
import it.smartcommunitylab.aac.common.NoSuchTemplateException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.templates.persistence.TemplateEntity;
import it.smartcommunitylab.aac.templates.persistence.TemplateEntityRepository;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

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
    public Collection<TemplateEntity> findTemplatesByAuthorityAndRealmAndTemplate(
        String authority,
        String realm,
        String template
    ) {
        return templateRepository.findByAuthorityAndRealmAndTemplate(authority, realm, template);
    }

    public TemplateEntity createTemplate(String authority, String realm) {
        String id = UUID.randomUUID().toString();
        return new TemplateEntity(id, authority, realm);
    }

    public TemplateEntity addTemplate(
        String id,
        String authority,
        String realm,
        String template,
        String language,
        Map<String, String> content
    ) throws RegistrationException {
        TemplateEntity t = templateRepository.findOne(id);
        if (t != null) {
            throw new AlreadyRegisteredException();
        }

        // check for same key
        t = templateRepository.findByAuthorityAndRealmAndTemplateAndLanguage(authority, realm, template, language);
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

    public TemplateEntity updateTemplate(String id, String language, Map<String, String> content)
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

    public Page<TemplateEntity> searchTemplatesByKeywords(String realm, String q, Pageable pageRequest) {
        Page<TemplateEntity> page = StringUtils.hasText(q)
            ? templateRepository.findByRealmAndAuthorityContainingIgnoreCaseOrRealmAndTemplateContainingIgnoreCase(
                realm,
                q,
                realm,
                q,
                pageRequest
            )
            : templateRepository.findByRealm(realm, pageRequest);

        return page;
    }
}
