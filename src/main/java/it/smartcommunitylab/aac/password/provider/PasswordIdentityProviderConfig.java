package it.smartcommunitylab.aac.password.provider;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityProviderConfig;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityProviderConfigMap;

public class PasswordIdentityProviderConfig extends InternalIdentityProviderConfig {
    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    private final static int MIN_DURATION = 300;
    private final static int PASSWORD_MIN_LENGTH = 2;
    private final static int PASSWORD_MAX_LENGTH = 75;

    // map capabilities
    private PasswordIdentityProviderConfigMap configMap;

    public PasswordIdentityProviderConfig(String provider, String realm) {
        super(SystemKeys.AUTHORITY_PASSWORD, provider, realm);
        this.configMap = new PasswordIdentityProviderConfigMap();
    }

    public InternalIdentityProviderConfigMap getConfigMap() {
        return configMap;
    }

    public void setConfigMap(PasswordIdentityProviderConfigMap configMap) {
        this.configMap = configMap;
        super.setConfigMap(configMap);
    }

    @Override
    public Map<String, Serializable> getConfiguration() {
        return configMap.getConfiguration();
    }

    @Override
    public void setConfiguration(Map<String, Serializable> props) {
        configMap = new PasswordIdentityProviderConfigMap();
        configMap.setConfiguration(props);

        super.setConfigMap(configMap);
    }

    /*
     * config flags
     */
    public boolean isEnablePasswordReset() {
        return configMap.getEnablePasswordReset() != null ? configMap.getEnablePasswordReset().booleanValue() : true;
    }

    public boolean isEnablePasswordSet() {
        return configMap.getEnablePasswordSet() != null ? configMap.getEnablePasswordSet().booleanValue() : true;
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
    public int getConfirmationValidity() {
        return configMap.getConfirmationValidity() != null ? configMap.getConfirmationValidity().intValue()
                : MIN_DURATION;
    }

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

    /*
     * Static parser
     */
    public static PasswordIdentityProviderConfig fromConfigurableProvider(ConfigurableIdentityProvider cp) {
        PasswordIdentityProviderConfig ip = new PasswordIdentityProviderConfig(cp.getProvider(), cp.getRealm());
        ip.configMap = new PasswordIdentityProviderConfigMap();
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
