package it.smartcommunitylab.aac.dto;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

public class CustomizationBean {
    private String identifier;

//    @JsonIgnore
    private Map<String, String> resources = Collections.emptyMap();

    public CustomizationBean() {
        resources = new HashMap<>();
    }

    public CustomizationBean(String identifier) {
        this.identifier = identifier;
        this.resources = new HashMap<>();
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

//    @JsonAnyGetter
    public Map<String, String> getResources() {
        return resources;
    }

    public void setResources(Map<String, String> resources) {
        this.resources = resources;
    }

//    @JsonAnySetter
    public void addResource(String key, String value) {
        resources.put(key, value);
    }

    @JsonIgnore
    public String getResource(String key, String lang) {
        String k = key + "_" + lang;
        if (resources.containsKey(k)) {
            return resources.get(k);
        }

        return resources.get(key);
    }

    @JsonIgnore
    public String getResource(String key) {
        return resources.get(key);
    }

}
