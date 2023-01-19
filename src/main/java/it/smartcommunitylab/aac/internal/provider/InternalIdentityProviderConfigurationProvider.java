package it.smartcommunitylab.aac.internal.provider;

import org.springframework.stereotype.Service;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.config.IdentityAuthoritiesProperties;
import it.smartcommunitylab.aac.core.base.AbstractIdentityConfigurationProvider;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;

@Service
public class InternalIdentityProviderConfigurationProvider
        extends
        AbstractIdentityConfigurationProvider<InternalIdentityProviderConfigMap, InternalIdentityProviderConfig> {

    public InternalIdentityProviderConfigurationProvider(IdentityAuthoritiesProperties authoritiesProperties) {
        super(SystemKeys.AUTHORITY_INTERNAL);
        if (authoritiesProperties != null && authoritiesProperties.getInternal() != null) {
            setDefaultConfigMap(authoritiesProperties.getInternal());
        } else {
            setDefaultConfigMap(new InternalIdentityProviderConfigMap());
        }
    }

    @Override
    protected InternalIdentityProviderConfig buildConfig(ConfigurableIdentityProvider cp) {
        // build configMap and then config
        return new InternalIdentityProviderConfig(cp, getConfigMap(cp.getConfiguration()));
    }

}
