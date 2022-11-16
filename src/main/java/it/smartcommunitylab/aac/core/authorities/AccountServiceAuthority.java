package it.smartcommunitylab.aac.core.authorities;

import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.core.model.ConfigurableAccountProvider;
import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.core.provider.AccountService;
import it.smartcommunitylab.aac.core.provider.AccountServiceConfig;

public interface AccountServiceAuthority<S extends AccountService<U, M, C>, U extends UserAccount, M extends ConfigMap, C extends AccountServiceConfig<M>>
        extends ConfigurableProviderAuthority<S, U, ConfigurableAccountProvider, M, C> {

}
