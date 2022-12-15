package it.smartcommunitylab.aac.password.provider;

import org.springframework.stereotype.Service;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.config.CredentialsAuthoritiesProperties;
import it.smartcommunitylab.aac.core.base.AbstractCredentialsConfigurationProvider;
import it.smartcommunitylab.aac.core.model.ConfigurableCredentialsProvider;

@Service
public class PasswordCredentialsConfigurationProvider extends
        AbstractCredentialsConfigurationProvider<PasswordIdentityProviderConfigMap, PasswordCredentialsServiceConfig> {

    public PasswordCredentialsConfigurationProvider(CredentialsAuthoritiesProperties authoritiesProperties) {
        super(SystemKeys.AUTHORITY_PASSWORD);
        if (authoritiesProperties != null && authoritiesProperties.getPassword() != null) {
            setDefaultConfigMap(authoritiesProperties.getPassword());
        } else {
            setDefaultConfigMap(new PasswordIdentityProviderConfigMap());
        }

    }

    @Override
    protected PasswordCredentialsServiceConfig buildConfig(ConfigurableCredentialsProvider cp) {
        return new PasswordCredentialsServiceConfig(cp, getConfigMap(cp.getConfiguration()));
    }

}