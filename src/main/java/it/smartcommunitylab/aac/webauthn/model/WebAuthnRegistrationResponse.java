package it.smartcommunitylab.aac.webauthn.model;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;

@Valid
@JsonIgnoreProperties(ignoreUnknown = true)
public class WebAuthnRegistrationResponse {

    @JsonProperty("key")
    @NotNull
    private String key;

    @JsonProperty("options")
    @JsonSerialize(using = PublicKeyCredentialCreationOptionsSerializer.class)
    @NotNull
    private PublicKeyCredentialCreationOptions options;

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

}
