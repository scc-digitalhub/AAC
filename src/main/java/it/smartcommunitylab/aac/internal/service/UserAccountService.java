package it.smartcommunitylab.aac.internal.service;

import java.util.List;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;

public interface UserAccountService {

    public InternalUserAccount findAccountByUsername(String provider, String username);

    public InternalUserAccount findAccountByConfirmationKey(String provider, String key);

    public InternalUserAccount findAccountByUuid(String provider, String uuid);

    public List<InternalUserAccount> findAccountByEmail(String provider, String email);

    public List<InternalUserAccount> findByUser(String provider, String userId);

    public InternalUserAccount addAccount(String provider, String username,
            InternalUserAccount reg) throws RegistrationException;

    public InternalUserAccount updateAccount(String provider, String username,
            InternalUserAccount reg) throws NoSuchUserException, RegistrationException;

    public void deleteAccount(String provider, String username);
}
