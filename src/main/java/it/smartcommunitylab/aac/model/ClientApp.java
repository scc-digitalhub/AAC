package it.smartcommunitylab.aac.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;

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

    private JsonSchema schema;

    // scopes
    // TODO evaluate a better mapping for services+attribute sets etc
    private String[] scopes = new String[0];

    private String[] resourceIds = new String[0];

    // providers enabled
    private String[] providers = new String[0];

    // roles
    // TODO

    // mappers
    // TODO

    // hook
    // TODO map to fixed list or explode
    @JsonIgnore
    private Map<String, String> hookFunctions;
    private Map<String, String> hookWebUrls;
    private String hookUniqueSpaces;

    public ClientApp() {
        this.configuration = new HashMap<>();
        this.hookFunctions = new HashMap<>();
        this.hookWebUrls = new HashMap<>();
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

    public String[] getScopes() {
        return scopes;
    }

    public void setScopes(String[] scopes) {
        this.scopes = scopes;
    }

    public String[] getResourceIds() {
        return resourceIds;
    }

    public void setResourceIds(String[] resourceIds) {
        this.resourceIds = resourceIds;
    }

    public String[] getProviders() {
        return providers;
    }

    public void setProviders(String[] providers) {
        this.providers = providers;
    }

    public Map<String, String> getHookFunctions() {
        return hookFunctions;
    }

    public void setHookFunctions(Map<String, String> hookFunctions) {
        this.hookFunctions = hookFunctions;
    }

    public Map<String, String> getHookWebUrls() {
        return hookWebUrls;
    }

    public void setHookWebUrls(Map<String, String> hookWebUrls) {
        this.hookWebUrls = hookWebUrls;
    }

    public String getHookUniqueSpaces() {
        return hookUniqueSpaces;
    }

    public void setHookUniqueSpaces(String hookUniqueSpaces) {
        this.hookUniqueSpaces = hookUniqueSpaces;
    }

    public JsonSchema getSchema() {
        return schema;
    }

    public void setSchema(JsonSchema schema) {
        this.schema = schema;
    }

    @JsonProperty("hookFunctions")
    public Map<String, String> getHookFunctionsBase64() {
        if (hookFunctions == null) {
            return null;
        }
        return hookFunctions.entrySet().stream()
                .filter(e -> StringUtils.hasText(e.getValue()))
                .collect(Collectors.toMap(e -> e.getKey(), e -> {
                    return Base64.getEncoder().encodeToString(e.getValue().getBytes());
                }));
    }

    @JsonProperty("hookFunctions")
    public void setHookFunctionsBase64(Map<String, String> hookFunctions) {
        if (hookFunctions != null) {
            this.hookFunctions = hookFunctions.entrySet().stream()
                    .filter(e -> StringUtils.hasText(e.getValue()))
                    .collect(Collectors.toMap(e -> e.getKey(), e -> {
                        return new String(Base64.getDecoder().decode(e.getValue().getBytes()));
                    }));
        }
    }

}
