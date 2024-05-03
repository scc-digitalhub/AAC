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

package it.smartcommunitylab.aac.attributes.base;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.provider.AttributeProviderSettingsMap;
import it.smartcommunitylab.aac.attributes.provider.ConfigurableAttributeProvider;
import it.smartcommunitylab.aac.attributes.provider.UserAttributeProviderConfig;
import it.smartcommunitylab.aac.base.model.AbstractConfigMap;
import it.smartcommunitylab.aac.base.provider.config.AbstractProviderConfig;
import java.util.Collections;
import java.util.Set;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
// @JsonSubTypes(
//     {
//         @Type(value = MapperAttributeProviderConfig.class, name = MapperAttributeProviderConfig.RESOURCE_TYPE),
//         @Type(value = ScriptAttributeProviderConfig.class, name = ScriptAttributeProviderConfig.RESOURCE_TYPE),
//         @Type(value = WebhookAttributeProviderConfig.class, name = WebhookAttributeProviderConfig.RESOURCE_TYPE),
//         @Type(value = InternalAttributeProviderConfig.class, name = InternalAttributeProviderConfig.RESOURCE_TYPE),
//     }
// )
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.ALWAYS)
public abstract class AbstractAttributeProviderConfig<M extends AbstractConfigMap>
    extends AbstractProviderConfig<AttributeProviderSettingsMap, M>
    implements UserAttributeProviderConfig<M> {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    protected AbstractAttributeProviderConfig(
        String authority,
        String provider,
        String realm,
        AttributeProviderSettingsMap settingsMap,
        M configMap
    ) {
        super(authority, provider, realm, settingsMap, configMap);
    }

    protected AbstractAttributeProviderConfig(
        ConfigurableAttributeProvider cp,
        AttributeProviderSettingsMap settingsMap,
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
    protected AbstractAttributeProviderConfig() {
        super();
    }

    public Set<String> getAttributeSets() {
        return settingsMap.getAttributeSets() != null ? settingsMap.getAttributeSets() : Collections.emptySet();
    }

    public String getEvents() {
        //TODO use ENUM and add default
        return settingsMap.getEvents();
    }
}
