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
import it.smartcommunitylab.aac.core.base.AbstractProviderConfig;
import it.smartcommunitylab.aac.core.provider.config.ConfigurableTemplateProvider;
import it.smartcommunitylab.aac.core.provider.config.TemplateProviderConfig;
import it.smartcommunitylab.aac.templates.service.LanguageService;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

public class RealmTemplateProviderConfig
    extends AbstractProviderConfig<TemplateProviderConfigMap, ConfigurableTemplateProvider>
    implements TemplateProviderConfig<TemplateProviderConfigMap> {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;
    public static final String RESOURCE_TYPE =
        SystemKeys.RESOURCE_PROVIDER + SystemKeys.ID_SEPARATOR + TemplateProviderConfigMap.RESOURCE_TYPE;

    private Set<String> languages;
    private String customStyle;

    public RealmTemplateProviderConfig(
        String authority,
        String provider,
        String realm,
        TemplateProviderConfigMap configMap
    ) {
        super(authority, provider, realm, configMap);
        this.languages = Collections.emptySet();
    }

    public RealmTemplateProviderConfig(ConfigurableTemplateProvider cp, TemplateProviderConfigMap configMap) {
        super(cp, configMap);
        this.languages = cp.getLanguages();
        this.customStyle = cp.getCustomStyle();
    }

    /**
     * Private constructor for JPA and other serialization tools.
     *
     * We need to implement this to enable deserialization of resources via
     * reflection
     */
    @SuppressWarnings("unused")
    public RealmTemplateProviderConfig() {
        super((String) null, (String) null, (String) null, new TemplateProviderConfigMap());
        this.languages = Collections.emptySet();
    }

    public Set<String> getLanguages() {
        return (languages != null && !languages.isEmpty())
            ? languages
            : new TreeSet<>(Arrays.asList(LanguageService.LANGUAGES));
    }

    public void setLanguages(Set<String> languages) {
        this.languages = languages;
    }

    public String getCustomStyle() {
        return customStyle;
    }

    public void setCustomStyle(String customStyle) {
        this.customStyle = customStyle;
    }
}
