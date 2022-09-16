package it.smartcommunitylab.aac.core;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.core.authorities.IdentityServiceAuthority;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityService;
import it.smartcommunitylab.aac.core.service.IdentityServiceAuthorityService;
import it.smartcommunitylab.aac.core.service.IdentityServiceService;

@Service
@PreAuthorize("hasAuthority('" + Config.R_ADMIN + "')"
        + " or hasAuthority(#realm+':" + Config.R_ADMIN + "')")
public class IdentityServiceManager
        extends ConfigurableProviderManager<ConfigurableIdentityService, IdentityServiceAuthority<?, ?, ?, ?, ?>> {

    public IdentityServiceManager(IdentityServiceService identityServiceService,
            IdentityServiceAuthorityService identityServiceAuthorityService) {
        super(identityServiceService, identityServiceAuthorityService);
    }

}
