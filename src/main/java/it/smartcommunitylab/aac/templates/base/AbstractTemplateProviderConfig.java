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
import it.smartcommunitylab.aac.templates.model.Language;
import it.smartcommunitylab.aac.templates.provider.RealmTemplateProviderConfig;
import it.smartcommunitylab.aac.templates.provider.TemplateProviderConfig;
import it.smartcommunitylab.aac.templates.provider.TemplateProviderSettingsMap;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({ @Type(value = RealmTemplateProviderConfig.class, name = RealmTemplateProviderConfig.RESOURCE_TYPE) })
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.ALWAYS)
public abstract class AbstractTemplateProviderConfig<M extends AbstractConfigMap>
    extends AbstractProviderConfig<TemplateProviderSettingsMap, M>
    implements TemplateProviderConfig<M> {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    protected AbstractTemplateProviderConfig(
        String authority,
        String provider,
        String realm,
        TemplateProviderSettingsMap settingsMap,
        M configMap
    ) {
        super(authority, provider, realm, settingsMap, configMap);
    }

    protected AbstractTemplateProviderConfig(
        ConfigurableTemplateProvider cp,
        TemplateProviderSettingsMap settingsMap,
        M configMap
    ) {
        super(cp, settingsMap, configMap);
    }

    /**
     * Private constructor for JPA and other serialization tools.
     *
     * We need to implement this to enable deserialization of resources via
     * reflection
     */

    @SuppressWarnings("unused")
    protected AbstractTemplateProviderConfig() {
        super();
    }

    @Override
    public Set<Language> getLanguages() {
        return !CollectionUtils.isEmpty(settingsMap.getLanguages())
            ? settingsMap.getLanguages()
            : new TreeSet<>(Arrays.asList(Language.values()));
    }

    @Override
    public String getCustomStyle() {
        return StringUtils.hasText(settingsMap.getCustomStyle()) ? settingsMap.getCustomStyle() : "";
    }
}
