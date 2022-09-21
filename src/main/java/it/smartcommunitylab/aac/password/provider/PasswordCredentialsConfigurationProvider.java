package it.smartcommunitylab.aac.password.provider;

import org.springframework.stereotype.Service;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractCredentialsConfigurationProvider;
import it.smartcommunitylab.aac.core.model.ConfigurableCredentialsService;

@Service
public class PasswordCredentialsConfigurationProvider extends
        AbstractCredentialsConfigurationProvider<PasswordCredentialsServiceConfigMap, PasswordCredentialsServiceConfig> {

    public PasswordCredentialsConfigurationProvider() {
        super(SystemKeys.AUTHORITY_PASSWORD);
        setDefaultConfigMap(new PasswordCredentialsServiceConfigMap());
    }

    @Override
    protected PasswordCredentialsServiceConfig buildConfig(ConfigurableCredentialsService cp) {
        return new PasswordCredentialsServiceConfig(cp);
    }

}