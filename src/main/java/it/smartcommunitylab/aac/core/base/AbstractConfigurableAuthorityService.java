package it.smartcommunitylab.aac.core.base;

import it.smartcommunitylab.aac.core.authorities.ConfigurableAuthorityService;
import it.smartcommunitylab.aac.core.authorities.ConfigurableProviderAuthority;

public abstract class AbstractConfigurableAuthorityService<A extends ConfigurableProviderAuthority<?, ?, ?, ?, ?>>
        extends AbstractAuthorityService<A> implements ConfigurableAuthorityService<A> {

    public AbstractConfigurableAuthorityService(String type) {
        super(type);
    }

}
