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

package it.smartcommunitylab.aac.core.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.provider.ResolvableGenericsTypeProvider;
import it.smartcommunitylab.aac.model.ConfigMap;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import org.springframework.core.ResolvableType;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

@Valid
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
// @JsonSubTypes(
//     {
//         @Type(value = ConfigurableAccountService.class, name = SystemKeys.RESOURCE_ACCOUNT),
//         @Type(value = ConfigurableAttributeProvider.class, name = SystemKeys.RESOURCE_ATTRIBUTES),
//         @Type(value = ConfigurableCredentialsProvider.class, name = SystemKeys.RESOURCE_CREDENTIALS),
//         @Type(value = ConfigurableIdentityProvider.class, name = SystemKeys.RESOURCE_IDENTITY),
//         @Type(value = ConfigurableIdentityService.class, name = SystemKeys.RESOURCE_USER),
//         @Type(value = ConfigurableTemplateProvider.class, name = SystemKeys.RESOURCE_TEMPLATE),
//     }
// )
public class ConfigurableProviderImpl<S extends ConfigMap>
    implements ConfigurableProvider<S>, ResolvableGenericsTypeProvider, Serializable {

    private ResolvableType ctype;
    private ResolvableType btype;

    //TODO evaluate ENUM for known types
    @NotBlank
    private String type;

    @NotBlank
    @Size(max = 128)
    private String authority;

    @Size(max = 128)
    @Pattern(regexp = SystemKeys.SLUG_PATTERN)
    // @NotBlank
    private String provider;

    @Size(max = 128)
    @Pattern(regexp = SystemKeys.SLUG_PATTERN)
    @NotBlank
    private String realm;

    //TODO replace with Boolean to allow null
    private boolean enabled;

    //TODO replace with status (ENUM)
    private Boolean registered;

    @Size(max = 128)
    @Pattern(regexp = SystemKeys.SLUG_PATTERN)
    @NotBlank
    private String name;

    private Map<String, String> titleMap = Collections.emptyMap();
    private Map<String, String> descriptionMap = Collections.emptyMap();

    // configMap as raw map - should match schema
    // TODO evaluate adding configMap type as field
    protected Map<String, Serializable> configuration;
    protected Map<String, Serializable> settings;

    @JsonProperty(access = Access.READ_ONLY)
    private Integer version;

    @JsonProperty(access = Access.READ_ONLY)
    protected JsonSchema schema;

    public ConfigurableProviderImpl(String type, String authority, String provider, String realm) {
        Assert.hasText(type, "type is required");
        this.type = type;

        this.authority = authority;
        this.provider = provider;
        this.realm = realm;

        this.configuration = new HashMap<>();
        this.settings = new HashMap<>();

        this.name = provider;
        this.enabled = false;
    }

    /**
     * Private constructor for JPA and other serialization tools.
     *
     * We need to implement this to enable deserialization of resources via
     * reflection
     */
    @SuppressWarnings("unused")
    private ConfigurableProviderImpl() {}

    @Override
    public String getAuthority() {
        return authority;
    }

    @Override
    public void setAuthority(String authority) {
        this.authority = authority;
    }

    @Override
    public String getRealm() {
        return realm;
    }

    @Override
    public void setRealm(String realm) {
        this.realm = realm;
    }

    @Override
    public String getProvider() {
        return provider;
    }

    @Override
    public void setProvider(String provider) {
        this.provider = provider;
    }

    @Override
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        if (StringUtils.hasText(name)) {
            this.name = name;
        }
    }

    @Override
    public Map<String, String> getTitleMap() {
        return titleMap;
    }

    @Override
    public void setTitleMap(Map<String, String> titleMap) {
        this.titleMap = titleMap;
    }

    @Override
    public Map<String, String> getDescriptionMap() {
        return descriptionMap;
    }

    @Override
    public void setDescriptionMap(Map<String, String> descriptionMap) {
        this.descriptionMap = descriptionMap;
    }

    @Override
    public Map<String, Serializable> getConfiguration() {
        return configuration;
    }

    @Override
    public void setConfiguration(Map<String, Serializable> props) {
        this.configuration = new HashMap<>();
        if (props != null) {
            this.configuration.putAll(props);
        }
    }

    public Map<String, Serializable> getSettings() {
        return settings;
    }

    public void setSettings(Map<String, Serializable> settings) {
        this.settings = settings;
    }

    // @Override
    public JsonSchema getSchema() {
        return schema;
    }

    // @Override
    public void setSchema(JsonSchema schema) {
        this.schema = schema;
    }

    @Override
    public Boolean getRegistered() {
        return registered;
    }

    @Override
    public void setRegistered(Boolean registered) {
        this.registered = registered;
    }

    @Override
    public Integer getVersion() {
        return version;
    }

    @Override
    public void setVersion(Integer version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return (
            "ConfigurableProvider [authority=" +
            authority +
            ", realm=" +
            realm +
            ", provider=" +
            provider +
            ", type=" +
            type +
            ", enabled=" +
            enabled +
            ", registered=" +
            registered +
            ", name=" +
            name +
            ", titleMap=" +
            titleMap +
            ", descriptionMap=" +
            descriptionMap +
            ", configuration=" +
            configuration +
            "]"
        );
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
                this.btype = ResolvableType.forClass(ConfigurableProviderImpl.class, getClass());
            } catch (Exception e) {
                //ignore
            }
        }

        return btype != null ? btype.getGeneric(pos) : null;
    }

    @JsonIgnoreProperties
    public ResolvableType getConfigType() {
        return getResolvableType(0);
    }
}
