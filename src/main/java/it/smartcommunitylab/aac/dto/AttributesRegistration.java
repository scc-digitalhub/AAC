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

package it.smartcommunitylab.aac.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import it.smartcommunitylab.aac.attributes.model.AttributeSet;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;

@Valid
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AttributesRegistration {

    @Size(max = 128)
    @NotBlank
    private String identifier;

    @Size(max = 128)
    @NotBlank
    private String provider;

    private String name;
    private String description;

    @NotNull
    private List<AttributeRegistration> attributes;

    public AttributesRegistration() {
        this.attributes = new ArrayList<>();
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<AttributeRegistration> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<AttributeRegistration> attributes) {
        this.attributes = attributes;
    }

    @Valid
    public static class AttributeRegistration {

        @Size(max = 128)
        @NotBlank
        private String key;

        private String type;
        private Serializable value;

        private String name;
        private String description;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Serializable getValue() {
            return value;
        }

        public void setValue(Serializable value) {
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    public static AttributesRegistration from(AttributeSet attributeSet) {
        // convert to DTO
        AttributesRegistration reg = new AttributesRegistration();
        reg.identifier = attributeSet.getIdentifier();
        reg.name = attributeSet.getName();
        reg.description = attributeSet.getDescription();

        List<AttributeRegistration> attributes = attributeSet
            .getAttributes()
            .stream()
            .map(a -> {
                AttributeRegistration dto = new AttributeRegistration();
                dto.key = a.getKey();
                dto.type = getFormType(a.getKey(), a.getType().getValue());
                dto.name = StringUtils.hasText(a.getName()) ? a.getName() : a.getKey();
                dto.description = a.getDescription();

                return dto;
            })
            .collect(Collectors.toList());
        reg.setAttributes(attributes);

        return reg;
    }

    private static String getFormType(String key, String type) {
        if ("date".equals(type)) {
            return "date";
        } else if ("datetime".equals(type)) {
            return "datetime-local";
        } else if ("time".equals(type)) {
            return "time";
        } else if ("number".equals(type)) {
            return "number";
        } else if ("boolean".equals(type)) {
            return "checkbox";
        }

        if ("string".equals(type) && key.toLowerCase().startsWith("email")) {
            return "email";
        } else if (
            "string".equals(type) && (key.toLowerCase().startsWith("tel") || key.toLowerCase().startsWith("phone"))
        ) {
            return "tel";
        }

        return "text";
    }
}
