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

package it.smartcommunitylab.aac.templates.base;

import it.smartcommunitylab.aac.base.model.AbstractConfigMap;
import it.smartcommunitylab.aac.base.provider.AbstractConfigurableResourceProvider;
import it.smartcommunitylab.aac.common.NoSuchTemplateException;
import it.smartcommunitylab.aac.templates.model.ConfigurableTemplateProvider;
import it.smartcommunitylab.aac.templates.model.Template;
import it.smartcommunitylab.aac.templates.model.TemplateModel;
import it.smartcommunitylab.aac.templates.provider.TemplateProvider;
import it.smartcommunitylab.aac.templates.service.TemplateService;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

public abstract class AbstractTemplateProvider<
    T extends TemplateModel, M extends AbstractConfigMap, C extends AbstractTemplateProviderConfig<M>
>
    extends AbstractConfigurableResourceProvider<T, ConfigurableTemplateProvider, M, C>
    implements TemplateProvider<T, M, C>, InitializingBean {

    protected final TemplateService templateService;
    protected Map<String, Supplier<TemplateModel>> factories;

    protected AbstractTemplateProvider(
        String authority,
        String providerId,
        TemplateService templateService,
        C providerConfig,
        String realm
    ) {
        super(authority, providerId, realm, providerConfig);
        Assert.notNull(templateService, "template service is required");

        this.templateService = templateService;
        this.factories = null;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(factories, "template factories can not be null");
    }

    @Override
    public Collection<String> getLanguages() {
        return config.getLanguages();
    }

    @Override
    public Map<String, String> getContext() {
        // nothing available by default
        return Collections.emptyMap();
    }

    @Override
    public Collection<Template> getTemplates() {
        return factories.entrySet().stream().map(e -> e.getValue().get()).collect(Collectors.toList());
    }

    @Override
    public TemplateModel getTemplate(String template) throws NoSuchTemplateException {
        Supplier<TemplateModel> sp = factories.get(template);
        if (sp == null) {
            throw new NoSuchTemplateException();
        }

        TemplateModel m = sp.get();
        m.setRealm(getRealm());
        m.setProvider(getProvider());

        return m;
    }

    @Override
    public Collection<Template> getTemplates(Locale locale) {
        Assert.notNull(locale, "locale can not be null");

        return factories
            .keySet()
            .stream()
            .map(e -> {
                try {
                    return getTemplate(e, locale);
                } catch (NoSuchTemplateException e1) {
                    return null;
                }
            })
            .filter(t -> t != null)
            .collect(Collectors.toList());
    }

    @Override
    public Template getTemplate(String template, Locale locale) throws NoSuchTemplateException {
        Assert.notNull(locale, "locale can not be null");

        String language = locale.getLanguage();
        TemplateModel m = getTemplate(template);
        TemplateModel e = templateService.findTemplate(getAuthority(), getRealm(), template, language);

        m.setLanguage(language);
        if (e != null) {
            m.setId(e.getId());            
            m.setContent(e.getContent());
        }

        return m;
    }
}
