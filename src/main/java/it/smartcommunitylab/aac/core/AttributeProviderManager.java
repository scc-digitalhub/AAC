package it.smartcommunitylab.aac.core;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.core.authorities.AttributeProviderAuthority;
import it.smartcommunitylab.aac.core.model.ConfigurableAttributeProvider;
import it.smartcommunitylab.aac.core.service.AttributeProviderService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@PreAuthorize("hasAuthority('" + Config.R_ADMIN + "')" + " or hasAuthority(#realm+':" + Config.R_ADMIN + "')")
public class AttributeProviderManager
    extends ConfigurableProviderManager<ConfigurableAttributeProvider, AttributeProviderAuthority<?, ?, ?>> {

    public AttributeProviderManager(AttributeProviderService providerService) {
        super(providerService);
    }
}
