package it.smartcommunitylab.aac.core.provider;

import java.util.Collection;
import java.util.Map;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchTemplateException;
import it.smartcommunitylab.aac.core.model.Template;

public interface TemplateProvider extends ResourceProvider<Template> {

    public Collection<String> getLanguages();

    public Collection<Template> getTemplates();

    public Map<String, String> getContext();

    public Template getTemplate(String template) throws NoSuchTemplateException;

    public Template getTemplate(String template, String language) throws NoSuchTemplateException;

    default String getType() {
        return SystemKeys.RESOURCE_TEMPLATE;
    }

    default String getProvider() {
        // single provider per authority/realm
        return getAuthority() + "." + getRealm();
    }

}
