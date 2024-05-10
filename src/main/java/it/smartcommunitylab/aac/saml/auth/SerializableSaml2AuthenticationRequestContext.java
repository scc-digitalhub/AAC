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

package it.smartcommunitylab.aac.saml.auth;

import it.smartcommunitylab.aac.SystemKeys;
import java.io.Serializable;
import org.springframework.security.saml2.provider.service.authentication.AbstractSaml2AuthenticationRequest;
import org.springframework.util.Assert;

public class SerializableSaml2AuthenticationRequestContext implements Serializable {

    private static final long serialVersionUID = SystemKeys.AAC_SAML_SERIAL_VERSION;

    private final String relyingPartyRegistrationId;
    private final String assertingPartyRegistrationId; // required for SPID where there are N+1 registration and we must distinguish the actual registration that identifies the upstream identity provider
    private final String issuer;
    private final String relayState;
    private final AbstractSaml2AuthenticationRequest samlAuthenticationRequest;

    public SerializableSaml2AuthenticationRequestContext(
        String relyingPartyRegistrationId,
        String issuer,
        String relayState,
        AbstractSaml2AuthenticationRequest samlAuthenticationRequest
    ) {
        Assert.hasText(relyingPartyRegistrationId, "relyingPartyRegistrationId is required");
        Assert.hasText(relayState, "relayState is required");
        Assert.hasText(issuer, "issuer is required");

        this.relyingPartyRegistrationId = relyingPartyRegistrationId;
        this.issuer = issuer;
        this.relayState = relayState;
        this.samlAuthenticationRequest = samlAuthenticationRequest;
        this.assertingPartyRegistrationId = null;
    }

    public SerializableSaml2AuthenticationRequestContext(
        String relyingPartyRegistrationId,
        String assertingPartyRegistrationId,
        String issuer,
        String relayState,
        AbstractSaml2AuthenticationRequest samlAuthenticationRequest
    ) {
        Assert.hasText(relyingPartyRegistrationId, "relyingPartyRegistrationId is required");
        Assert.hasText(relayState, "relayState is required");
        Assert.hasText(issuer, "issuer is required");

        this.relyingPartyRegistrationId = relyingPartyRegistrationId;
        this.assertingPartyRegistrationId = assertingPartyRegistrationId;
        this.issuer = issuer;
        this.relayState = relayState;
        this.samlAuthenticationRequest = samlAuthenticationRequest;
    }

    protected SerializableSaml2AuthenticationRequestContext() {
        this((String) null, (String) null, (String) null, (AbstractSaml2AuthenticationRequest) null);
    }

    public String getRelyingPartyRegistrationId() {
        return relyingPartyRegistrationId;
    }

    public String getAssertingPartyRegistrationId() {
        return assertingPartyRegistrationId;
    }

    public String getIssuer() {
        return issuer;
    }

    public String getRelayState() {
        return relayState;
    }

    public AbstractSaml2AuthenticationRequest getSamlAuthenticationRequest() {
        return samlAuthenticationRequest;
    }

    @Override
    public String toString() {
        return (
            "SerializableSaml2AuthenticationRequestContext{" +
            "relyingPartyRegistrationId='" +
            relyingPartyRegistrationId +
            '\'' +
            ", assertingPartyRegistrationId='" +
            assertingPartyRegistrationId +
            '\'' +
            ", issuer='" +
            issuer +
            '\'' +
            ", relayState='" +
            relayState +
            '\'' +
            ", samlAuthenticationRequest=" +
            samlAuthenticationRequest +
            '}'
        );
    }
}
