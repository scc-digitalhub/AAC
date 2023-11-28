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

package it.smartcommunitylab.aac.internal.provider;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.base.model.AbstractConfigMap;
import java.io.Serializable;
import java.util.Map;
import javax.validation.Valid;

@Valid
@JsonIgnoreProperties(ignoreUnknown = true)
public class InternalAttributeProviderConfigMap extends AbstractConfigMap {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;
    public static final String RESOURCE_TYPE =
        SystemKeys.RESOURCE_CONFIG +
        SystemKeys.ID_SEPARATOR +
        SystemKeys.RESOURCE_ATTRIBUTE_PROVIDER +
        SystemKeys.ID_SEPARATOR +
        SystemKeys.AUTHORITY_INTERNAL;

    private Boolean usermode;
    private Boolean askAtLogin;

    public InternalAttributeProviderConfigMap() {}

    public Boolean getUsermode() {
        return usermode;
    }

    public void setUsermode(Boolean usermode) {
        this.usermode = usermode;
    }

    public Boolean getAskAtLogin() {
        return askAtLogin;
    }

    public void setAskAtLogin(Boolean askAtLogin) {
        this.askAtLogin = askAtLogin;
    }

    public void setConfiguration(InternalAttributeProviderConfigMap map) {
        this.usermode = map.getUsermode();
    }

    @JsonIgnore
    @Override
    public void setConfiguration(Map<String, Serializable> props) {
        // use mapper
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        InternalAttributeProviderConfigMap map = mapper.convertValue(props, InternalAttributeProviderConfigMap.class);

        // map all props defined in model
        setConfiguration(map);
    }

    @JsonIgnore
    @Override
    public JsonSchema getSchema() throws JsonMappingException {
        return schemaGen.generateSchema(InternalAttributeProviderConfigMap.class);
    }
}
