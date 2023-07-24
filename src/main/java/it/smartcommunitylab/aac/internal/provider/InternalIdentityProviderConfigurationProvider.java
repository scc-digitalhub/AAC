package it.smartcommunitylab.aac.internal.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.config.IdentityAuthoritiesProperties;
import it.smartcommunitylab.aac.core.base.AbstractIdentityConfigurationProvider;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;
import org.springframework.stereotype.Service;

@Service
public class InternalIdentityProviderConfigurationProvider
    extends AbstractIdentityConfigurationProvider<InternalIdentityProviderConfigMap, InternalIdentityProviderConfig> {

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
