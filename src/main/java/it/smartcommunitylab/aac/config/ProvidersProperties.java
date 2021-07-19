package it.smartcommunitylab.aac.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.springframework.boot.context.properties.NestedConfigurationProperty;

import it.smartcommunitylab.aac.internal.provider.InternalIdentityProviderConfigMap;
import it.smartcommunitylab.aac.openid.provider.OIDCIdentityProviderConfigMap;
import it.smartcommunitylab.aac.saml.provider.SamlIdentityProviderConfigMap;

public class ProvidersProperties {

    @NestedConfigurationProperty
    private List<ProviderConfiguration> identity;

    @NestedConfigurationProperty
    private List<ProviderConfiguration> attributes;

    @NestedConfigurationProperty
    private ProviderTemplates templates;

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

    public ProviderTemplates getTemplates() {
        return templates;
    }

    public void setTemplates(ProviderTemplates templates) {
        this.templates = templates;
    }

    public static class ProviderConfiguration {
        @NotBlank
        private String authority;
        @NotNull
        private String provider;
        @NotNull
        private String type;

        private String realm;

        private String persistence;

        private String events;

        private String name;

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

        public String getPersistence() {
            return persistence;
        }

        public void setPersistence(String persistence) {
            this.persistence = persistence;
        }

        public String getEvents() {
            return events;
        }

        public void setEvents(String events) {
            this.events = events;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "ProviderConfiguration [authority=" + authority + ", provider=" + provider + ", realm=" + realm
                    + ", type=" + type + "]";
        }

    }

    public static class ProviderTemplates {
        @NestedConfigurationProperty
        private List<OIDCIdentityProviderConfigMap> oidc;

        @NestedConfigurationProperty
        private List<SamlIdentityProviderConfigMap> saml;

        @NestedConfigurationProperty
        private List<InternalIdentityProviderConfigMap> internal;

        public List<OIDCIdentityProviderConfigMap> getOidc() {
            return oidc;
        }

        public void setOidc(List<OIDCIdentityProviderConfigMap> oidc) {
            this.oidc = oidc;
        }

        public List<SamlIdentityProviderConfigMap> getSaml() {
            return saml;
        }

        public void setSaml(List<SamlIdentityProviderConfigMap> saml) {
            this.saml = saml;
        }

        public List<InternalIdentityProviderConfigMap> getInternal() {
            return internal;
        }

        public void setInternal(List<InternalIdentityProviderConfigMap> internal) {
            this.internal = internal;
        }

    }

}
