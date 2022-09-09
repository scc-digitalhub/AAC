package it.smartcommunitylab.aac.password.provider;

import org.springframework.stereotype.Service;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.config.AuthoritiesProperties;
import it.smartcommunitylab.aac.core.base.AbstractIdentityConfigurationProvider;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;

@Service
public class InternalPasswordIdentityConfigurationProvider extends
        AbstractIdentityConfigurationProvider<InternalPasswordIdentityProviderConfig, InternalPasswordIdentityProviderConfigMap> {

    public InternalPasswordIdentityConfigurationProvider(AuthoritiesProperties authoritiesProperties) {
        super(SystemKeys.AUTHORITY_PASSWORD);
        if (authoritiesProperties != null && authoritiesProperties.getPassword() != null) {
            setDefaultConfigMap(authoritiesProperties.getPassword());
        } else {
            setDefaultConfigMap(new InternalPasswordIdentityProviderConfigMap());
        }

    }

    @Override
    protected InternalPasswordIdentityProviderConfig buildConfig(ConfigurableIdentityProvider cp) {
        return new InternalPasswordIdentityProviderConfig(cp);
    }

}
