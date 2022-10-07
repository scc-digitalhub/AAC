package it.smartcommunitylab.aac.core.provider;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchTemplateException;
import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.core.model.ConfigurableTemplateProvider;
import it.smartcommunitylab.aac.core.model.Template;

/*
 * Template providers expose template for multi-language customization of views
 */
public interface TemplateProvider<T extends Template, M extends ConfigMap, C extends TemplateProviderConfig<M>>
        extends ConfigurableResourceProvider<T, ConfigurableTemplateProvider, M, C> {

    /*
     * Get languages enabled for this provider
     */
    public Collection<String> getLanguages();

//    /*
//     * Get a list of template keys managed by this provider
//     */
//    public Collection<String> getTemplateKeys();

//    public Collection<Template> getTemplates();

    /*
     * Get a blank template as source for customization
     */
    public Collection<Template> getTemplates();

    public Template getTemplate(String template) throws NoSuchTemplateException;

    /*
     * Get a localized template for view
     */
    public Collection<Template> getTemplates(Locale locale);

    public Template getTemplate(String template, Locale locale) throws NoSuchTemplateException;

    /*
     * Context for extensions
     */
    public Map<String, String> getContext();

    default String getType() {
        return SystemKeys.RESOURCE_TEMPLATE;
    }

    default String getProvider() {
        // single provider per authority/realm
        return getAuthority() + "." + getRealm();
    }

}
