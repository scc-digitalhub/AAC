package it.smartcommunitylab.aac.scope.base;

import it.smartcommunitylab.aac.core.base.AbstractConfigMap;
import it.smartcommunitylab.aac.core.base.AbstractConfigurationProvider;
import it.smartcommunitylab.aac.scope.model.ApiResourceProviderConfigurationProvider;
import it.smartcommunitylab.aac.scope.model.ConfigurableApiResourceProvider;

public abstract class AbstractApiResourceConfigurationProvider<A extends AbstractApiResource<?>, M extends AbstractConfigMap, C extends AbstractApiResourceProviderConfig<A, M>>
        extends AbstractConfigurationProvider<M, ConfigurableApiResourceProvider, C>
        implements ApiResourceProviderConfigurationProvider<A, M, C> {

    public AbstractApiResourceConfigurationProvider(String authority) {
        super(authority);
    }

    @Override
    public ConfigurableApiResourceProvider getConfigurable(C providerConfig) {
        ConfigurableApiResourceProvider cs = new ConfigurableApiResourceProvider(providerConfig.getAuthority(),
                providerConfig.getProvider(),
                providerConfig.getRealm());

        cs.setName(providerConfig.getName());
        cs.setTitleMap(providerConfig.getTitleMap());
        cs.setDescriptionMap(providerConfig.getDescriptionMap());

        String resourceId = providerConfig.getResource() != null ? providerConfig.getResource().getResourceId()
                : null;
        cs.setResource(resourceId);

        cs.setConfiguration(getConfiguration(providerConfig.getConfigMap()));

        // provider config are active by definition
        cs.setEnabled(true);

        return cs;
    }
}
