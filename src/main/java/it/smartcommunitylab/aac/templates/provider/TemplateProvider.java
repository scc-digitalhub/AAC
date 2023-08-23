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

package it.smartcommunitylab.aac.templates.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchTemplateException;
import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.core.provider.ConfigurableResourceProvider;
import it.smartcommunitylab.aac.templates.model.Template;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;

/*
 * Template providers expose template for multi-language customization of views
 */
public interface TemplateProvider<T extends Template, M extends ConfigMap, C extends TemplateProviderConfig<M>>
    extends ConfigurableResourceProvider<T, C, TemplateProviderSettingsMap, M> {
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
