package it.smartcommunitylab.aac.internal.service;

import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;

public interface InternalUserConfirmKeyService {
    public InternalUserAccount findAccountByConfirmationKey(String repository, String key);

    public InternalUserAccount confirmAccount(String repository, String username, String key)
        throws NoSuchUserException, RegistrationException;
}
