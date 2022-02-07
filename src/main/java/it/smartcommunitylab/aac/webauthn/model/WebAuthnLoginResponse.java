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

    @JsonProperty("key")
    @NotNull
    private String key;

    @NotNull
    private AssertionRequest assertionrequest;

    private static final ObjectMapper mapper = new ObjectMapper();

    public String getKey() {
        return this.key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public AssertionRequest getAssertionRequest() {
        return this.assertionrequest;
    }

    public void setAssertionRequest(AssertionRequest assertionrequest) {
        this.assertionrequest = assertionrequest;
    }

    @JsonGetter("assertionRequest")
    public JsonNode getOptionsAsJson() throws JsonProcessingException {
        return mapper.readTree(assertionrequest.toCredentialsGetJson());
    }
}
