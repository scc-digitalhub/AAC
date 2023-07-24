package it.smartcommunitylab.aac.openid.apple.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.config.IdentityAuthoritiesProperties;
import it.smartcommunitylab.aac.core.base.AbstractIdentityConfigurationProvider;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;
import org.springframework.stereotype.Service;

@Service
public class AppleIdentityConfigurationProvider
    extends AbstractIdentityConfigurationProvider<AppleIdentityProviderConfigMap, AppleIdentityProviderConfig> {

    public AppleIdentityConfigurationProvider(IdentityAuthoritiesProperties authoritiesProperties) {
        super(SystemKeys.AUTHORITY_APPLE);
        if (authoritiesProperties != null && authoritiesProperties.getApple() != null) {
            setDefaultConfigMap(authoritiesProperties.getApple());
        } else {
            setDefaultConfigMap(new AppleIdentityProviderConfigMap());
        }
    }

    @Override
    protected AppleIdentityProviderConfig buildConfig(ConfigurableIdentityProvider cp) {
        return new AppleIdentityProviderConfig(cp, getConfigMap(cp.getConfiguration()));
    }
}
