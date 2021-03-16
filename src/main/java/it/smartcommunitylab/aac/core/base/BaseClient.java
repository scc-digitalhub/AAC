package it.smartcommunitylab.aac.core.base;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;


public abstract class BaseClient extends AbstractClient {

    private String name;
    private String description;

    private Set<String> providers;
    
    private Set<String> scopes;

    public BaseClient(String realm, String clientId) {
        super(realm, clientId);

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

    public Set<String> getScopes() {
        return scopes;
    }

    public void setScopes(Set<String> scopes) {
        this.scopes = scopes;
    }

    public Set<String> getProviders() {
        return providers;
    }

    public void setProviders(Set<String> providers) {
        this.providers = providers;
    }

    /*
     * Implementation classes should be able to return a serializable map to
     * represent configuration, with nested objects where needed
     *
     * This is used to import/export models + UI model
     */
    public abstract Map<String, Serializable> getConfigurationMap();

}
