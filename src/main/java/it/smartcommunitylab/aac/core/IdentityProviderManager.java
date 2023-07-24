package it.smartcommunitylab.aac.core;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.core.authorities.IdentityProviderAuthority;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.core.service.IdentityProviderService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@PreAuthorize("hasAuthority('" + Config.R_ADMIN + "')" + " or hasAuthority(#realm+':" + Config.R_ADMIN + "')")
public class IdentityProviderManager
    extends ConfigurableProviderManager<ConfigurableIdentityProvider, IdentityProviderAuthority<?, ?, ?, ?>> {

    public IdentityProviderManager(IdentityProviderService identityProviderService) {
        super(identityProviderService);
    }
}
