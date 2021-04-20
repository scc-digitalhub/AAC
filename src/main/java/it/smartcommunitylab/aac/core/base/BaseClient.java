package it.smartcommunitylab.aac.core.base;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;

public abstract class BaseClient extends AbstractClient {

    private String name;

    private String description;

    private Set<String> providers;

    private Set<String> scopes;

    private Set<String> resourceIds;

    private Map<String, String> hookFunctions;
    private Map<String, String> hookWebUrls;

    public BaseClient(String realm, String clientId) {
        super(realm, clientId);

    }

    @JsonIgnore
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @JsonIgnore
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @JsonIgnore
    public Set<String> getScopes() {
        return scopes;
    }

    public void setScopes(Set<String> scopes) {
        this.scopes = scopes;
    }

    @JsonIgnore
    public Set<String> getResourceIds() {
        return resourceIds;
    }

    public void setResourceIds(Set<String> resourceIds) {
        this.resourceIds = resourceIds;
    }

    @JsonIgnore
    public Set<String> getProviders() {
        return providers;
    }

    public void setProviders(Set<String> providers) {
        this.providers = providers;
    }

    @JsonIgnore
    public Map<String, String> getHookFunctions() {
        return hookFunctions;
    }

    public void setHookFunctions(Map<String, String> hookFunctions) {
        this.hookFunctions = hookFunctions;
    }

    @JsonIgnore
    public Map<String, String> getHookWebUrls() {
        return hookWebUrls;
    }

    public void setHookWebUrls(Map<String, String> hookWebUrls) {
        this.hookWebUrls = hookWebUrls;
    }

    /*
     * Implementation classes should be able to return a serializable map to
     * represent configuration, with nested objects where needed
     *
     * This is used to import/export models + UI model
     */
    @JsonIgnore
    public abstract Map<String, Serializable> getConfigurationMap();

}
