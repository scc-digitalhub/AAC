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

package it.smartcommunitylab.aac.templates.provider;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.base.model.AbstractSettingsMap;
import it.smartcommunitylab.aac.templates.model.Language;
import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import javax.validation.Valid;

@Valid
@JsonIgnoreProperties(ignoreUnknown = true)
public class TemplateProviderSettingsMap extends AbstractSettingsMap {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    public static final String RESOURCE_TYPE =
        SystemKeys.RESOURCE_SETTINGS + SystemKeys.ID_SEPARATOR + SystemKeys.RESOURCE_TEMPLATE_PROVIDER;

    private Set<Language> languages;
    private String customStyle;

    public Set<Language> getLanguages() {
        return languages;
    }

    public void setLanguages(Set<Language> languages) {
        this.languages = languages;
    }

    public String getCustomStyle() {
        return customStyle;
    }

    public void setCustomStyle(String customStyle) {
        this.customStyle = customStyle;
    }

    @JsonIgnore
    public void setConfiguration(TemplateProviderSettingsMap map) {
        if (map == null) {
            throw new IllegalArgumentException();
        }

        this.languages = map.getLanguages();
        this.customStyle = map.getCustomStyle();
    }

    @Override
    public void setConfiguration(Map<String, Serializable> props) {
        // use mapper
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        TemplateProviderSettingsMap map = mapper.convertValue(props, TemplateProviderSettingsMap.class);
        setConfiguration(map);
    }

    @Override
    public JsonSchema getSchema() throws JsonMappingException {
        return schemaGen.generateSchema(TemplateProviderSettingsMap.class);
    }
}
