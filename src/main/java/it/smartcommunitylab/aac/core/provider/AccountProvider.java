package it.smartcommunitylab.aac.core.provider;

import java.util.Collection;
import java.util.Map;

import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.model.UserAccount;

public interface AccountProvider extends ResourceProvider {

    /*
     * Fetch accounts from this provider
     * 
     * userId is globally addressable
     */

    public UserAccount getAccount(String userId) throws NoSuchUserException;

    public UserAccount getByIdentifyingAttributes(Map<String, String> attributes) throws NoSuchUserException;

    public Collection<UserAccount> listAccounts(String subject);
}
