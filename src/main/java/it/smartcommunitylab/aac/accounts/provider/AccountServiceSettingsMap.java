/**
 * Copyright 2023 Fondazione Bruno Kessler
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

package it.smartcommunitylab.aac.accounts.provider;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.base.model.AbstractSettingsMap;
import it.smartcommunitylab.aac.model.PersistenceMode;
import jakarta.validation.Valid;
import java.io.Serializable;
import java.util.Map;

@Valid
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountServiceSettingsMap extends AbstractSettingsMap {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    public static final String RESOURCE_TYPE =
        SystemKeys.RESOURCE_SETTINGS + SystemKeys.ID_SEPARATOR + SystemKeys.RESOURCE_ACCOUNT_SERVICE;

    private String repositoryId;
    private PersistenceMode persistence;

    public String getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }

    public PersistenceMode getPersistence() {
        return persistence;
    }

    public void setPersistence(PersistenceMode persistence) {
        this.persistence = persistence;
    }

    @JsonIgnore
    public void setConfiguration(AccountServiceSettingsMap map) {
        if (map == null) {
            throw new IllegalArgumentException();
        }

        this.repositoryId = map.getRepositoryId();
        this.persistence = map.getPersistence();
    }

    @Override
    @JsonIgnore
    public void setConfiguration(Map<String, Serializable> props) {
        // use mapper
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        AccountServiceSettingsMap map = mapper.convertValue(props, AccountServiceSettingsMap.class);
        setConfiguration(map);
    }

    @Override
    @JsonIgnore
    public JsonSchema getSchema() throws JsonMappingException {
        return schemaGen.generateSchema(AccountServiceSettingsMap.class);
    }
}
