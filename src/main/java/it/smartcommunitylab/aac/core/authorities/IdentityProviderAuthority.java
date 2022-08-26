package it.smartcommunitylab.aac.core.authorities;

import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.core.provider.FilterProvider;
import it.smartcommunitylab.aac.core.provider.IdentityConfigurationProvider;
import it.smartcommunitylab.aac.core.provider.IdentityProvider;
import it.smartcommunitylab.aac.core.provider.IdentityProviderConfig;

public interface IdentityProviderAuthority<I extends UserIdentity, S extends IdentityProvider<I, P>, C extends IdentityProviderConfig<P>, P extends ConfigMap>
        extends ProviderAuthority<S> {

//    /*
//     * identify
//     */
//    public String getAuthorityId();
//
//    public boolean hasProvider(String providerId);
//
//    /*
//     * Identity providers resolve identities via authentication or direct fetch
//     * 
//     * We support multiple idps from the same authority for a given realm.
//     */
//    public IdentityProvider<I> getProvider(
//            String providerId) throws NoSuchProviderException;
//
//    public List<? extends IdentityProvider<I>> getProviders(
//            String realm);
//
//    /*
//     * Manage providers
//     * 
//     * We expect idps to be either registered and usable, or removed. To update
//     * config implementations should unregister+register: we want identities (and
//     * optionally sessions) to be invalidated when the config changes.
//     */
//    public IdentityProvider<I> registerProvider(
//            ConfigurableIdentityProvider idp)
//            throws IllegalArgumentException, RegistrationException, SystemException;
//
//    public void unregisterProvider(String providerId) throws SystemException;

    /*
     * Filter provider exposes auth filters for registration in filter chain
     */
    public FilterProvider getFilterProvider();

    /*
     * Config provider exposes configuration validation and schema
     */
    public IdentityConfigurationProvider<C, P> getConfigurationProvider();
}
