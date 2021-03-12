package it.smartcommunitylab.aac.core.authorities;

import java.util.List;

import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.base.ConfigurableProvider;
import it.smartcommunitylab.aac.core.provider.IdentityProvider;

public interface IdentityAuthority {

    /*
     * identify
     */
    public String getAuthorityId();

    // identity

    public IdentityProvider getIdentityProvider(String providerId);

    public List<IdentityProvider> getIdentityProviders(String realm);

    /*
     * we also expect auth provider to be able to infer *provider from userId
     * implementations should build ids predictably via composition
     * 
     * also *providers should return the same id for the same user!
     */
    public IdentityProvider getUserIdentityProvider(String userId);

    /*
     * Manage providers
     * 
     * we expect idp to be registered and usable, or removed. To update config
     * implementations should unregister+register, we want identities and sessions
     * to be invalidated if config changes
     */
    public void registerIdentityProvider(ConfigurableProvider idp) throws IllegalArgumentException, SystemException;

    public void unregisterIdentityProvider(String providerId) throws SystemException;

}
