package it.smartcommunitylab.aac.webauthn.model;

import java.io.Serializable;
import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Valid
@JsonIgnoreProperties(ignoreUnknown = true)
public class WebAuthnAttestationResponse {

    @JsonProperty("attestation")
    @NotNull
    private Map<String, Serializable> attestation;

    @JsonProperty("key")
    @NotNull
    private String key;

    private static ObjectMapper mapper = new ObjectMapper();

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

    public String toJson() throws JsonProcessingException{
        return mapper.writeValueAsString(attestation);
    }
}
