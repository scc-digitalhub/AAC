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

package it.smartcommunitylab.aac.base.provider.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.model.ConfigurableProvider;
import it.smartcommunitylab.aac.core.model.ProviderConfig;
import it.smartcommunitylab.aac.core.provider.ResolvableGenericsTypeProvider;
import it.smartcommunitylab.aac.model.ConfigMap;
import java.io.Serializable;
import java.util.Locale;
import java.util.Map;
import org.springframework.core.ResolvableType;

// @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.ALWAYS)
public abstract class AbstractProviderConfig<S extends ConfigMap, M extends ConfigMap>
    implements ProviderConfig<S, M>, ResolvableGenericsTypeProvider, Serializable {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;
    private ResolvableType ctype;
    private ResolvableType btype;

    private String authority;
    private String realm;
    private String provider;

    protected String name;
    protected Map<String, String> titleMap;
    protected Map<String, String> descriptionMap;

    protected S settingsMap;
    protected M configMap;
    protected int version;

    protected AbstractProviderConfig(String authority, String provider, String realm, S settingsMap, M configMap) {
        this.authority = authority;
        this.realm = realm;
        this.provider = provider;

        this.settingsMap = settingsMap;
        this.configMap = configMap;

        this.version = 0;
    }

    protected AbstractProviderConfig(ConfigurableProvider<S> cp, S settingsMap, M configMap) {
        this(cp.getAuthority(), cp.getProvider(), cp.getRealm(), settingsMap, configMap);
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
    protected AbstractProviderConfig() {
        this((String) null, (String) null, (String) null, null, null);
    }

    public String getAuthority() {
        return authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
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

    public S getSettingsMap() {
        return settingsMap;
    }

    public void setSettingsMap(S settingsMap) {
        this.settingsMap = settingsMap;
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

    @Override
    @JsonIgnoreProperties
    public ResolvableType getResolvableType() {
        if (this.ctype == null) {
            try {
                this.ctype = ResolvableType.forClass(getClass());
            } catch (Exception e) {
                //ignore
            }
        }

        return ctype;
    }

    @Override
    @JsonIgnoreProperties
    public ResolvableType getResolvableType(int pos) {
        if (this.btype == null) {
            try {
                this.btype = ResolvableType.forClass(AbstractProviderConfig.class, getClass());
            } catch (Exception e) {
                //ignore
            }
        }

        return btype != null ? btype.getGeneric(pos) : null;
    }

    @JsonIgnoreProperties
    public ResolvableType getSettingsType() {
        return getResolvableType(0);
    }

    @JsonIgnoreProperties
    public ResolvableType getConfigType() {
        return getResolvableType(1);
    }
}
