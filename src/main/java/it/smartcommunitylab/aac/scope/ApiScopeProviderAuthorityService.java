package it.smartcommunitylab.aac.scope;

import java.util.Collection;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractAuthorityService;
import it.smartcommunitylab.aac.scope.model.ApiScopeProviderAuthority;

@Service
public class ApiScopeProviderAuthorityService extends AbstractAuthorityService<ApiScopeProviderAuthority<?, ?>>
        implements InitializingBean {

    public ApiScopeProviderAuthorityService(Collection<ApiScopeProviderAuthority<?, ?>> authorities) {
        super(SystemKeys.RESOURCE_SCOPE);
        this.setAuthorities(authorities);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notEmpty(authorities, "at least one provider authority is required");
    }

}
