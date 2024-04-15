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

package it.smartcommunitylab.aac.attributes.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.base.AbstractAttributeProviderConfig;
import it.smartcommunitylab.aac.attributes.mapper.DefaultAttributesMapper;
import it.smartcommunitylab.aac.attributes.model.ConfigurableAttributeProvider;

public class MapperAttributeProviderConfig extends AbstractAttributeProviderConfig<MapperAttributeProviderConfigMap> {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;
    public static final String RESOURCE_TYPE =
        SystemKeys.RESOURCE_PROVIDER + SystemKeys.ID_SEPARATOR + MapperAttributeProviderConfigMap.RESOURCE_TYPE;

    public MapperAttributeProviderConfig(String provider, String realm) {
        super(
            SystemKeys.AUTHORITY_MAPPER,
            provider,
            realm,
            new AttributeProviderSettingsMap(),
            new MapperAttributeProviderConfigMap()
        );
    }

    public MapperAttributeProviderConfig(
        ConfigurableAttributeProvider cp,
        AttributeProviderSettingsMap settingsMap,
        MapperAttributeProviderConfigMap configMap
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
    private MapperAttributeProviderConfig() {
        super();
    }

    public String getMapperType() {
        return configMap.getType() != null ? configMap.getType() : DefaultAttributesMapper.TYPE;
    }
}
