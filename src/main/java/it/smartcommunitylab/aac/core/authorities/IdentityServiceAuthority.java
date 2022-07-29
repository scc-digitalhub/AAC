package it.smartcommunitylab.aac.core.authorities;

import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.model.ConfigurableProvider;
import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.core.provider.IdentityService;

public interface IdentityServiceAuthority {

    /*
     * identify
     */
    public String getAuthorityId();

    public boolean hasService(String realm);

    /*
     * Identity services
     * 
     * Manage identities read-write. An ids support identities (and account)
     * registration, update and delete and serves as the user repository for
     * identity providers. We don't enforce a direct relationship between ids and
     * idps.
     * 
     * Any given authority can expose only a single provider per realm, which acts
     * as authoritative
     */
    public IdentityService<? extends UserIdentity, ? extends UserAccount> getService(
            String realm);

    /*
     * Manage providers
     * 
     * In order to change a configuration unregister to unload and then register the
     * new config to instantiate the service.
     * 
     */
    public IdentityService<? extends UserIdentity, ? extends UserAccount> registerService(
            String realm, ConfigurableProvider idp)
            throws IllegalArgumentException, RegistrationException, SystemException;

    public void unregisterService(String realm) throws SystemException;

}
