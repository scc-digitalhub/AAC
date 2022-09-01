package it.smartcommunitylab.aac.webauthn.model;

import javax.validation.Valid;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Valid
@JsonIgnoreProperties(ignoreUnknown = true)
public class AttestationResponse {

    private String attestation;

    public String getAttestation() {
        return attestation;
    }

    public void setAttestation(String attestation) {
        this.attestation = attestation;
    }

}
