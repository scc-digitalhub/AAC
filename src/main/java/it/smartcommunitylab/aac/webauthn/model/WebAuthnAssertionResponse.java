package it.smartcommunitylab.aac.webauthn.model;

import java.io.Serializable;
import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@Valid
@JsonIgnoreProperties(ignoreUnknown = true)
public class WebAuthnAssertionResponse {

    @JsonProperty("assertion")
    @NotNull
    private Map<String, Serializable> assertion;

    @JsonProperty("key")
    @NotNull
    private String key;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Map<String, Serializable> getAssertion() {
        return assertion;
    }

    public void setAssertion(Map<String, Serializable> assertion) {
        this.assertion = assertion;
    }
}
