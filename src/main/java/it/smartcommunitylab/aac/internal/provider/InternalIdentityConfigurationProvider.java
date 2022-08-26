package it.smartcommunitylab.aac.internal.provider;

import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.config.AuthoritiesProperties;
import it.smartcommunitylab.aac.core.base.AbstractIdentityConfigurationProvider;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.core.model.ConfigurableProvider;

@Service
public class InternalIdentityConfigurationProvider
        extends
        AbstractIdentityConfigurationProvider<InternalIdentityProviderConfig, InternalIdentityProviderConfigMap> {

    public InternalIdentityConfigurationProvider(AuthoritiesProperties authoritiesProperties) {
        super(SystemKeys.AUTHORITY_INTERNAL);
        if (authoritiesProperties != null && authoritiesProperties.getInternal() != null) {
            setDefaultConfigMap(authoritiesProperties.getInternal());
        } else {
            setDefaultConfigMap(new InternalIdentityProviderConfigMap());
        }
    }

    @Override
    protected InternalIdentityProviderConfig buildConfig(ConfigurableProvider cp) {
        Assert.isInstanceOf(ConfigurableIdentityProvider.class, cp);
        return new InternalIdentityProviderConfig((ConfigurableIdentityProvider) cp);
    }

}
