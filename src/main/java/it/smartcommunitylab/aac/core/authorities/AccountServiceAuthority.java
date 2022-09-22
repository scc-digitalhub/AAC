package it.smartcommunitylab.aac.core.authorities;

import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.core.model.ConfigurableAccountService;
import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.core.provider.FilterProvider;
import it.smartcommunitylab.aac.core.provider.AccountService;
import it.smartcommunitylab.aac.core.provider.AccountServiceConfig;

public interface AccountServiceAuthority<S extends AccountService<U, M, C>, U extends UserAccount, M extends ConfigMap, C extends AccountServiceConfig<M>>
        extends ProviderAuthority<S, U, ConfigurableAccountService, M, C> {

    /*
     * Filter provider exposes auth filters for registration in filter chain
     */
    public FilterProvider getFilterProvider();

}
