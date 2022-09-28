package it.smartcommunitylab.aac.core.service;

import java.util.Collection;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.authorities.AttributeProviderAuthority;
import it.smartcommunitylab.aac.core.base.AbstractAuthorityService;

@Service
public class AttributeProviderAuthorityService extends
        AbstractAuthorityService<AttributeProviderAuthority<?, ?, ?>>
        implements InitializingBean {

    public AttributeProviderAuthorityService(
            Collection<AttributeProviderAuthority<?, ?, ?>> authorities) {
        super(SystemKeys.RESOURCE_ATTRIBUTES);
        this.setAuthorities(authorities);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notEmpty(authorities, "at least one provider authority is required");
    }

}
