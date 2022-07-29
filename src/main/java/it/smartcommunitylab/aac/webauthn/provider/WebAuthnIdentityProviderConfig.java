package it.smartcommunitylab.aac.webauthn.provider;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractIdentityProviderConfig;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;

public class WebAuthnIdentityProviderConfig extends AbstractIdentityProviderConfig {
    private static final long serialVersionUID = SystemKeys.AAC_WEBAUTHN_SERIAL_VERSION;

    private final static int MIN_LINK_DURATION = 3600;

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
    public boolean isEnableRegistration() {
        return configMap.getEnableRegistration() != null ? configMap.getEnableRegistration().booleanValue() : true;
    }

    public boolean isEnableUpdate() {
        return configMap.getEnableUpdate() != null ? configMap.getEnableUpdate().booleanValue() : true;
    }

    public boolean isEnableReset() {
        return configMap.getEnableReset() != null ? configMap.getEnableReset().booleanValue() : false;
    }

    public boolean isEnableConfirmation() {
        return configMap.getEnableConfirmation() != null ? configMap.getEnableConfirmation().booleanValue() : false;
    }

    public int getConfirmationValidity() {
        return configMap.getConfirmationValidity() != null ? configMap.getConfirmationValidity().intValue()
                : MIN_LINK_DURATION;
    }

    public boolean isAllowedUnstrustedAssertions() {
        return configMap.getTrustUnverifiedAuthenticatorResponses() != null
                ? configMap.getTrustUnverifiedAuthenticatorResponses().booleanValue()
                : false;
    }

    public int getMaxSessionDuration() {
        return configMap.getMaxSessionDuration();
    }

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
