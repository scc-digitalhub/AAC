package it.smartcommunitylab.aac.core.authorities;

import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.core.provider.FilterProvider;
import it.smartcommunitylab.aac.core.provider.IdentityProvider;
import it.smartcommunitylab.aac.core.provider.IdentityProviderConfig;

public interface IdentityProviderAuthority<
    S extends IdentityProvider<I, ?, ?, M, C>,
    I extends UserIdentity,
    M extends ConfigMap,
    C extends IdentityProviderConfig<M>
>
    extends ConfigurableProviderAuthority<S, I, ConfigurableIdentityProvider, M, C> {
    /*
     * Filter provider exposes auth filters for registration in filter chain
     */
    public FilterProvider getFilterProvider();
    //    /*
    //     * Config provider exposes configuration validation and schema
    //     */
    //    public IdentityConfigurationProvider<C, P> getConfigurationProvider();

}
