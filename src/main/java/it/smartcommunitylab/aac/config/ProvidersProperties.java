package it.smartcommunitylab.aac.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

public class ProvidersProperties {

    @NestedConfigurationProperty
    private List<ProviderConfiguration> identity;

    @NestedConfigurationProperty
    private List<ProviderConfiguration> attributes;

    public ProvidersProperties() {
        identity = new ArrayList<>();
        attributes = new ArrayList<>();
    }

    public List<ProviderConfiguration> getIdentity() {
        return identity;
    }

    public void setIdentity(List<ProviderConfiguration> identity) {
        this.identity = identity;
    }

    public List<ProviderConfiguration> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<ProviderConfiguration> attributes) {
        this.attributes = attributes;
    }

    public static class ProviderConfiguration {
        @NotBlank
        private String authority;
        @NotNull
        private String provider;
        @NotNull
        private String type;

        private String realm;

        private Map<String, String> configuration;

        public ProviderConfiguration() {
            this.configuration = new HashMap<>();
        }

        public String getAuthority() {
            return authority;
        }

        public void setAuthority(String authority) {
            this.authority = authority;
        }

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getRealm() {
            return realm;
        }

        public void setRealm(String realm) {
            this.realm = realm;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Map<String, String> getConfiguration() {
            return configuration;
        }

        public void setConfiguration(Map<String, String> configuration) {
            this.configuration = configuration;
        }

        @Override
        public String toString() {
            return "ProviderConfiguration [authority=" + authority + ", provider=" + provider + ", realm=" + realm
                    + ", type=" + type + "]";
        }

    }

}
