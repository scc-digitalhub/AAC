package it.smartcommunitylab.aac.otp.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.credentials.base.AbstractCredentialsServiceConfig;
import it.smartcommunitylab.aac.credentials.model.ConfigurableCredentialsProvider;
import it.smartcommunitylab.aac.credentials.provider.CredentialsServiceSettingsMap;
import java.util.Objects;

public class OtpCredentialsServiceConfig extends AbstractCredentialsServiceConfig<OtpIdentityProviderConfigMap> {

    public static final String RESOURCE_TYPE =
        SystemKeys.RESOURCE_PROVIDER +
        SystemKeys.ID_SEPARATOR +
        SystemKeys.RESOURCE_CONFIG +
        SystemKeys.ID_SEPARATOR +
        SystemKeys.RESOURCE_CREDENTIALS_SERVICE +
        SystemKeys.ID_SEPARATOR +
        SystemKeys.AUTHORITY_OTP;

    public OtpCredentialsServiceConfig(String provider, String realm) {
        super(
            SystemKeys.AUTHORITY_OTP,
            provider,
            realm,
            new CredentialsServiceSettingsMap(),
            new OtpIdentityProviderConfigMap()
        );
    }

    public OtpCredentialsServiceConfig(
        ConfigurableCredentialsProvider cp,
        CredentialsServiceSettingsMap settingsMap,
        OtpIdentityProviderConfigMap configMap
    ) {
        super(cp, settingsMap, configMap);
    }

    protected OtpCredentialsServiceConfig() {
        super();
    }

    @Override
    public String getRepositoryId() {
        return Objects.requireNonNullElse(configMap.getRepositoryId(), getRealm());
    }

    public boolean isRequireAccountConfirmation() {
        return Objects.requireNonNullElse(configMap.getRequireAccountConfirmation(), true);
    }
}
