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

package it.smartcommunitylab.aac.openidfed.provider;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.base.model.AbstractStatusMap;

import javax.validation.Valid;
import java.io.Serializable;
import java.util.Map;

@Valid
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenIdFedIdentityProviderStatusMap extends AbstractStatusMap {

    private static final long serialVersionUID = SystemKeys.AAC_OPENIDFED_SERIAL_VERSION;

    public static final String RESOURCE_TYPE =
            SystemKeys.RESOURCE_CONFIG +
                    SystemKeys.ID_SEPARATOR +
                    SystemKeys.RESOURCE_IDENTITY_PROVIDER +
                    SystemKeys.ID_SEPARATOR +
                    SystemKeys.AUTHORITY_OPENIDFED;

    private String entityConfigurationUrl;
    private String clientId;

    public String getEntityConfigurationUrl() {
        return entityConfigurationUrl;
    }

    public void setEntityConfigurationUrl(String entityConfigurationUrl) { this.entityConfigurationUrl = entityConfigurationUrl; }

    public String getClientId() { return clientId; }

    public void setClientId(String clientId) { this.clientId = clientId; }

    public OpenIdFedIdentityProviderStatusMap() {}

    @JsonIgnore
    public void setConfiguration(OpenIdFedIdentityProviderStatusMap map) {
        this.entityConfigurationUrl = map.getEntityConfigurationUrl();
        this.clientId = map.getClientId();
    }

    @Override
    public void setConfiguration(Map<String, Serializable> props) {
        // use mapper
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        OpenIdFedIdentityProviderStatusMap map = mapper.convertValue(props, OpenIdFedIdentityProviderStatusMap.class);

        setConfiguration(map);
    }

    @JsonIgnore
    public JsonSchema getSchema() throws JsonMappingException {
        return schemaGen.generateSchema(OpenIdFedIdentityProviderStatusMap.class);
    }
}
