package it.smartcommunitylab.aac.internal.provider;

import org.springframework.stereotype.Service;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.config.IdentityAuthoritiesProperties;
import it.smartcommunitylab.aac.core.base.AbstractConfigurationProvider;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityService;
import it.smartcommunitylab.aac.core.provider.IdentityServiceConfigurationProvider;

@Service
public class InternalIdentityServiceConfigurationProvider
        extends
        AbstractConfigurationProvider<InternalIdentityProviderConfigMap, ConfigurableIdentityService, InternalIdentityServiceConfig>
        implements
        IdentityServiceConfigurationProvider<InternalIdentityProviderConfigMap, InternalIdentityServiceConfig> {

    public InternalIdentityServiceConfigurationProvider(IdentityAuthoritiesProperties authoritiesProperties) {
        super(SystemKeys.AUTHORITY_INTERNAL);
        if (authoritiesProperties != null && authoritiesProperties.getInternal() != null) {
            setDefaultConfigMap(authoritiesProperties.getInternal());
        } else {
            setDefaultConfigMap(new InternalIdentityProviderConfigMap());
        }
    }

    @Override
    protected InternalIdentityServiceConfig buildConfig(ConfigurableIdentityService cp) {
        return new InternalIdentityServiceConfig(cp);
    }

}
