package it.smartcommunitylab.aac.core.authorities;

import java.util.List;

import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.base.ConfigurableProvider;
import it.smartcommunitylab.aac.core.provider.IdentityProvider;
import it.smartcommunitylab.aac.core.provider.IdentityService;

public interface IdentityAuthority {

    /*
     * identify
     */
    public String getAuthorityId();

    /*
     * identity providers
     * 
     * Resolve identities via authentication or direct fetch
     */

    public IdentityProvider getIdentityProvider(String providerId);

    public List<IdentityProvider> getIdentityProviders(String realm);

    /*
     * we also expect auth provider to be able to infer *provider from userId
     * implementations should build ids predictably via composition
     * 
     * also *providers should return the same id for the same user!
     */
    public String getUserProvider(String userId);

    /*
     * Manage providers
     * 
     * we expect idp to be registered and usable, or removed. To update config
     * implementations should unregister+register, we want identities and sessions
     * to be invalidated if config changes
     */
    public IdentityProvider registerIdentityProvider(ConfigurableProvider idp)
            throws IllegalArgumentException, RegistrationException, SystemException;

    public void unregisterIdentityProvider(String realm, String providerId) throws SystemException;

    /*
     * Identity services
     * 
     * Manage identities read-write. Implementations may choose to return null when
     * identities are not manageable, but at minimum they should return a service
     * with delete. When not provided, identities will be immutable.
     */
    public IdentityService getIdentityService(String providerId);

    public List<IdentityService> getIdentityServices(String realm);

}
