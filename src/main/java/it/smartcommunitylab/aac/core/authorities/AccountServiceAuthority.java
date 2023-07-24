package it.smartcommunitylab.aac.core.authorities;

import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.core.model.EditableUserAccount;
import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.core.provider.AccountService;
import it.smartcommunitylab.aac.core.provider.AccountServiceConfig;

public interface AccountServiceAuthority<
    S extends AccountService<U, E, M, C>,
    U extends UserAccount,
    E extends EditableUserAccount,
    M extends ConfigMap,
    C extends AccountServiceConfig<M>
>
    extends ProviderAuthority<S, U> {}
