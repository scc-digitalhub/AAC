package it.smartcommunitylab.aac.webauthn.provider;

import com.yubico.webauthn.data.ResidentKeyRequirement;
import com.yubico.webauthn.data.UserVerificationRequirement;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractCredentialsServiceConfig;
import it.smartcommunitylab.aac.core.model.ConfigurableCredentialsProvider;

public class WebAuthnCredentialsServiceConfig
        extends AbstractCredentialsServiceConfig<WebAuthnIdentityProviderConfigMap> {
    private static final long serialVersionUID = SystemKeys.AAC_WEBAUTHN_SERIAL_VERSION;

    private static final int DEFAULT_TIMEOUT = 30;

    public WebAuthnCredentialsServiceConfig(String provider, String realm) {
        super(SystemKeys.AUTHORITY_WEBAUTHN, provider, realm, new WebAuthnIdentityProviderConfigMap());
    }

    public WebAuthnCredentialsServiceConfig(ConfigurableCredentialsProvider cp) {
        super(cp);
    }

    public String getRepositoryId() {
        return configMap.getRepositoryId() != null ? configMap.getRepositoryId() : getRealm();
    }

    /*
     * config flags
     */
    public boolean isRequireAccountConfirmation() {
        return configMap.getRequireAccountConfirmation() != null
                ? configMap.getRequireAccountConfirmation().booleanValue()
                : true;
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

    public ResidentKeyRequirement getRequireResidentKey() {
        return configMap.getRequireResidentKey() != null ? configMap.getRequireResidentKey()
                : ResidentKeyRequirement.PREFERRED;
    }

    public int getRegistrationTimeout() {
        // return timeout in seconds
        return configMap.getRegistrationTimeout() != null ? configMap.getRegistrationTimeout().intValue()
                : DEFAULT_TIMEOUT;
    }

}
