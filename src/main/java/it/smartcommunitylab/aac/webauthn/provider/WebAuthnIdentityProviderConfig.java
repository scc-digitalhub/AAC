package it.smartcommunitylab.aac.webauthn.provider;

import com.yubico.webauthn.data.UserVerificationRequirement;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractIdentityProviderConfig;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;

public class WebAuthnIdentityProviderConfig extends AbstractIdentityProviderConfig<WebAuthnIdentityProviderConfigMap> {
    private static final long serialVersionUID = SystemKeys.AAC_WEBAUTHN_SERIAL_VERSION;

    private static final int TIMEOUT = 9;
    private final static int MAX_SESSION_DURATION = 24 * 60 * 60; // 24h

    public WebAuthnIdentityProviderConfig(String provider, String realm) {
        super(SystemKeys.AUTHORITY_WEBAUTHN, provider, realm, new WebAuthnIdentityProviderConfigMap());
    }

    public WebAuthnIdentityProviderConfig(ConfigurableIdentityProvider cp) {
        super(cp);
    }

    public String getRepositoryId() {
        return configMap.getRepositoryId() != null ? configMap.getRepositoryId() : getRealm();
    }

    /*
     * config flags
     */
    public int getMaxSessionDuration() {
        return configMap.getMaxSessionDuration() != null ? configMap.getMaxSessionDuration().intValue()
                : MAX_SESSION_DURATION;
    }

    public boolean isAllowedUnstrustedAttestation() {
        return configMap.getAllowUntrustedAttestation() != null
                ? configMap.getAllowUntrustedAttestation().booleanValue()
                : false;
    }

    public UserVerificationRequirement getRequireUserVerification() {
        return configMap.getRequireUserVerification() != null ? configMap.getRequireUserVerification()
                : UserVerificationRequirement.PREFERRED;
    }

    public int getLoginTimeout() {
        // return timeout in seconds
        return configMap.getLoginTimeout() != null ? configMap.getLoginTimeout().intValue() : TIMEOUT;
    }

    /*
     * display mode
     */

    public boolean displayAsButton() {
        return configMap.getDisplayAsButton() != null
                ? configMap.getDisplayAsButton().booleanValue()
                : false;
    }

    public boolean isRequireAccountConfirmation() {
        return configMap.getRequireAccountConfirmation() != null
                ? configMap.getRequireAccountConfirmation().booleanValue()
                : true;
    }

}
