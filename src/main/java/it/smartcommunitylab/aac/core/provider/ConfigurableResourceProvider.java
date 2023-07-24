package it.smartcommunitylab.aac.core.provider;

import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.core.model.ConfigurableProvider;
import it.smartcommunitylab.aac.core.model.Resource;
import java.util.Locale;

/*
 * Configurable ResourceProviders are providers backed by a persisted configuration,
 * in form of a ConfigurableProvider carrying a specific ConfigMap.
 * At runtime their config is expressed via a ProviderConfig with the same ConfigMap.
 */
public interface ConfigurableResourceProvider<
    R extends Resource, T extends ConfigurableProvider, M extends ConfigMap, C extends ProviderConfig<M>
>
    extends ResourceProvider<R> {
    public String getName();

    public String getTitle(Locale locale);

    public String getDescription(Locale locale);

    /*
     * Config
     */

    public C getConfig();
}
