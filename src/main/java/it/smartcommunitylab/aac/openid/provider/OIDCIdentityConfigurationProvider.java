package it.smartcommunitylab.aac.openid.provider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.config.AuthoritiesProperties;
import it.smartcommunitylab.aac.core.base.AbstractIdentityConfigurationProvider;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.core.model.ConfigurableProvider;

@Service
public class OIDCIdentityConfigurationProvider extends
        AbstractIdentityConfigurationProvider<OIDCIdentityProviderConfig, OIDCIdentityProviderConfigMap> {

    @Autowired
    public OIDCIdentityConfigurationProvider(AuthoritiesProperties authoritiesProperties) {
        this(SystemKeys.AUTHORITY_OIDC, new OIDCIdentityProviderConfigMap());
        if (authoritiesProperties != null && authoritiesProperties.getOidc() != null) {
            setDefaultConfigMap(authoritiesProperties.getOidc());
        }
    }

    public OIDCIdentityConfigurationProvider(String authority, OIDCIdentityProviderConfigMap configMap) {
        super(authority);
        setDefaultConfigMap(configMap);
    }

    @Override
    protected OIDCIdentityProviderConfig buildConfig(ConfigurableProvider cp) {
        Assert.isInstanceOf(ConfigurableIdentityProvider.class, cp);
        return new OIDCIdentityProviderConfig((ConfigurableIdentityProvider) cp);
    }

}
