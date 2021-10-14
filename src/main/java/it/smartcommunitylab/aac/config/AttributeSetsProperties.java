package it.smartcommunitylab.aac.config;

import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.springframework.boot.context.properties.NestedConfigurationProperty;

public class AttributeSetsProperties {
    @NestedConfigurationProperty
    private List<AttributeSetDefinition> sets;

    public AttributeSetsProperties() {
        sets = new ArrayList<>();
    }

    public List<AttributeSetDefinition> getSets() {
        return sets;
    }

    public void setSets(List<AttributeSetDefinition> sets) {
        this.sets = sets;
    }

    public static class AttributeSetDefinition {
        @NotBlank
        private String identifier;
        @NotNull
        private String[] keys;

        public String getIdentifier() {
            return identifier;
        }

        public void setIdentifier(String identifier) {
            this.identifier = identifier;
        }

        public String[] getKeys() {
            return keys;
        }

        public void setKeys(String[] keys) {
            this.keys = keys;
        }

    }
}
