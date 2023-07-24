package it.smartcommunitylab.aac.core.authorities;

import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.core.model.ConfigurableProvider;
import it.smartcommunitylab.aac.core.model.Resource;
import it.smartcommunitylab.aac.core.provider.ConfigurableResourceProvider;
import it.smartcommunitylab.aac.core.provider.ConfigurationProvider;
import it.smartcommunitylab.aac.core.provider.ProviderConfig;

/*
 * Provider authorities handle (configurable) resource providers by managing registrations and configuration
 */
public interface ConfigurableProviderAuthority<
    S extends ConfigurableResourceProvider<R, T, M, C>,
    R extends Resource,
    T extends ConfigurableProvider,
    M extends ConfigMap,
    C extends ProviderConfig<M>
>
    extends ProviderAuthority<S, R> {
    /*
     * Registration
     *
     * TODO remove and make interface RO
     */

    public C registerProvider(ConfigurableProvider config)
        throws IllegalArgumentException, RegistrationException, SystemException;

    public void unregisterProvider(String providerId) throws SystemException;

    /*
     * Config provider exposes configuration translation, validation and schema
     */
    public ConfigurationProvider<M, T, C> getConfigurationProvider();
}
