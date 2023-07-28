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

package it.smartcommunitylab.aac.base;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.base.model.AbstractConfigMap;
import it.smartcommunitylab.aac.core.provider.config.AbstractConfigurableProviderI;
import it.smartcommunitylab.aac.core.provider.config.ProviderConfig;

import java.io.Serializable;
import java.util.Locale;
import java.util.Map;

public abstract class AbstractProviderConfig<M extends AbstractConfigMap, T extends AbstractConfigurableProviderI>
    implements ProviderConfig<M>, Serializable {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    private final String authority;
    private final String realm;
    private final String provider;

    protected String name;
    protected Map<String, String> titleMap;
    protected Map<String, String> descriptionMap;

    protected M configMap;
    protected int version;

    protected AbstractProviderConfig(String authority, String provider, String realm, M configMap) {
        this.authority = authority;
        this.realm = realm;
        this.provider = provider;
        this.configMap = configMap;
        this.version = 0;
    }

    protected AbstractProviderConfig(T cp, M configMap) {
        this(cp.getAuthority(), cp.getProvider(), cp.getRealm(), configMap);
        this.name = cp.getName();
        this.titleMap = cp.getTitleMap();
        this.descriptionMap = cp.getDescriptionMap();

        this.version = cp.getVersion() != null ? cp.getVersion() : 0;
    }

    /**
     * Private constructor for JPA and other serialization tools.
     *
     * We need to implement this to enable deserialization of resources via
     * reflection
     */
    @SuppressWarnings("unused")
    private AbstractProviderConfig() {
        this((String) null, (String) null, (String) null, null);
    }

    public String getAuthority() {
        return authority;
    }

    public String getRealm() {
        return realm;
    }

    public String getProvider() {
        return provider;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle(Locale locale) {
        String lang = locale.getLanguage();
        if (titleMap != null) {
            return titleMap.get(lang);
        }

        return null;
    }

    public Map<String, String> getTitleMap() {
        return titleMap;
    }

    public void setTitleMap(Map<String, String> titleMap) {
        this.titleMap = titleMap;
    }

    public String getDescription(Locale locale) {
        String lang = locale.getLanguage();
        if (descriptionMap != null) {
            return descriptionMap.get(lang);
        }

        return null;
    }

    public Map<String, String> getDescriptionMap() {
        return descriptionMap;
    }

    public void setDescriptionMap(Map<String, String> descriptionMap) {
        this.descriptionMap = descriptionMap;
    }

    public M getConfigMap() {
        return configMap;
    }

    public void setConfigMap(M configMap) {
        this.configMap = configMap;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }
}
