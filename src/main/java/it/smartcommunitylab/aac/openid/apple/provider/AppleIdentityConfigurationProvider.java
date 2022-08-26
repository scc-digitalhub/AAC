package it.smartcommunitylab.aac.openid.apple.provider;

import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.config.AuthoritiesProperties;
import it.smartcommunitylab.aac.core.base.AbstractIdentityConfigurationProvider;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.core.model.ConfigurableProvider;

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
    protected AppleIdentityProviderConfig buildConfig(ConfigurableProvider cp) {
        Assert.isInstanceOf(ConfigurableIdentityProvider.class, cp);
        return new AppleIdentityProviderConfig((ConfigurableIdentityProvider) cp);
    }

}
