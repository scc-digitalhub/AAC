package it.smartcommunitylab.aac.webauthn.model;

import java.io.Serializable;
import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@Valid
@JsonIgnoreProperties(ignoreUnknown = true)
public class WebAuthnAttestationResponse {

    @JsonProperty("attestation")
    @NotNull
    private Map<String, Serializable> attestation;

    @JsonProperty("key")
    @NotNull
    private String key;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Map<String, Serializable> getAttestation() {
        return attestation;
    }

    public void setAttestation(Map<String, Serializable> attestation) {
        this.attestation = attestation;
    }
}
