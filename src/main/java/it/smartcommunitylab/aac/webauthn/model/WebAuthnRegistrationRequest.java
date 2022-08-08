package it.smartcommunitylab.aac.webauthn.model;

import org.springframework.util.Assert;

import com.yubico.webauthn.RegistrationResult;

public class WebAuthnRegistrationRequest {

    private final String userHandle;

    private WebAuthnRegistrationStartRequest startRequest;

    private CredentialCreationInfo credentialCreationInfo;

    private AttestationResponse attestationResponse;

    private RegistrationResult registrationResult;

    public WebAuthnRegistrationRequest(String userHandle) {
        Assert.hasText(userHandle, "userHandle can not be null or empty");
        this.userHandle = userHandle;
    }

    public String getUserHandle() {
        return userHandle;
    }

    public WebAuthnRegistrationStartRequest getStartRequest() {
        return startRequest;
    }

    public void setStartRequest(WebAuthnRegistrationStartRequest startRequest) {
        this.startRequest = startRequest;
    }

    public CredentialCreationInfo getCredentialCreationInfo() {
        return credentialCreationInfo;
    }

    public void setCredentialCreationInfo(CredentialCreationInfo credentialCreationInfo) {
        this.credentialCreationInfo = credentialCreationInfo;
    }

    public AttestationResponse getAttestationResponse() {
        return attestationResponse;
    }

    public void setAttestationResponse(AttestationResponse attestationResponse) {
        this.attestationResponse = attestationResponse;
    }

    public RegistrationResult getRegistrationResult() {
        return registrationResult;
    }

    public void setRegistrationResult(RegistrationResult registrationResult) {
        this.registrationResult = registrationResult;
    }

}
