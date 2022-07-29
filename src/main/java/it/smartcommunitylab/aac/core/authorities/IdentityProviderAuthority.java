package it.smartcommunitylab.aac.core.authorities;

import java.util.List;

import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.core.provider.IdentityProvider;

public interface IdentityProviderAuthority<I extends UserIdentity> {

    /*
     * identify
     */
    public String getAuthorityId();

    public boolean hasProvider(String providerId);

    /*
     * Identity providers resolve identities via authentication or direct fetch
     * 
     * We support multiple idps from the same authority for a given realm.
     */
    public IdentityProvider<I> getProvider(
            String providerId) throws NoSuchProviderException;

    public List<? extends IdentityProvider<I>> getProviders(
            String realm);

    /*
     * Manage providers
     * 
     * We expect idps to be either registered and usable, or removed. To update
     * config implementations should unregister+register: we want identities (and
     * optionally sessions) to be invalidated when the config changes.
     */
    public IdentityProvider<I> registerProvider(
            ConfigurableIdentityProvider idp)
            throws IllegalArgumentException, RegistrationException, SystemException;

    public void unregisterProvider(String providerId) throws SystemException;

}
