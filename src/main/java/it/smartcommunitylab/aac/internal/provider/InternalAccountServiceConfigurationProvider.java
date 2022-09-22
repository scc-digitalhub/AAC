package it.smartcommunitylab.aac.internal.provider;

import org.springframework.stereotype.Service;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.config.AccountAuthoritiesProperties;
import it.smartcommunitylab.aac.core.base.AbstractConfigurationProvider;
import it.smartcommunitylab.aac.core.model.ConfigurableAccountService;
import it.smartcommunitylab.aac.core.provider.AccountServiceConfigurationProvider;

@Service
public class InternalAccountServiceConfigurationProvider
        extends
        AbstractConfigurationProvider<InternalAccountServiceConfigMap, ConfigurableAccountService, InternalAccountServiceConfig>
        implements
        AccountServiceConfigurationProvider<InternalAccountServiceConfigMap, InternalAccountServiceConfig> {

    public InternalAccountServiceConfigurationProvider(AccountAuthoritiesProperties authoritiesProperties) {
        super(SystemKeys.AUTHORITY_INTERNAL);
        if (authoritiesProperties != null && authoritiesProperties.getInternal() != null) {
            setDefaultConfigMap(authoritiesProperties.getInternal());
        } else {
            setDefaultConfigMap(new InternalAccountServiceConfigMap());
        }
    }

    @Override
    protected InternalAccountServiceConfig buildConfig(ConfigurableAccountService cp) {
        return new InternalAccountServiceConfig(cp);
    }

}
