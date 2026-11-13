package it.smartcommunitylab.aac.otp.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.config.IdentityAuthoritiesProperties;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.identity.base.AbstractIdentityConfigurationProvider;
import it.smartcommunitylab.aac.identity.model.ConfigurableIdentityProvider;
import org.springframework.stereotype.Service;

@Service
public class OtpIdentityConfigurationProvider
    extends AbstractIdentityConfigurationProvider<OtpIdentityProviderConfig, OtpIdentityProviderConfigMap> {

    public OtpIdentityConfigurationProvider(
        ProviderConfigRepository<OtpIdentityProviderConfig> registrationRepository,
        IdentityAuthoritiesProperties authoritiesProperties
    ) {
        super(SystemKeys.AUTHORITY_OTP, registrationRepository);
        if (authoritiesProperties != null && authoritiesProperties.getSettings() != null) {
            setDefaultSettingsMap(authoritiesProperties.getSettings());
        }
        setDefaultConfigMap(new OtpIdentityProviderConfigMap());
    }

    @Override
    protected OtpIdentityProviderConfig buildConfig(ConfigurableIdentityProvider cp) {
        return new OtpIdentityProviderConfig(cp, getSettingsMap(cp.getSettings()), getConfigMap(cp.getConfiguration()));
    }
}
