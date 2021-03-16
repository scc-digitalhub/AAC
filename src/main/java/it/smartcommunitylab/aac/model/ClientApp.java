package it.smartcommunitylab.aac.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import it.smartcommunitylab.aac.SystemKeys;

@JsonInclude(Include.NON_NULL) 
public class ClientApp {

    @Pattern(regexp = SystemKeys.SLUG_PATTERN)
    private String clientId;

    @Pattern(regexp = SystemKeys.SLUG_PATTERN)
    private String realm;

    @NotBlank
    private String type;

    @NotBlank
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

    public ClientApp() {
        this.configuration = new HashMap<>();
        this.scopes = new ArrayList<>();
        this.providers = new ArrayList<>();
        this.name = "";
        this.description = "";
    }

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
