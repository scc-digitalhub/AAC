package it.smartcommunitylab.aac.core.provider;

import java.util.Collection;

import org.springframework.lang.Nullable;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityService;
import it.smartcommunitylab.aac.core.model.EditableUserAccount;
import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.core.model.UserIdentity;

/*
 * Identity service are r/w repositories for local users.
 * 
 * Accounts managed by services are eventually used by IdentityProviders
 */

public interface IdentityService<I extends UserIdentity, U extends UserAccount, E extends EditableUserAccount, M extends ConfigMap, C extends IdentityServiceConfig<M>>
        extends ConfigurableResourceProvider<I, ConfigurableIdentityService, M, C> {

    /*
     * Services
     */

    public AccountService<U, E, ?, ?> getAccountService() throws NoSuchProviderException;

    public AccountCredentialsService<?, ?, ?> getCredentialsService(String authority) throws NoSuchProviderException;

    public Collection<AccountCredentialsService<?, ?, ?>> getCredentialsServices();

//    public AttributeService<?, ?> getAttributeService();

    // TODO evaluate subjectResolver moved here, we manage accounts

    /*
     * Fetch identities from this provider
     */

    public I findIdentity(String userId, String identityId);

    public I getIdentity(String userId, String identityId) throws NoSuchUserException;

    public I getIdentity(String userId, String identityId, boolean loadCredentials) throws NoSuchUserException;

    public Collection<I> listIdentities(String userId);

    /*
     * Manage identities from this provider
     * 
     * userId is globally addressable
     */

    public I createIdentity(
            @Nullable String userId, UserIdentity identity) throws NoSuchUserException, RegistrationException;

    public I registerIdentity(
            @Nullable String userId, UserIdentity identity)
            throws NoSuchUserException, RegistrationException;

    public I updateIdentity(
            String userId,
            String identityId, UserIdentity identity) throws NoSuchUserException, RegistrationException;

    public void deleteIdentity(
            String userId,
            String identityId) throws NoSuchUserException, RegistrationException;

    public void deleteIdentities(String userId);

    /*
     * Registration
     */

    public String getRegistrationUrl();

//    public RegistrationProvider getRegistrationProvider();

    default public String getType() {
        return SystemKeys.RESOURCE_IDENTITY;
    }
}
