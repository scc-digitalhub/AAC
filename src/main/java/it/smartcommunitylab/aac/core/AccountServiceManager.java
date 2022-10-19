package it.smartcommunitylab.aac.core;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.core.authorities.AccountServiceAuthority;
import it.smartcommunitylab.aac.core.model.ConfigurableAccountService;
import it.smartcommunitylab.aac.core.service.AccountServiceService;

@Service
@PreAuthorize("hasAuthority('" + Config.R_ADMIN + "')"
        + " or hasAuthority(#realm+':" + Config.R_ADMIN + "')")
public class AccountServiceManager
        extends ConfigurableProviderManager<ConfigurableAccountService, AccountServiceAuthority<?, ?, ?, ?>> {

    public AccountServiceManager(AccountServiceService accountServiceService) {
        super(accountServiceService);
    }

}
