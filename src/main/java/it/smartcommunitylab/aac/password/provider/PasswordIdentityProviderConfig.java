package it.smartcommunitylab.aac.password.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractIdentityProviderConfig;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.internal.model.CredentialsType;

public class PasswordIdentityProviderConfig
        extends AbstractIdentityProviderConfig<PasswordIdentityProviderConfigMap> {
    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    private final static int DEFAULT_SESSION_DURATION = 24 * 60 * 60; // 24h
    private final static int DEFAULT_RESET_DURATION = 900; // 15m

    public PasswordIdentityProviderConfig(String provider, String realm) {
        super(SystemKeys.AUTHORITY_PASSWORD, provider, realm, new PasswordIdentityProviderConfigMap());
    }

    public PasswordIdentityProviderConfig(ConfigurableIdentityProvider cp) {
        super(cp);
    }

    public String getRepositoryId() {
        return configMap.getRepositoryId() != null ? configMap.getRepositoryId() : getRealm();
    }

    public CredentialsType getCredentialsType() {
        return CredentialsType.PASSWORD;
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
     * default config
     */
    public boolean isEnablePasswordReset() {
        return configMap.getEnablePasswordReset() != null ? configMap.getEnablePasswordReset().booleanValue() : true;
    }

    public int getPasswordResetValidity() {
        return configMap.getPasswordResetValidity() != null ? configMap.getPasswordResetValidity().intValue()
                : DEFAULT_RESET_DURATION;
    }

    public int getMaxSessionDuration() {
        return configMap.getMaxSessionDuration() != null ? configMap.getMaxSessionDuration().intValue()
                : DEFAULT_SESSION_DURATION;
    }

    /*
     * Account confirmation
     */
    public boolean isRequireAccountConfirmation() {
        return configMap.getRequireAccountConfirmation() != null
                ? configMap.getRequireAccountConfirmation().booleanValue()
                : true;
    }

}
