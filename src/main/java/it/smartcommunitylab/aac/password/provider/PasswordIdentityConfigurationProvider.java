package it.smartcommunitylab.aac.password.provider;

import org.springframework.stereotype.Service;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.config.IdentityAuthoritiesProperties;
import it.smartcommunitylab.aac.core.base.AbstractIdentityConfigurationProvider;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;

@Service
public class PasswordIdentityConfigurationProvider extends
        AbstractIdentityConfigurationProvider<PasswordIdentityProviderConfigMap, PasswordIdentityProviderConfig> {

    public PasswordIdentityConfigurationProvider(IdentityAuthoritiesProperties authoritiesProperties) {
        super(SystemKeys.AUTHORITY_PASSWORD);
        if (authoritiesProperties != null && authoritiesProperties.getPassword() != null) {
            setDefaultConfigMap(authoritiesProperties.getPassword());
        } else {
            setDefaultConfigMap(new PasswordIdentityProviderConfigMap());
        }

    }

    @Override
    protected PasswordIdentityProviderConfig buildConfig(ConfigurableIdentityProvider cp) {
        return new PasswordIdentityProviderConfig(cp);
    }

}
