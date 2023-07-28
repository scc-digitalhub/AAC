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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.mapper.DefaultAttributesMapper;
import it.smartcommunitylab.aac.base.model.AbstractConfigMap;

import java.io.Serializable;
import java.util.Map;
import javax.validation.Valid;

@Valid
@JsonIgnoreProperties(ignoreUnknown = true)
public class MapperAttributeProviderConfigMap extends AbstractConfigMap {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;
    public static final String RESOURCE_TYPE =
        SystemKeys.RESOURCE_CONFIG +
        SystemKeys.ID_SEPARATOR +
        SystemKeys.RESOURCE_ATTRIBUTE_PROVIDER +
        SystemKeys.ID_SEPARATOR +
        SystemKeys.AUTHORITY_MAPPER;

    // mapper type
    // note: all attribute sets will be mapped via the same type
    private String type;

    public MapperAttributeProviderConfigMap() {
        // use default
        type = DefaultAttributesMapper.TYPE;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setConfiguration(MapperAttributeProviderConfigMap map) {
        this.type = map.getType();
    }

    @JsonIgnore
    @Override
    public void setConfiguration(Map<String, Serializable> props) {
        // use mapper
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        MapperAttributeProviderConfigMap map = mapper.convertValue(props, MapperAttributeProviderConfigMap.class);

        // map all props defined in model
        setConfiguration(map);
    }

    @JsonIgnore
    @Override
    public JsonSchema getSchema() throws JsonMappingException {
        return schemaGen.generateSchema(MapperAttributeProviderConfigMap.class);
    }
}
