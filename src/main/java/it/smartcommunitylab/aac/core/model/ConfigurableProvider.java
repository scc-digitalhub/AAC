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

package it.smartcommunitylab.aac.core.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import it.smartcommunitylab.aac.core.provider.config.ConfigurableProviderImpl;
import java.io.Serializable;
import java.util.Map;

/*
 * Mutable configuration for resource providers
 */
//DISABLED deserialization annotation due to conflict with jsonSubTypes annotated on implementation
//due to bug in jackson annotation collection this does not work
// @JsonDeserialize(as = ConfigurableProviderImpl.class)
public interface ConfigurableProvider<S extends ConfigMap> extends ConfigurableProperties {
    String getType();

    String getAuthority();
    void setAuthority(String authority);

    String getRealm();
    void setRealm(String realm);

    String getProvider();
    void setProvider(String provider);

    String getName();
    void setName(String name);

    Map<String, String> getTitleMap();
    void setTitleMap(Map<String, String> titleMap);

    Map<String, String> getDescriptionMap();
    void setDescriptionMap(Map<String, String> descriptionMap);

    boolean isEnabled();
    void setEnabled(boolean enabled);

    Boolean getRegistered();
    void setRegistered(Boolean registered);

    Integer getVersion();
    void setVersion(Integer version);

    Map<String, Serializable> getSettings();
    void setSettings(Map<String, Serializable> props);

    JsonSchema getSchema();
    void setSchema(JsonSchema schema);
}
