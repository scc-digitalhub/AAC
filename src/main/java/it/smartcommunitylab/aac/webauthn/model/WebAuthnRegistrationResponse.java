package it.smartcommunitylab.aac.webauthn.model;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;

@Valid
@JsonIgnoreProperties(ignoreUnknown = true)
public class WebAuthnRegistrationResponse {

    private static final ObjectMapper mapper = new ObjectMapper();

    @JsonProperty("key")
    @NotNull
    private String key;

    @NotNull
    private PublicKeyCredentialCreationOptions options;

    public WebAuthnRegistrationResponse(String key, PublicKeyCredentialCreationOptions options) {
        this.key = key;
        this.options = options;
    }

    public String getKey() {
        return this.key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public PublicKeyCredentialCreationOptions getOptions() {
        return this.options;
    }

    public void setOptions(PublicKeyCredentialCreationOptions options) {
        this.options = options;
    }

    @JsonGetter("options")
    public JsonNode getOptionsAsJson() throws JsonProcessingException {
        return mapper.readTree(options.toCredentialsCreateJson());
    }

}
