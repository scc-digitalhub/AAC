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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.base.model.AbstractConfigMap;
import it.smartcommunitylab.aac.base.provider.config.AbstractProviderConfig;
import it.smartcommunitylab.aac.templates.model.ConfigurableTemplateProvider;
import it.smartcommunitylab.aac.templates.provider.RealmTemplateProviderConfig;
import it.smartcommunitylab.aac.templates.provider.TemplateProviderConfig;
import it.smartcommunitylab.aac.templates.service.LanguageService;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({ @Type(value = RealmTemplateProviderConfig.class, name = RealmTemplateProviderConfig.RESOURCE_TYPE) })
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.ALWAYS)
public abstract class AbstractTemplateProviderConfig<M extends AbstractConfigMap>
    extends AbstractProviderConfig<M, ConfigurableTemplateProvider>
    implements TemplateProviderConfig<M> {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    protected Set<String> languages;

    protected AbstractTemplateProviderConfig(String authority, String provider, String realm, M configMap) {
        super(authority, provider, realm, configMap);
        this.languages = Collections.emptySet();
    }

    protected AbstractTemplateProviderConfig(ConfigurableTemplateProvider cp, M configMap) {
        super(cp, configMap);
        this.languages = cp.getLanguages();
    }

    public Set<String> getLanguages() {
        return (languages != null && !languages.isEmpty())
            ? languages
            : new TreeSet<>(Arrays.asList(LanguageService.LANGUAGES));
    }

    public void setLanguages(Set<String> languages) {
        this.languages = languages;
    }
}
