package it.smartcommunitylab.aac.password.provider;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchTemplateException;
import it.smartcommunitylab.aac.core.model.Template;
import it.smartcommunitylab.aac.core.provider.TemplateProvider;
import it.smartcommunitylab.aac.password.templates.PasswordLoginTemplate;
import it.smartcommunitylab.aac.templates.model.TemplateModel;
import it.smartcommunitylab.aac.templates.service.TemplateService;

public class PasswordTemplateProvider implements TemplateProvider {

    private final TemplateService templateService;

    private final String realm;

    public PasswordTemplateProvider(String realm, TemplateService templateService) {
        Assert.hasText(realm, "realm can not be null or empty");
        Assert.notNull(templateService, "template service is required");

        this.realm = realm;
        this.templateService = templateService;
    }

    @Override
    public String getAuthority() {
        return SystemKeys.AUTHORITY_PASSWORD;
    }

    @Override
    public String getRealm() {
        return realm;
    }

    @Override
    public Collection<String> getLanguages() {
        // TODO from config
        return null;
    }

    @Override
    public Collection<Template> getTemplates() {
        Map<String, Template> map = new HashMap<>();
        // always add all templates
        map.put(PasswordLoginTemplate.TEMPLATE, new PasswordLoginTemplate(realm));

        // fetch from db
        Collection<TemplateModel> models = templateService.listTemplates(realm, realm);
        models.forEach(m -> {
            if (PasswordLoginTemplate.TEMPLATE.equals(m.getTemplate())) {
                map.put(PasswordLoginTemplate.TEMPLATE, new PasswordLoginTemplate(realm, m));
            }
        });

        return map.values();
    }

    @Override
    public Map<String, String> getContext() {
        // nothing available
        return Collections.emptyMap();
    }

    @Override
    public Template getTemplate(String template) throws NoSuchTemplateException {
        if (PasswordLoginTemplate.TEMPLATE.equals(template)) {
            return new PasswordLoginTemplate(realm);
        }

        throw new NoSuchTemplateException();
    }

    @Override
    public Template getTemplate(String template, String language) throws NoSuchTemplateException {
        TemplateModel model = templateService.findTemplate(SystemKeys.AUTHORITY_PASSWORD, realm, template, language);

        if (PasswordLoginTemplate.TEMPLATE.equals(template)) {
            return model != null ? new PasswordLoginTemplate(realm, model) : new PasswordLoginTemplate(realm);
        }

        throw new NoSuchTemplateException();
    }

}
