package it.smartcommunitylab.aac.password.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractCredentialsServiceConfig;
import it.smartcommunitylab.aac.core.model.ConfigurableCredentialsProvider;

public class PasswordCredentialsServiceConfig
        extends AbstractCredentialsServiceConfig<PasswordIdentityProviderConfigMap> {
    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;
    public static final String RESOURCE_TYPE = SystemKeys.RESOURCE_PROVIDER + SystemKeys.ID_SEPARATOR
            + SystemKeys.RESOURCE_CONFIG + SystemKeys.ID_SEPARATOR
            + SystemKeys.RESOURCE_CREDENTIALS_SERVICE + SystemKeys.ID_SEPARATOR
            + SystemKeys.AUTHORITY_PASSWORD;
    
    private final static int MIN_DURATION = 300;
    private final static int PASSWORD_MIN_LENGTH = 2;
    private final static int PASSWORD_MAX_LENGTH = 75;

    public PasswordCredentialsServiceConfig(String provider, String realm) {
        super(SystemKeys.AUTHORITY_PASSWORD, provider, realm, new PasswordIdentityProviderConfigMap());
    }

    public PasswordCredentialsServiceConfig(ConfigurableCredentialsProvider cp,
            PasswordIdentityProviderConfigMap configMap) {
        super(cp, configMap);
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

    public boolean isEnablePasswordReset() {
        return configMap.getEnablePasswordReset() != null ? configMap.getEnablePasswordReset().booleanValue() : true;
    }

    public boolean isPasswordRequireAlpha() {
        return configMap.getPasswordRequireAlpha() != null ? configMap.getPasswordRequireAlpha().booleanValue() : false;
    }

    public boolean isPasswordRequireUppercaseAlpha() {
        return configMap.getPasswordRequireUppercaseAlpha() != null
                ? configMap.getPasswordRequireUppercaseAlpha().booleanValue()
                : false;
    }

    public boolean isPasswordRequireNumber() {
        return configMap.getPasswordRequireNumber() != null ? configMap.getPasswordRequireNumber().booleanValue()
                : false;
    }

    public boolean isPasswordRequireSpecial() {
        return configMap.getPasswordRequireSpecial() != null ? configMap.getPasswordRequireSpecial().booleanValue()
                : false;
    }

    public boolean isPasswordSupportWhitespace() {
        return configMap.getPasswordSupportWhitespace() != null
                ? configMap.getPasswordSupportWhitespace().booleanValue()
                : false;
    }

    /*
     * default config
     */

    public int getPasswordResetValidity() {
        return configMap.getPasswordResetValidity() != null ? configMap.getPasswordResetValidity().intValue()
                : MIN_DURATION;
    }

    public int getPasswordMinLength() {
        return configMap.getPasswordMinLength() != null ? configMap.getPasswordMinLength().intValue()
                : PASSWORD_MIN_LENGTH;
    }

    public int getPasswordMaxLength() {
        return configMap.getPasswordMaxLength() != null ? configMap.getPasswordMaxLength().intValue()
                : PASSWORD_MAX_LENGTH;
    }

    public int getPasswordKeepNumber() {
        return configMap.getPasswordKeepNumber() != null ? configMap.getPasswordKeepNumber().intValue()
                : 0;
    }

    public int getPasswordMaxDays() {
        return configMap.getPasswordMaxDays() != null ? configMap.getPasswordMaxDays().intValue()
                : -1;
    }

}
