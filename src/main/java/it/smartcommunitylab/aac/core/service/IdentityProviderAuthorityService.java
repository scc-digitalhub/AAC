package it.smartcommunitylab.aac.core.service;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.authorities.IdentityProviderAuthority;
import it.smartcommunitylab.aac.core.base.AbstractAuthorityService;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.core.provider.IdentityProvider;

//@Service
public class IdentityProviderAuthorityService extends
        AbstractAuthorityService<IdentityProvider<UserIdentity>, ConfigurableIdentityProvider, IdentityProviderAuthority<UserIdentity, IdentityProvider<UserIdentity>>>
        implements InitializingBean {

    public IdentityProviderAuthorityService(
            Collection<? extends IdentityProviderAuthority<? extends UserIdentity, ? extends IdentityProvider<? extends UserIdentity>>> authorities) {
        super(SystemKeys.RESOURCE_IDENTITY);

        @SuppressWarnings("unchecked")
        Map<String, IdentityProviderAuthority<UserIdentity, IdentityProvider<UserIdentity>>> map = authorities
                .stream()
                .map(a -> (IdentityProviderAuthority<UserIdentity, IdentityProvider<UserIdentity>>) a)
                .collect(Collectors.toMap(e -> e.getAuthorityId(), e -> e));

        this.authorities = map;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notEmpty(authorities, "at least one identity provider authority is required");
    }

}
