package it.smartcommunitylab.aac.internal.provider;

import org.springframework.stereotype.Service;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.config.AuthoritiesProperties;
import it.smartcommunitylab.aac.core.base.AbstractConfigurationProvider;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityService;
import it.smartcommunitylab.aac.core.provider.IdentityServiceConfigurationProvider;

@Service
public class InternalIdentityServiceConfigurationProvider
        extends
        AbstractConfigurationProvider<InternalIdentityServiceConfigMap, ConfigurableIdentityService, InternalIdentityServiceConfig>
        implements
        IdentityServiceConfigurationProvider<InternalIdentityServiceConfigMap, InternalIdentityServiceConfig> {

    public InternalIdentityServiceConfigurationProvider(AuthoritiesProperties authoritiesProperties) {
        super(SystemKeys.AUTHORITY_INTERNAL);
        if (authoritiesProperties != null && authoritiesProperties.getInternal() != null) {
            setDefaultConfigMap(authoritiesProperties.getInternal());
        } else {
            setDefaultConfigMap(new InternalIdentityServiceConfigMap());
        }
    }

    @Override
    protected InternalIdentityServiceConfig buildConfig(ConfigurableIdentityService cp) {
        return new InternalIdentityServiceConfig(cp);
    }

}
