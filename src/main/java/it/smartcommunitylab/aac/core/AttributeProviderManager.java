package it.smartcommunitylab.aac.core;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.core.authorities.AttributeProviderAuthority;
import it.smartcommunitylab.aac.core.model.ConfigurableAttributeProvider;
import it.smartcommunitylab.aac.core.service.AttributeProviderService;

@Service
@PreAuthorize("hasAuthority('" + Config.R_ADMIN + "')"
        + " or hasAuthority(#realm+':" + Config.R_ADMIN + "')")
public class AttributeProviderManager
        extends ConfigurableProviderManager<ConfigurableAttributeProvider, AttributeProviderAuthority<?, ?, ?>> {

    public AttributeProviderManager(AttributeProviderService providerService) {
        super(providerService);
    }

}
