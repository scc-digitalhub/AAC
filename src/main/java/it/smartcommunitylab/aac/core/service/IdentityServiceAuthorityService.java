package it.smartcommunitylab.aac.core.service;

import java.util.Collection;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.authorities.IdentityServiceAuthority;
import it.smartcommunitylab.aac.core.base.AbstractAuthorityService;

@Service
public class IdentityServiceAuthorityService
        extends AbstractAuthorityService<IdentityServiceAuthority<?, ?, ?, ?, ?, ?>>
        implements InitializingBean {

    public IdentityServiceAuthorityService(Collection<IdentityServiceAuthority<?, ?, ?, ?, ?, ?>> authorities) {
        super(SystemKeys.RESOURCE_USER);

        this.setAuthorities(authorities);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notEmpty(authorities, "at least one provider authority is required");
    }

}
