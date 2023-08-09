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
import it.smartcommunitylab.aac.templates.base.AbstractTemplateProviderConfig;
import it.smartcommunitylab.aac.templates.model.ConfigurableTemplateProvider;

public class RealmTemplateProviderConfig extends AbstractTemplateProviderConfig<TemplateProviderConfigMap> {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;
    public static final String RESOURCE_TYPE =
        SystemKeys.RESOURCE_PROVIDER + SystemKeys.ID_SEPARATOR + TemplateProviderConfigMap.RESOURCE_TYPE;

    private String customStyle;

    public RealmTemplateProviderConfig(
        String authority,
        String provider,
        String realm,
        TemplateProviderConfigMap configMap
    ) {
        super(authority, provider, realm, configMap);
    }

    public RealmTemplateProviderConfig(ConfigurableTemplateProvider cp, TemplateProviderConfigMap configMap) {
        super(cp, configMap);
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
    }

    public String getCustomStyle() {
        return customStyle;
    }

    public void setCustomStyle(String customStyle) {
        this.customStyle = customStyle;
    }
}
