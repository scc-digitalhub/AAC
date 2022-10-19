package it.smartcommunitylab.aac.core.provider;

import java.util.List;

import javax.validation.constraints.NotNull;

import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.model.UserAccount;

public interface UserAccountService<U extends UserAccount> {

    public List<U> findAccountByRealm(@NotNull String realm);

    public U findAccountById(@NotNull String repository, @NotNull String id);

    public U findAccountByUuid(@NotNull String repository, @NotNull String uuid);

    public List<U> findAccountByUsername(@NotNull String repository, @NotNull String username);

    public List<U> findAccountByEmail(@NotNull String repository, @NotNull String email);

    public List<U> findAccountByUser(@NotNull String repository, @NotNull String userId);

    public U addAccount(@NotNull String repository, @NotNull String id, @NotNull U reg) throws RegistrationException;

    public U updateAccount(@NotNull String repository, @NotNull String id, @NotNull U reg)
            throws NoSuchUserException, RegistrationException;

    public void deleteAccount(@NotNull String repository, @NotNull String id);
}
