package it.smartcommunitylab.aac.bootstrap;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.validation.annotation.Validated;

@Configuration
@PropertySource(factory = YamlPropertySourceFactory.class, value = "classpath:bootstrap.yml")
@ConfigurationProperties(prefix = "bootstrap")
@Validated
public class BootstrapConfig {
    @NestedConfigurationProperty
    private List<BootstrapClient> clients;

    @NestedConfigurationProperty
    private List<BootstrapUser> users;

    public BootstrapConfig() {
        this.users = new ArrayList<>();
        this.clients = new ArrayList<>();
    }

    public List<BootstrapClient> getClients() {
        return clients;
    }

    public void setClients(List<BootstrapClient> clients) {
        this.clients = clients;
    }

    public List<BootstrapUser> getUsers() {
        return users;
    }

    public void setUsers(List<BootstrapUser> users) {
        this.users = users;
    }

    public static class BootstrapUser {
        private String username;
        private String password;
        private String[] roles;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String[] getRoles() {
            return roles;
        }

        public void setRoles(String[] roles) {
            this.roles = roles;
        }

    }

    public static class BootstrapClient {
        private String id;
        private String secret;
        private String developer;
        private String[] grantTypes;
        private String[] scopes;
        private String[] redirectUris;
        private String[] uniqueSpaces;
        private String claimMappingFunction;
        private String afterApprovalWebhook;
        private boolean isTrusted;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public String getDeveloper() {
            return developer;
        }

        public void setDeveloper(String developer) {
            this.developer = developer;
        }

        public String[] getGrantTypes() {
            return grantTypes;
        }

        public void setGrantTypes(String[] grantTypes) {
            this.grantTypes = grantTypes;
        }

        public String[] getScopes() {
            return scopes;
        }

        public void setScopes(String[] scopes) {
            this.scopes = scopes;
        }

        public String[] getRedirectUris() {
            return redirectUris;
        }

        public void setRedirectUris(String[] redirectUris) {
            this.redirectUris = redirectUris;
        }

        public String[] getUniqueSpaces() {
            return uniqueSpaces;
        }

        public void setUniqueSpaces(String[] uniqueSpaces) {
            this.uniqueSpaces = uniqueSpaces;
        }

        public String getClaimMappingFunction() {
            return claimMappingFunction;
        }

        public void setClaimMappingFunction(String claimMappingFunction) {
            this.claimMappingFunction = claimMappingFunction;
        }

        public String getAfterApprovalWebhook() {
            return afterApprovalWebhook;
        }

        public void setAfterApprovalWebhook(String afterApprovalWebhook) {
            this.afterApprovalWebhook = afterApprovalWebhook;
        }

        public boolean isTrusted() {
            return isTrusted;
        }

        public void setTrusted(boolean isTrusted) {
            this.isTrusted = isTrusted;
        }

    }

}
