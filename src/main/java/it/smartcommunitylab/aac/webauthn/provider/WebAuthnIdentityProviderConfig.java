package it.smartcommunitylab.aac.webauthn.provider;

import com.yubico.webauthn.data.ResidentKeyRequirement;
import com.yubico.webauthn.data.UserVerificationRequirement;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractIdentityProviderConfig;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.internal.model.CredentialsType;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityProviderConfig;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityProviderConfigMap;

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

    /*
     * Repository scoped to realm
     * 
     * TODO evaluate supporting custom scope
     */
    public String getRepositoryId() {
        return this.getRealm();
    }

    public boolean isScopedData() {
        return false;
    }

    public String getScope() {
        return SystemKeys.RESOURCE_REALM;
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
//    public static WebAuthnIdentityProviderConfig fromConfigurableProvider(ConfigurableIdentityProvider cp) {
//        WebAuthnIdentityProviderConfig ip = new WebAuthnIdentityProviderConfig(cp.getProvider(), cp.getRealm());
//        ip.configMap = new WebAuthnIdentityProviderConfigMap();
//        ip.configMap.setConfiguration(cp.getConfiguration());
//
//        ip.name = cp.getName();
//        ip.description = cp.getDescription();
//        ip.icon = cp.getIcon();
//
//        ip.linkable = cp.isLinkable();
//        ip.persistence = cp.getPersistence();
//        ip.events = cp.getEvents();
//        ip.position = cp.getPosition();
//
//        ip.hookFunctions = (cp.getHookFunctions() != null ? cp.getHookFunctions() : Collections.emptyMap());
//        return ip;
//    }
//    
    public InternalIdentityProviderConfig toInternalProviderConfig() {
        InternalIdentityProviderConfig ip = new InternalIdentityProviderConfig(SystemKeys.AUTHORITY_WEBAUTHN,
                getProvider(),
                getRealm());
        InternalIdentityProviderConfigMap cMap = new InternalIdentityProviderConfigMap();
        cMap.setCredentialsType(CredentialsType.WEBAUTHN);
        cMap.setScopedData(false);
        ip.setConfigMap(cMap);

        return ip;
    }

//    /*
//     * Static parser
//     */
//    public static WebAuthnIdentityProviderConfig fromConfigurableProvider(ConfigurableIdentityProvider cp) {
//        WebAuthnIdentityProviderConfig ip = new WebAuthnIdentityProviderConfig(cp.getProvider(), cp.getRealm());
//        ip.configMap = new WebAuthnIdentityProviderConfigMap();
//        ip.configMap.setConfiguration(cp.getConfiguration());
//
//        ip.name = cp.getName();
//        ip.description = cp.getDescription();
//        ip.icon = cp.getIcon();
//
//        ip.persistence = cp.getPersistence();
//        ip.linkable = cp.isLinkable();
//        ip.hookFunctions = (cp.getHookFunctions() != null ? cp.getHookFunctions() : Collections.emptyMap());
//
//        return ip;
//    }

}
