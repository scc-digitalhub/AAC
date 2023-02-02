package it.smartcommunitylab.aac.core.base;

import it.smartcommunitylab.aac.core.model.ConfigurableCredentialsProvider;
import it.smartcommunitylab.aac.core.provider.CredentialsServiceConfigurationProvider;

public abstract class AbstractCredentialsConfigurationProvider<M extends AbstractConfigMap, C extends AbstractCredentialsServiceConfig<M>>
        extends AbstractConfigurationProvider<M, ConfigurableCredentialsProvider, C>
        implements CredentialsServiceConfigurationProvider<M, C> {

    public AbstractCredentialsConfigurationProvider(String authority) {
        super(authority);
    }

    @Override
    public ConfigurableCredentialsProvider getConfigurable(C providerConfig) {
        ConfigurableCredentialsProvider cs = new ConfigurableCredentialsProvider(providerConfig.getAuthority(),
                providerConfig.getProvider(),
                providerConfig.getRealm());

        cs.setName(providerConfig.getName());
        cs.setTitleMap(providerConfig.getTitleMap());
        cs.setDescriptionMap(providerConfig.getDescriptionMap());

        cs.setRepositoryId(providerConfig.getRepositoryId());

        cs.setConfiguration(getConfiguration(providerConfig.getConfigMap()));

        // provider config are active by definition
        cs.setEnabled(true);

        return cs;
    }
}
