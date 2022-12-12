package it.smartcommunitylab.aac.core.authorities;

import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityService;
import it.smartcommunitylab.aac.core.model.EditableUserAccount;
import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.core.provider.FilterProvider;
import it.smartcommunitylab.aac.core.provider.IdentityService;
import it.smartcommunitylab.aac.core.provider.IdentityServiceConfig;

public interface IdentityServiceAuthority<S extends IdentityService<I, U, E, M, C>, I extends UserIdentity, U extends UserAccount, E extends EditableUserAccount, M extends ConfigMap, C extends IdentityServiceConfig<M>>
        extends ConfigurableProviderAuthority<S, I, ConfigurableIdentityService, M, C> {

    /*
     * Filter provider exposes filters for registration in filter chain
     */
    public FilterProvider getFilterProvider();

}
