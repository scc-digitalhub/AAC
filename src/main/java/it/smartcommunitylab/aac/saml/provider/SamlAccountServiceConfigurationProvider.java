package it.smartcommunitylab.aac.saml.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractConfigurationProvider;
import it.smartcommunitylab.aac.core.model.ConfigurableAccountProvider;
import it.smartcommunitylab.aac.core.provider.AccountServiceConfigurationProvider;

public class SamlAccountServiceConfigurationProvider
        extends
        AbstractConfigurationProvider<SamlIdentityProviderConfigMap, ConfigurableAccountProvider, SamlAccountServiceConfig>
        implements
        AccountServiceConfigurationProvider<SamlIdentityProviderConfigMap, SamlAccountServiceConfig> {

    public SamlAccountServiceConfigurationProvider() {
        this(SystemKeys.AUTHORITY_SAML);
    }

    public SamlAccountServiceConfigurationProvider(String authority) {
        super(authority);
        setDefaultConfigMap(new SamlIdentityProviderConfigMap());
    }

    @Override
    protected SamlAccountServiceConfig buildConfig(ConfigurableAccountProvider cp) {
        return new SamlAccountServiceConfig(cp);
    }

}
