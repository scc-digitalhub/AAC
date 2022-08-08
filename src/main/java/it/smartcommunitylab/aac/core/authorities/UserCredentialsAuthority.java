package it.smartcommunitylab.aac.core.authorities;

import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.model.ConfigurableProvider;
import it.smartcommunitylab.aac.core.model.UserCredentials;
import it.smartcommunitylab.aac.core.provider.UserCredentialsService;

public interface UserCredentialsAuthority {

    /*
     * identify
     */
    public String getAuthorityId();

    public boolean hasService(String realm);

    /*
     * User Credentials services handle credentials associated to local users as
     * read/write
     * 
     * Credentials are eventually used by IdentityProviders to perform local
     * authentication
     */
    public UserCredentialsService<? extends UserCredentials> getService(
            String realm);

    /*
     * Manage providers
     * 
     * Each authority can expose a single provider per realm. In order to change
     * configuration unregister to unload and then register the new config.
     * 
     */
    public UserCredentialsService<? extends UserCredentials> registerService(
            String realm, ConfigurableProvider config)
            throws IllegalArgumentException, RegistrationException, SystemException;

    public void unregisterService(String realm) throws SystemException;

}
