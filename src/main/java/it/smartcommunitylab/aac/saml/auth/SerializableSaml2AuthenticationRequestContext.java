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
import org.springframework.util.Assert;

public class SerializableSaml2AuthenticationRequestContext implements Serializable {

    private static final long serialVersionUID = SystemKeys.AAC_SAML_SERIAL_VERSION;

    private String relyingPartyRegistrationId;
    private String issuer;
    private String relayState;

    public SerializableSaml2AuthenticationRequestContext(
        String relyingPartyRegistrationId,
        String issuer,
        String relayState
    ) {
        Assert.hasText(relyingPartyRegistrationId, "relyingPartyRegistrationId is required");
        Assert.hasText(relayState, "relayState is required");
        Assert.hasText(issuer, "issuer is required");

        this.relyingPartyRegistrationId = relyingPartyRegistrationId;
        this.issuer = issuer;
        this.relayState = relayState;
    }

    protected SerializableSaml2AuthenticationRequestContext() {
        this((String) null, (String) null, (String) null);
    }

    public String getRelyingPartyRegistrationId() {
        return relyingPartyRegistrationId;
    }

    public void setRelyingPartyRegistrationId(String relyingPartyRegistrationId) {
        this.relyingPartyRegistrationId = relyingPartyRegistrationId;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getRelayState() {
        return relayState;
    }

    public void setRelayState(String relayState) {
        this.relayState = relayState;
    }
}
