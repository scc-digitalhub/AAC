package it.smartcommunitylab.aac.otp.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.identity.base.AbstractIdentityProviderConfig;
import it.smartcommunitylab.aac.identity.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.identity.provider.IdentityProviderSettingsMap;

public class OtpIdentityProviderConfig extends AbstractIdentityProviderConfig<OtpIdentityProviderConfigMap> {

    public static final String RESOURCE_TYPE =
        SystemKeys.RESOURCE_PROVIDER + SystemKeys.ID_SEPARATOR + OtpIdentityProviderConfigMap.RESOURCE_TYPE;

    private static final int DEFAULT_SESSION_DURATION = 24 * 60 * 60; // 24h
    private static final int TRY_NUMBER = 3;

    public OtpIdentityProviderConfig(String provider, String realm) {
        super(
            SystemKeys.AUTHORITY_OTP,
            provider,
            realm,
            new IdentityProviderSettingsMap(),
            new OtpIdentityProviderConfigMap()
        );
    }

    public OtpIdentityProviderConfig(
        ConfigurableIdentityProvider cp,
        IdentityProviderSettingsMap settingsMap,
        OtpIdentityProviderConfigMap configMap
    ) {
        super(cp, settingsMap, configMap);
    }

    /**
     * Private constructor for JPA and other serialization tools.
     *
     * We need to implement this to enable deserialization of resources via
     * reflection
     */

    @SuppressWarnings("unused")
    private OtpIdentityProviderConfig() {
        super();
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

    public int getDefaultSessionDuration() {
        return configMap.getMaxSessionDuration() != null
            ? configMap.getMaxSessionDuration().intValue()
            : DEFAULT_SESSION_DURATION;
    }

    public int getDefaultOtpTryNumber() {
        return configMap.getOtpTryNumber() != null ? configMap.getOtpTryNumber().intValue() : TRY_NUMBER;
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
