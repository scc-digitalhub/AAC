package it.smartcommunitylab.aac.model;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class ClientApp {
    private String clientId;
    private String realm;
    private String type;

    private String name;
    private String description;

    // configuration, type-specific
    private Map<String, Serializable> configuration;

    // scopes
    // TODO evaluate a better mapping for services+attribute sets etc
    private Collection<String> scopes;

    // providers enabled
    private Collection<String> providers;

    // roles
    // TODO

    // mappers
    // TODO

    // hook
    // TODO

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
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

    public Map<String, Serializable> getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Map<String, Serializable> configuration) {
        this.configuration = configuration;
    }

    public Collection<String> getScopes() {
        return scopes;
    }

    public void setScopes(Collection<String> scopes) {
        this.scopes = scopes;
    }

    public Collection<String> getProviders() {
        return providers;
    }

    public void setProviders(Collection<String> providers) {
        this.providers = providers;
    }

}
