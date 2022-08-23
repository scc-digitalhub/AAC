package it.smartcommunitylab.aac.webauthn.provider;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

import com.yubico.webauthn.data.ResidentKeyRequirement;
import com.yubico.webauthn.data.UserVerificationRequirement;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityProviderConfig;

public class WebAuthnIdentityProviderConfig extends InternalIdentityProviderConfig {
    private static final long serialVersionUID = SystemKeys.AAC_WEBAUTHN_SERIAL_VERSION;
    private static final int TIMEOUT = 9;

    // map capabilities
    private WebAuthnIdentityProviderConfigMap configMap;

    public WebAuthnIdentityProviderConfig(String provider, String realm) {
        super(SystemKeys.AUTHORITY_WEBAUTHN, provider, realm);
        this.configMap = new WebAuthnIdentityProviderConfigMap();
    }

    public WebAuthnIdentityProviderConfigMap getConfigMap() {
        return configMap;
    }

    public void setConfigMap(WebAuthnIdentityProviderConfigMap configMap) {
        this.configMap = configMap;
    }

    @Override
    public Map<String, Serializable> getConfiguration() {
        return configMap.getConfiguration();
    }

    @Override
    public void setConfiguration(Map<String, Serializable> props) {
        configMap = new WebAuthnIdentityProviderConfigMap();
        configMap.setConfiguration(props);
    }

    /*
     * config flags
     */

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
        return configMap.getRegistrationTimeout() != null ? configMap.getRegistrationTimeout().intValue() : TIMEOUT;
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

    /*
     * Static parser
     */
    public static WebAuthnIdentityProviderConfig fromConfigurableProvider(ConfigurableIdentityProvider cp) {
        WebAuthnIdentityProviderConfig ip = new WebAuthnIdentityProviderConfig(cp.getProvider(), cp.getRealm());
        ip.configMap = new WebAuthnIdentityProviderConfigMap();
        ip.configMap.setConfiguration(cp.getConfiguration());

        ip.name = cp.getName();
        ip.description = cp.getDescription();
        ip.icon = cp.getIcon();

        ip.persistence = cp.getPersistence();
        ip.linkable = cp.isLinkable();
        ip.hookFunctions = (cp.getHookFunctions() != null ? cp.getHookFunctions() : Collections.emptyMap());

        return ip;
    }

}
