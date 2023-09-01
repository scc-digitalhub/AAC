/*
 * Copyright 2023 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.webauthn.model;

import com.yubico.webauthn.RegistrationResult;
import java.io.Serializable;
import org.springframework.util.Assert;

public class WebAuthnRegistrationRequest implements Serializable {

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
