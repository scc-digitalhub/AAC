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

package it.smartcommunitylab.aac.templates;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchAuthorityException;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchTemplateException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.ConfigurableProviderManager;
import it.smartcommunitylab.aac.model.Realm;
import it.smartcommunitylab.aac.realms.service.RealmService;
import it.smartcommunitylab.aac.templates.model.ConfigurableTemplateProvider;
import it.smartcommunitylab.aac.templates.model.Language;
import it.smartcommunitylab.aac.templates.model.Template;
import it.smartcommunitylab.aac.templates.model.TemplateModel;
import it.smartcommunitylab.aac.templates.provider.TemplateProviderSettingsMap;
import it.smartcommunitylab.aac.templates.service.LanguageService;
import it.smartcommunitylab.aac.templates.service.TemplateProviderAuthorityService;
import it.smartcommunitylab.aac.templates.service.TemplateProviderService;
import it.smartcommunitylab.aac.templates.service.TemplateService;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Safelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.MethodArgumentNotValidException;

@Service
@PreAuthorize("hasAuthority('" + Config.R_ADMIN + "')" + " or hasAuthority(#realm+':" + Config.R_ADMIN + "')")
public class TemplatesManager
    extends ConfigurableProviderManager<ConfigurableTemplateProvider, TemplateProviderAuthority<?, ?, ?, ?>> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private TemplateService templateService;

    @Autowired
    private RealmService realmService;

    @Autowired
    private TemplateProviderAuthorityService authorityService;

    public TemplatesManager(TemplateProviderService templateProviderService) {
        super(templateProviderService);
    }

    /*
     * Config per realm we always expose a single config per realm
     */
    public ConfigurableTemplateProvider findProviderByRealm(String realm) throws NoSuchRealmException {
        // we expect a single provider per realm, so fetch first
        return super.listProviders(realm).stream().findFirst().orElse(null);
    }

    public ConfigurableTemplateProvider getProviderByRealm(String realm)
        throws NoSuchProviderException, NoSuchRealmException, RegistrationException, SystemException, MethodArgumentNotValidException {
        // fetch first if available
        ConfigurableTemplateProvider provider = findProviderByRealm(realm);

        if (provider == null) {
            // create as new
            String id = SystemKeys.AUTHORITY_TEMPLATE + SystemKeys.SLUG_SEPARATOR + realm;
            ConfigurableTemplateProvider cp = new ConfigurableTemplateProvider(
                SystemKeys.AUTHORITY_TEMPLATE,
                id,
                realm
            );

            try {
                provider = addProvider(realm, cp);
            } catch (NoSuchAuthorityException e) {
                throw new NoSuchProviderException();
            }
        }

        TemplateProviderSettingsMap settingsMap = new TemplateProviderSettingsMap();
        settingsMap.setConfiguration(provider.getSettings());

        if (settingsMap.getLanguages() == null || settingsMap.getLanguages().isEmpty()) {
            Realm r = realmService.findRealm(realm);
            if (
                r != null &&
                r.getLocalizationConfiguration() != null &&
                r.getLocalizationConfiguration().getLanguages() != null
            ) {
                settingsMap.setLanguages(r.getLocalizationConfiguration().getLanguages());
            } else {
                settingsMap.setLanguages(
                    Arrays
                        .asList(LanguageService.LANGUAGES)
                        .stream()
                        .map(l -> Language.parse(l))
                        .collect(Collectors.toSet())
                );
            }

            provider.setSettings(settingsMap.getConfiguration());
        }

        return provider;
    }

    @Override
    public ConfigurableTemplateProvider addProvider(String realm, ConfigurableTemplateProvider provider)
        throws NoSuchRealmException, NoSuchProviderException, NoSuchAuthorityException, RegistrationException, SystemException, MethodArgumentNotValidException {
        // validate style
        // TODO add css validator
        if (provider.getSettings() != null && provider.getSettings().containsKey("customStyle")) {
            String customStyle = String.valueOf(provider.getSettings().get("customStyle"));
            if ("null".equals(customStyle)) {
                customStyle = "";
            }
            // use wholetext to avoid escaping ><& etc.
            // this could break the final document
            // TODO use a proper CSS parser
            Document dirty = Jsoup.parseBodyFragment(customStyle);
            Cleaner cleaner = new Cleaner(Safelist.none());
            Document clean = cleaner.clean(dirty);

            String style = clean.body().wholeText();
            provider.getSettings().put("customStyle", style);
        }

        ConfigurableTemplateProvider cp = super.addProvider(realm, provider);

        // also register as active
        cp = registerProvider(realm, cp.getProvider());
        return cp;
    }

    @Override
    public ConfigurableTemplateProvider updateProvider(
        String realm,
        String providerId,
        ConfigurableTemplateProvider provider
    )
        throws NoSuchRealmException, NoSuchProviderException, NoSuchAuthorityException, RegistrationException, MethodArgumentNotValidException {
        // validate style
        // TODO add css validator
        if (provider.getSettings() != null && provider.getSettings().containsKey("customStyle")) {
            String customStyle = String.valueOf(provider.getSettings().get("customStyle"));
            if ("null".equals(customStyle)) {
                customStyle = "";
            }
            // use wholetext to avoid escaping ><& etc.
            // this could break the final document
            // TODO use a proper CSS parser
            Document dirty = Jsoup.parseBodyFragment(customStyle);
            Cleaner cleaner = new Cleaner(Safelist.none());
            Document clean = cleaner.clean(dirty);

            String style = clean.body().wholeText();
            provider.getSettings().put("customStyle", style);
        }

        ConfigurableTemplateProvider cp = super.updateProvider(realm, providerId, provider);

        // also register as active
        cp = registerProvider(realm, cp.getProvider());
        return cp;
    }

    /*
     * Templates from authorities
     */

    public Collection<String> getAuthorities(String realm) {
        return authorityService.getAuthoritiesIds();
    }

    public Collection<Template> listTemplates(String realm, String authority)
        throws NoSuchProviderException, NoSuchAuthorityException {
        logger.debug(
            "list templates for realm {} from authority {}",
            StringUtils.trimAllWhitespace(realm),
            StringUtils.trimAllWhitespace(authority)
        );
        return authorityService.getAuthority(authority).getProviderByRealm(realm).getTemplates();
    }

    public Template getTemplate(String realm, String authority, String template)
        throws NoSuchTemplateException, NoSuchProviderException, NoSuchAuthorityException {
        logger.debug(
            "get template {} for realm {} from authority {}",
            StringUtils.trimAllWhitespace(template),
            StringUtils.trimAllWhitespace(realm),
            StringUtils.trimAllWhitespace(authority)
        );

        return authorityService.getAuthority(authority).getProviderByRealm(realm).getTemplate(template);
    }

    /*
     * Template models from service
     */
    public TemplateModel findTemplateModel(String realm, String authority, String template, String language) {
        logger.debug(
            "find template model {} for realm {} from authority {} language {}",
            StringUtils.trimAllWhitespace(template),
            StringUtils.trimAllWhitespace(realm),
            StringUtils.trimAllWhitespace(authority),
            StringUtils.trimAllWhitespace(language)
        );

        return templateService.findTemplate(authority, realm, template, language);
    }

    public TemplateModel getTemplateModel(String realm, String id) throws NoSuchTemplateException {
        logger.debug(
            "get template model {} for realm {}",
            StringUtils.trimAllWhitespace(id),
            StringUtils.trimAllWhitespace(realm)
        );

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
        logger.debug(
            "list template models for realm {} for authority {}",
            StringUtils.trimAllWhitespace(realm),
            StringUtils.trimAllWhitespace(authority)
        );

        return templateService.listTemplates(authority, realm);
    }

    public Collection<TemplateModel> listTemplateModels(String realm, String authority, String template) {
        logger.debug(
            "list template {} models for realm {} for authority {}",
            StringUtils.trimAllWhitespace(template),
            StringUtils.trimAllWhitespace(realm),
            StringUtils.trimAllWhitespace(authority)
        );

        return templateService.listTemplates(authority, realm, template);
    }

    public Page<TemplateModel> searchTemplateModels(String realm, String keywords, Pageable pageRequest) {
        logger.debug(
            "search templates for realm {} with keywords {}",
            StringUtils.trimAllWhitespace(realm),
            StringUtils.trimAllWhitespace(keywords)
        );

        String query = StringUtils.trimAllWhitespace(keywords);
        Page<TemplateModel> page = templateService.searchTemplates(realm, query, pageRequest);
        return page;
    }

    public TemplateModel addTemplateModel(String realm, TemplateModel reg)
        throws NoSuchTemplateException, NoSuchProviderException, NoSuchAuthorityException, RegistrationException {
        // check model
        String template = reg.getTemplate();
        String authority = reg.getAuthority();

        if (!StringUtils.hasText(authority) || !StringUtils.hasText(template)) {
            throw new RegistrationException();
        }

        logger.debug(
            "add template {} with authority {} for realm {}",
            StringUtils.trimAllWhitespace(template),
            StringUtils.trimAllWhitespace(authority),
            StringUtils.trimAllWhitespace(realm)
        );

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
            content =
                reg
                    .getContent()
                    .entrySet()
                    .stream()
                    .filter(e -> keys.contains(e.getKey()))
                    .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
        }
        reg.setContent(content);

        // save
        TemplateModel m = templateService.addTemplate(null, authority, realm, reg);
        return m;
    }

    public TemplateModel updateTemplateModel(String realm, String id, TemplateModel reg)
        throws NoSuchTemplateException, NoSuchProviderException, NoSuchAuthorityException, RegistrationException {
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

        logger.debug(
            "update template {}:{} with authority {} for realm {}",
            StringUtils.trimAllWhitespace(template),
            StringUtils.trimAllWhitespace(id),
            StringUtils.trimAllWhitespace(authority),
            StringUtils.trimAllWhitespace(realm)
        );

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
            content =
                reg
                    .getContent()
                    .entrySet()
                    .stream()
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

        logger.debug(
            "delete template {}:{} with authority {} for realm {}",
            m.getTemplate(),
            StringUtils.trimAllWhitespace(id),
            m.getAuthority(),
            StringUtils.trimAllWhitespace(realm)
        );
        templateService.deleteTemplate(id);
    }

    public TemplateModel sanitizeTemplateModel(String realm, String id, TemplateModel reg)
        throws NoSuchTemplateException, NoSuchProviderException, NoSuchAuthorityException, RegistrationException {
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
            content =
                reg
                    .getContent()
                    .entrySet()
                    .stream()
                    .filter(e -> keys.contains(e.getKey()))
                    .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
        }
        reg.setContent(content);

        content = templateService.sanitizeTemplate(id, reg);
        reg.setContent(content);

        return reg;
    }
}
