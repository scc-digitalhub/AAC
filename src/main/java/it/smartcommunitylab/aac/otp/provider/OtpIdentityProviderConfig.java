package it.smartcommunitylab.aac.otp.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.identity.base.AbstractIdentityProviderConfig;
import it.smartcommunitylab.aac.identity.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.identity.provider.IdentityProviderSettingsMap;
import java.util.Objects;

public class OtpIdentityProviderConfig extends AbstractIdentityProviderConfig<OtpIdentityProviderConfigMap> {

    public static final String RESOURCE_TYPE =
        SystemKeys.RESOURCE_PROVIDER + SystemKeys.ID_SEPARATOR + OtpIdentityProviderConfigMap.RESOURCE_TYPE;

    private static final int DEFAULT_SESSION_DURATION = 24 * 60 * 60;
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

    protected OtpIdentityProviderConfig() {
        super();
    }

    public String getRepositoryId() {
        return Objects.requireNonNullElse(configMap.getRepositoryId(), getRealm());
    }

    public boolean displayAsButton() {
        return Boolean.TRUE.equals(configMap.getDisplayAsButton());
    }

    public int getDefaultSessionDuration() {
        return Objects.requireNonNullElse(configMap.getMaxSessionDuration(), DEFAULT_SESSION_DURATION);
    }

    public int getDefaultOtpTryNumber() {
        return Objects.requireNonNullElse(configMap.getOtpTryNumber(), TRY_NUMBER);
    }

    public boolean isRequireAccountConfirmation() {
        return Objects.requireNonNullElse(configMap.getRequireAccountConfirmation(), true);
    }
}
