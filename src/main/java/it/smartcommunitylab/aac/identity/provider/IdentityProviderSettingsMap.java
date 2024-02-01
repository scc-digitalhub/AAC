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

package it.smartcommunitylab.aac.identity.provider;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.base.model.AbstractSettingsMap;
import it.smartcommunitylab.aac.model.PersistenceMode;
import it.smartcommunitylab.aac.repository.SafeString;
import java.io.Serializable;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import javax.validation.Valid;
import org.springframework.util.StringUtils;

@Valid
@JsonIgnoreProperties(ignoreUnknown = true)
public class IdentityProviderSettingsMap extends AbstractSettingsMap {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    public static final String RESOURCE_TYPE =
        SystemKeys.RESOURCE_SETTINGS + SystemKeys.ID_SEPARATOR + SystemKeys.RESOURCE_IDENTITY_PROVIDER;

    private Boolean linkable;
    private PersistenceMode persistence;
    private String events;
    private Integer position;
    private String template;

    @SafeString
    private String notes;

    @JsonIgnore
    private Map<String, String> hookFunctions = new HashMap<>();

    public Boolean getLinkable() {
        return linkable;
    }

    public void setLinkable(Boolean linkable) {
        this.linkable = linkable;
    }

    public PersistenceMode getPersistence() {
        return persistence;
    }

    public void setPersistence(PersistenceMode persistence) {
        this.persistence = persistence;
    }

    public String getEvents() {
        return events;
    }

    public void setEvents(String events) {
        this.events = events;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Map<String, String> getHookFunctions() {
        return hookFunctions;
    }

    public void setHookFunctions(Map<String, String> hookFunctions) {
        this.hookFunctions = hookFunctions;
    }

    @JsonProperty("hookFunctions")
    public Map<String, String> getHookFunctionsBase64() {
        if (hookFunctions == null) {
            return Collections.emptyMap();
        }

        return hookFunctions
            .entrySet()
            .stream()
            .filter(e -> StringUtils.hasText(e.getValue()))
            .collect(
                Collectors.toMap(
                    e -> e.getKey(),
                    e -> {
                        return Base64.getEncoder().withoutPadding().encodeToString(e.getValue().getBytes());
                    }
                )
            );
    }

    @JsonProperty("hookFunctions")
    public void setHookFunctionsBase64(Map<String, String> hookFunctions) {
        if (hookFunctions != null) {
            this.hookFunctions =
                hookFunctions
                    .entrySet()
                    .stream()
                    .filter(e -> StringUtils.hasText(e.getValue()))
                    .collect(
                        Collectors.toMap(
                            e -> e.getKey(),
                            e -> {
                                return new String(Base64.getDecoder().decode(e.getValue().getBytes()));
                            }
                        )
                    );
        }
    }

    @JsonIgnore
    public void setConfiguration(IdentityProviderSettingsMap map) {
        if (map == null) {
            throw new IllegalArgumentException();
        }

        this.linkable = map.getLinkable();
        this.persistence = map.getPersistence();
        this.events = map.getEvents();
        this.position = map.getPosition();
        this.hookFunctions = map.getHookFunctions();
        this.notes = map.getNotes();
    }

    @Override
    @JsonIgnore
    public void setConfiguration(Map<String, Serializable> props) {
        // use mapper
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        IdentityProviderSettingsMap map = mapper.convertValue(props, IdentityProviderSettingsMap.class);
        setConfiguration(map);
    }

    @Override
    @JsonIgnore
    public JsonSchema getSchema() throws JsonMappingException {
        return schemaGen.generateSchema(IdentityProviderSettingsMap.class);
    }
}
