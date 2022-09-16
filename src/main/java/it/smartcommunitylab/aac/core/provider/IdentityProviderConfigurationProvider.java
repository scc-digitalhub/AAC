package it.smartcommunitylab.aac.core.provider;

import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;

public interface IdentityProviderConfigurationProvider<M extends ConfigMap, C extends IdentityProviderConfig<M>>
        extends ConfigurationProvider<M, ConfigurableIdentityProvider, C> {

}
