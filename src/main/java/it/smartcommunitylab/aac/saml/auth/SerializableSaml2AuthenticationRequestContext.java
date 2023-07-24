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
