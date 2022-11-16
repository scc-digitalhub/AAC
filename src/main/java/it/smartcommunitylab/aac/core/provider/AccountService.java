package it.smartcommunitylab.aac.core.provider;

import org.springframework.lang.Nullable;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.core.model.ConfigurableAccountProvider;
import it.smartcommunitylab.aac.core.model.UserAccount;

public interface AccountService<U extends UserAccount, M extends ConfigMap, C extends AccountServiceConfig<M>>
        extends ConfigurableResourceProvider<U, ConfigurableAccountProvider, M, C>, AccountProvider<U> {

    /*
     * Manage accounts from this provider
     * 
     * accountId is local to provider
     */
    public U registerAccount(@Nullable String userId, UserAccount account)
            throws NoSuchUserException, RegistrationException;

    public U createAccount(@Nullable String userId, @Nullable String accountId, UserAccount account)
            throws NoSuchUserException, RegistrationException;

    public U updateAccount(String userId, String accountId, UserAccount account)
            throws NoSuchUserException, RegistrationException;

    /*
     * Account confirmation
     * 
     * verify will trigger account verification via provider
     * 
     * confirm/unconfirm directly change status
     */

    public U verifyAccount(String accountId) throws NoSuchUserException, RegistrationException;

    public U confirmAccount(String accountId) throws NoSuchUserException, RegistrationException;

    public U unconfirmAccount(String accountId) throws NoSuchUserException, RegistrationException;

    /*
     * Registration
     */

    public String getRegistrationUrl();

//    public RegistrationProvider getRegistrationProvider();

    default public String getType() {
        return SystemKeys.RESOURCE_ACCOUNT;
    }
}
