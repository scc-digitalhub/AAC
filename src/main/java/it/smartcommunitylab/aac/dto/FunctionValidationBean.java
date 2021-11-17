package it.smartcommunitylab.aac.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@Valid
@JsonInclude(Include.ALWAYS)
@JsonIgnoreProperties(ignoreUnknown = true)
public class FunctionValidationBean {
    // function invocation
    @NotBlank
    private String name;

    @NotBlank
    private String code;

    private Set<String> scopes = new HashSet<>();

    private Map<String, Serializable> context = new HashMap<>();
    private Map<String, Serializable> attributes = new HashMap<>();

    // function result
    private Map<String, Serializable> result = new HashMap<>();
    private List<String> errors = new ArrayList<>();

    // execution log
    private List<String> messages;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Set<String> getScopes() {
        return scopes;
    }

    public void setScopes(Set<String> scopes) {
        this.scopes = scopes;
    }

    public Map<String, Serializable> getContext() {
        return context;
    }

    public void setContext(Map<String, Serializable> context) {
        this.context = context;
    }

    public Map<String, Serializable> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Serializable> attributes) {
        this.attributes = attributes;
    }

    public Map<String, Serializable> getResult() {
        return result;
    }

    public void setResult(Map<String, Serializable> result) {
        this.result = result;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public void addError(String error) {
        this.errors.add(error);
    }

    public List<String> getMessages() {
        return messages;
    }

    public void setMessages(List<String> messages) {
        this.messages = messages;
    }

    public void addMessage(String msg) {
        this.messages.add(msg);
    }

}
