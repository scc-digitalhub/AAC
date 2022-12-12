package it.smartcommunitylab.aac.openid.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractConfigurationProvider;
import it.smartcommunitylab.aac.core.model.ConfigurableAccountProvider;
import it.smartcommunitylab.aac.core.provider.AccountServiceConfigurationProvider;

public class OIDCAccountServiceConfigurationProvider
        extends
        AbstractConfigurationProvider<OIDCIdentityProviderConfigMap, ConfigurableAccountProvider, OIDCAccountServiceConfig>
        implements
        AccountServiceConfigurationProvider<OIDCIdentityProviderConfigMap, OIDCAccountServiceConfig> {

    public OIDCAccountServiceConfigurationProvider() {
        this(SystemKeys.AUTHORITY_OIDC);
    }

    public OIDCAccountServiceConfigurationProvider(String authority) {
        super(authority);
        setDefaultConfigMap(new OIDCIdentityProviderConfigMap());
    }

    @Override
    protected OIDCAccountServiceConfig buildConfig(ConfigurableAccountProvider cp) {
        return new OIDCAccountServiceConfig(cp);
    }

}
