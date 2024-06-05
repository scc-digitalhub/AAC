package it.smartcommunitylab.aac.scope;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractAuthorityService;
import it.smartcommunitylab.aac.scope.model.ApiResource;
import it.smartcommunitylab.aac.scope.model.ApiResourceProvider;
import it.smartcommunitylab.aac.scope.model.ApiResourceProviderAuthority;
import java.util.Collection;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
public class ApiResourceProviderAuthorityService
    extends AbstractAuthorityService<
        ApiResourceProviderAuthority<? extends ApiResourceProvider<?>, ? extends ApiResource>
    >
    implements InitializingBean {

    public ApiResourceProviderAuthorityService(
        Collection<ApiResourceProviderAuthority<? extends ApiResourceProvider<?>, ? extends ApiResource>> authorities
    ) {
        super(SystemKeys.RESOURCE_API_RESOURCE);
        this.setAuthorities(authorities);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notEmpty(authorities, "at least one provider authority is required");
    }
}
