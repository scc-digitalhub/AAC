package it.smartcommunitylab.aac.openid.apple.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractConfigurationProvider;
import it.smartcommunitylab.aac.core.model.ConfigurableAccountProvider;
import it.smartcommunitylab.aac.core.provider.AccountServiceConfigurationProvider;

public class AppleAccountServiceConfigurationProvider
        extends
        AbstractConfigurationProvider<AppleIdentityProviderConfigMap, ConfigurableAccountProvider, AppleAccountServiceConfig>
        implements
        AccountServiceConfigurationProvider<AppleIdentityProviderConfigMap, AppleAccountServiceConfig> {

    public AppleAccountServiceConfigurationProvider() {
        super(SystemKeys.AUTHORITY_APPLE);
        setDefaultConfigMap(new AppleIdentityProviderConfigMap());
    }

    @Override
    protected AppleAccountServiceConfig buildConfig(ConfigurableAccountProvider cp) {
        return new AppleAccountServiceConfig(cp);
    }

}
