package it.smartcommunitylab.aac.core.provider;

import java.util.Collection;

import javax.validation.constraints.NotNull;

import it.smartcommunitylab.aac.common.NoSuchCredentialException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.model.UserCredentials;

public interface UserCredentialsService<C extends UserCredentials> {

    public Collection<C> findCredentialsByRealm(@NotNull String realm);

    public C findCredentialsById(@NotNull String repository, @NotNull String id);

    public C findCredentialsByUuid(@NotNull String uuid);

    public Collection<C> findCredentialsByAccount(@NotNull String repository, @NotNull String accountId);

    public Collection<C> findCredentialsByUser(@NotNull String repository, @NotNull String userId);

    public C addCredentials(@NotNull String repository, @NotNull String id, @NotNull C reg)
            throws RegistrationException;

    public C updateCredentials(@NotNull String repository, @NotNull String id, @NotNull C reg)
            throws NoSuchCredentialException, RegistrationException;

    public void deleteCredentials(@NotNull String repository, @NotNull String id);

    public void deleteAllCredentials(@NotNull String repository, @NotNull Collection<String> id);

    public void deleteAllCredentialsByUser(@NotNull String repository, @NotNull String userId);

    public void deleteAllCredentialsByAccount(@NotNull String repository, @NotNull String account);


}
