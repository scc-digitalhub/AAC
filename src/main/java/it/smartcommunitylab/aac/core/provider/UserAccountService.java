package it.smartcommunitylab.aac.core.provider;

import java.util.List;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.model.UserAccount;

public interface UserAccountService<U extends UserAccount> {

    public U findAccountById(String repository, String id);

    public U findAccountByUuid(String repository, String uuid);

    public List<U> findAccountByUsername(String repository, String username);

    public List<U> findAccountByEmail(String repository, String email);

    public List<U> findAccountByUser(String repository, String userId);

    public U addAccount(String repository, String id, U reg) throws RegistrationException;

    public U updateAccount(String repository, String id, U reg) throws NoSuchUserException, RegistrationException;

    public void deleteAccount(String repository, String id);
}
