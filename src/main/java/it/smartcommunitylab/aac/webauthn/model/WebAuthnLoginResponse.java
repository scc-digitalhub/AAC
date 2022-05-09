package it.smartcommunitylab.aac.webauthn.model;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yubico.webauthn.AssertionRequest;

@Valid
@JsonIgnoreProperties(ignoreUnknown = true)
public class WebAuthnLoginResponse {

    private static final ObjectMapper mapper = new ObjectMapper();

    @JsonProperty("key")
    @NotNull
    private String key;

    @NotNull
    private AssertionRequest assertionRequest;

    public String getKey() {
        return this.key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public AssertionRequest getAssertionRequest() {
        return this.assertionRequest;
    }

    public void setAssertionRequest(AssertionRequest assertionrequest) {
        this.assertionRequest = assertionrequest;
    }

    @JsonGetter("assertionRequest")
    public JsonNode getOptionsAsJson() throws JsonProcessingException {
        return mapper.readTree(assertionRequest.toCredentialsGetJson());
    }
}
