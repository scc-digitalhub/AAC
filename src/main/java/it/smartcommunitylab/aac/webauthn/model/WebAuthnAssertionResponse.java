package it.smartcommunitylab.aac.webauthn.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.Serializable;
import java.util.Map;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Valid
@JsonIgnoreProperties(ignoreUnknown = true)
public class WebAuthnAssertionResponse {

    private static ObjectMapper mapper = new ObjectMapper();

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

    @JsonIgnore
    public String toJson() throws JsonProcessingException {
        return mapper.writeValueAsString(this.assertion);
    }
}
