package it.smartcommunitylab.aac.openid.apple.provider;

import org.springframework.stereotype.Service;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.config.AuthoritiesProperties;
import it.smartcommunitylab.aac.core.base.AbstractIdentityConfigurationProvider;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;

@Service
public class AppleIdentityConfigurationProvider
        extends AbstractIdentityConfigurationProvider<AppleIdentityProviderConfig, AppleIdentityProviderConfigMap> {

    public AppleIdentityConfigurationProvider(AuthoritiesProperties authoritiesProperties) {
        super(SystemKeys.AUTHORITY_APPLE);
        if (authoritiesProperties != null && authoritiesProperties.getApple() != null) {
            setDefaultConfigMap(authoritiesProperties.getApple());
        } else {
            setDefaultConfigMap(new AppleIdentityProviderConfigMap());
        }
    }

    @Override
    protected AppleIdentityProviderConfig buildConfig(ConfigurableIdentityProvider cp) {
        return new AppleIdentityProviderConfig(cp);
    }

}
