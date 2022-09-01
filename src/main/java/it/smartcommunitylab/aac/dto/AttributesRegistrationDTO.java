package it.smartcommunitylab.aac.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import it.smartcommunitylab.aac.core.model.AttributeSet;

@Valid
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AttributesRegistrationDTO {

    @Size(max = 128)
    @NotBlank
    private String identifier;

    @Size(max = 128)
    @NotBlank
    private String provider;

    private String name;
    private String description;

    @NotNull
    private List<AttributeDTO> attributes;

    public AttributesRegistrationDTO() {
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

    public List<AttributeDTO> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<AttributeDTO> attributes) {
        this.attributes = attributes;
    }

    @Valid
    public static class AttributeDTO {

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

    public static AttributesRegistrationDTO from(AttributeSet attributeSet) {
        // convert to DTO
        AttributesRegistrationDTO reg = new AttributesRegistrationDTO();
        reg.identifier = attributeSet.getIdentifier();
        reg.name = attributeSet.getName();
        reg.description = attributeSet.getDescription();

        List<AttributeDTO> attributes = attributeSet.getAttributes().stream().map(a -> {
            AttributeDTO dto = new AttributeDTO();
            dto.key = a.getKey();
            dto.type = getFormType(a.getKey(), a.getType().getValue());
            dto.name = StringUtils.hasText(a.getName()) ? a.getName() : a.getKey();
            dto.description = a.getDescription();

            return dto;
        }).collect(Collectors.toList());
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
        } else if ("string".equals(type)
                && (key.toLowerCase().startsWith("tel") || key.toLowerCase().startsWith("phone"))) {
            return "tel";
        }

        return "text";
    }
}
