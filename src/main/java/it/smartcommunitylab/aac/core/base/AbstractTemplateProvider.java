package it.smartcommunitylab.aac.core.base;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.common.NoSuchTemplateException;
import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.core.model.ConfigurableTemplateProvider;
import it.smartcommunitylab.aac.core.model.Template;
import it.smartcommunitylab.aac.core.provider.TemplateProvider;
import it.smartcommunitylab.aac.core.provider.TemplateProviderConfig;
import it.smartcommunitylab.aac.templates.model.TemplateModel;
import it.smartcommunitylab.aac.templates.service.TemplateService;

public abstract class AbstractTemplateProvider<T extends TemplateModel, M extends ConfigMap, C extends TemplateProviderConfig<M>>
        extends
        AbstractConfigurableProvider<T, ConfigurableTemplateProvider, M, C>
        implements TemplateProvider<T, M, C>,
        InitializingBean {

    protected final TemplateService templateService;
    protected Map<String, Supplier<TemplateModel>> factories;

    public AbstractTemplateProvider(
            String authority, String providerId,
            TemplateService templateService,
            C providerConfig, String realm) {
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
    public Collection<Template> getTemplates(String language) {
        return factories.keySet().stream()
                .map(e -> {
                    try {
                        return getTemplate(e, language);
                    } catch (NoSuchTemplateException e1) {
                        return null;
                    }
                })
                .filter(t -> t != null)
                .collect(Collectors.toList());
    }

    @Override
    public Template getTemplate(String template, String language) throws NoSuchTemplateException {
        TemplateModel m = getTemplate(template);
        TemplateModel e = templateService.findTemplate(getAuthority(), getRealm(), template,
                language);

        m.setLanguage(language);
        if (e != null) {
            m.setContent(e.getContent());
        }

        return m;
    }

}
