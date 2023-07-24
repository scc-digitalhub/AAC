package it.smartcommunitylab.aac.password.provider;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractIdentityProviderConfig;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;

public class PasswordIdentityProviderConfig extends AbstractIdentityProviderConfig<PasswordIdentityProviderConfigMap> {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;
    public static final String RESOURCE_TYPE =
        SystemKeys.RESOURCE_PROVIDER + SystemKeys.ID_SEPARATOR + PasswordIdentityProviderConfigMap.RESOURCE_TYPE;

    private static final int DEFAULT_SESSION_DURATION = 24 * 60 * 60; // 24h
    private static final int DEFAULT_RESET_DURATION = 900; // 15m

    public PasswordIdentityProviderConfig(
        @JsonProperty("provider") String provider,
        @JsonProperty("realm") String realm
    ) {
        super(SystemKeys.AUTHORITY_PASSWORD, provider, realm, new PasswordIdentityProviderConfigMap());
    }

    public PasswordIdentityProviderConfig(
        ConfigurableIdentityProvider cp,
        PasswordIdentityProviderConfigMap configMap
    ) {
        super(cp, configMap);
    }

    public String getRepositoryId() {
        return configMap.getRepositoryId() != null ? configMap.getRepositoryId() : getRealm();
    }

    /*
     * display mode
     */

    public boolean displayAsButton() {
        return configMap.getDisplayAsButton() != null ? configMap.getDisplayAsButton().booleanValue() : false;
    }

    /*
     * default config
     */
    public boolean isEnablePasswordReset() {
        return configMap.getEnablePasswordReset() != null ? configMap.getEnablePasswordReset().booleanValue() : true;
    }

    public int getPasswordResetValidity() {
        return configMap.getPasswordResetValidity() != null
            ? configMap.getPasswordResetValidity().intValue()
            : DEFAULT_RESET_DURATION;
    }

    public int getMaxSessionDuration() {
        return configMap.getMaxSessionDuration() != null
            ? configMap.getMaxSessionDuration().intValue()
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
