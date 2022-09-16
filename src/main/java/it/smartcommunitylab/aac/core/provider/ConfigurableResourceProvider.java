package it.smartcommunitylab.aac.core.provider;

import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.core.model.ConfigurableProvider;
import it.smartcommunitylab.aac.core.model.Resource;

/*
 * Configurable ResourceProviders are providers backed by a persisted configuration,
 * in form of a ConfigurableProvider carrying a specific ConfigMap.
 * At runtime their config is expressed via a ProviderConfig with the same ConfigMap.
 */
public interface ConfigurableResourceProvider<R extends Resource, T extends ConfigurableProvider, M extends ConfigMap, C extends ProviderConfig<M, T>>
        extends ResourceProvider<R> {

    public String getName();

    public String getDescription();

    /*
     * Config
     */

    public C getConfig();

    public T getConfigurable();

}
