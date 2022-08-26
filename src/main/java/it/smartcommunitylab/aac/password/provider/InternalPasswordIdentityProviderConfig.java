package it.smartcommunitylab.aac.password.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractIdentityProviderConfig;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.internal.model.CredentialsType;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityProviderConfig;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityProviderConfigMap;

public class InternalPasswordIdentityProviderConfig
        extends AbstractIdentityProviderConfig<InternalPasswordIdentityProviderConfigMap> {
    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    private final static int MIN_DURATION = 300;
    private final static int MAX_SESSION_DURATION = 24 * 60 * 60; // 24h
    private final static int PASSWORD_MIN_LENGTH = 2;
    private final static int PASSWORD_MAX_LENGTH = 75;

    public InternalPasswordIdentityProviderConfig(String provider, String realm) {
        super(SystemKeys.AUTHORITY_INTERNAL, provider, realm, new InternalPasswordIdentityProviderConfigMap());
    }

    public InternalPasswordIdentityProviderConfig(ConfigurableIdentityProvider cp) {
        super(cp);
    }

    public String getRepositoryId() {
        // scoped providers will use their id as providerId for data repositories
        // otherwise they'll expose realm slug as id
        if (isScopedData()) {
            return this.getProvider();
        } else {
            return this.getRealm();
        }
    }

    public CredentialsType getCredentialsType() {
        return CredentialsType.PASSWORD;
    }

    public boolean isScopedData() {
        return configMap.getScopedData() != null ? configMap.getScopedData().booleanValue() : false;
    }

    public String getScope() {
        if (isScopedData()) {
            return SystemKeys.RESOURCE_PROVIDER;
        }

        return SystemKeys.RESOURCE_REALM;
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
     * config flags
     */
    public static InternalPasswordIdentityProviderConfig fromConfigurableProvider(ConfigurableIdentityProvider cp) {
        InternalPasswordIdentityProviderConfig ip = new InternalPasswordIdentityProviderConfig(cp.getProvider(),
                cp.getRealm());
        // parse and use setter to properly propagate config to super
        InternalPasswordIdentityProviderConfigMap configMap = new InternalPasswordIdentityProviderConfigMap();
        configMap.setConfiguration(cp.getConfiguration());
        ip.setConfigMap(configMap);

        ip.name = cp.getName();
        ip.description = cp.getDescription();
        ip.icon = cp.getIcon();

        ip.linkable = cp.isLinkable();
        ip.persistence = cp.getPersistence();
        ip.events = cp.getEvents();
        ip.position = cp.getPosition();

        ip.hookFunctions = (cp.getHookFunctions() != null ? cp.getHookFunctions() : Collections.emptyMap());
        return ip;
    }
    
    public boolean isEnableRegistration() {
        return configMap.getEnableRegistration() != null ? configMap.getEnableRegistration().booleanValue() : true;
    }

    public boolean isEnableUpdate() {
        return configMap.getEnableUpdate() != null ? configMap.getEnableUpdate().booleanValue() : true;
    }

    public boolean isConfirmationRequired() {
        return configMap.getConfirmationRequired() != null ? configMap.getConfirmationRequired().booleanValue() : true;
    }

    /*
     * default config
     */
    public int getConfirmationValidity() {
        return configMap.getConfirmationValidity() != null ? configMap.getConfirmationValidity().intValue()
                : MIN_DURATION;
    }

    public int getMaxSessionDuration() {
        return configMap.getMaxSessionDuration() != null ? configMap.getMaxSessionDuration().intValue()
                : MAX_SESSION_DURATION;
    }

    public InternalIdentityProviderConfig toInternalProviderConfig() {
        InternalIdentityProviderConfig ip = new InternalIdentityProviderConfig(SystemKeys.AUTHORITY_PASSWORD,
                getProvider(),
                getRealm());
        InternalIdentityProviderConfigMap cMap = new InternalIdentityProviderConfigMap();
        cMap.setCredentialsType(CredentialsType.PASSWORD);
        cMap.setMaxSessionDuration(getMaxSessionDuration());
        cMap.setScopedData(isScopedData());
        cMap.setEnableRegistration(isEnableRegistration());
        cMap.setEnableUpdate(isEnableUpdate());
        cMap.setConfirmationRequired(isConfirmationRequired());
        cMap.setConfirmationValidity(getConfirmationValidity());

        ip.setConfigMap(cMap);

        return ip;
    }

//    /*
//     * Static parser
//     */
//    public static InternalPasswordIdentityProviderConfig fromConfigurableProvider(ConfigurableIdentityProvider cp) {
//        InternalPasswordIdentityProviderConfig ip = new InternalPasswordIdentityProviderConfig(cp.getProvider(),
//                cp.getRealm());
//        // parse and use setter to properly propagate config to super
//        InternalPasswordIdentityProviderConfigMap configMap = new InternalPasswordIdentityProviderConfigMap();
//        configMap.setConfiguration(cp.getConfiguration());
//        ip.setConfigMap(configMap);
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
