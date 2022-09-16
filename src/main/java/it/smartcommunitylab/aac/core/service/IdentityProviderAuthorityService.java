package it.smartcommunitylab.aac.core.service;

import java.util.Collection;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.authorities.IdentityProviderAuthority;
import it.smartcommunitylab.aac.core.base.AbstractAuthorityService;

//@Service
public class IdentityProviderAuthorityService extends
        AbstractAuthorityService<IdentityProviderAuthority<?, ?, ?, ?>>
        implements InitializingBean {

    public IdentityProviderAuthorityService(
            Collection<IdentityProviderAuthority<?, ?, ?, ?>> authorities) {
        super(SystemKeys.RESOURCE_IDENTITY);
        this.setAuthorities(authorities);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notEmpty(authorities, "at least one identity provider authority is required");
    }

}
