package it.smartcommunitylab.aac.core.service;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.authorities.IdentityProviderAuthority;
import it.smartcommunitylab.aac.core.base.AbstractAuthorityService;
import it.smartcommunitylab.aac.core.base.AbstractProviderConfig;
import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.core.provider.IdentityProvider;
import it.smartcommunitylab.aac.core.provider.IdentityProviderConfig;

//@Service
public class IdentityProviderAuthorityService extends
        AbstractAuthorityService<IdentityProviderAuthority<?, ?, ?, ?>>
        implements InitializingBean {

    public IdentityProviderAuthorityService(
//            Collection<IdentityProviderAuthority<IdentityProvider<? extends UserIdentity, ? extends ConfigMap, IdentityProviderConfig<?>>, ? extends UserIdentity, ? extends ConfigMap, IdentityProviderConfig<?>>> authorities) {
            Collection<IdentityProviderAuthority<?, ?, ?, ?>> authorities) {
        super(SystemKeys.RESOURCE_IDENTITY);

////        @SuppressWarnings("unchecked")
//        Map<String, IdentityProviderAuthority<IdentityProvider<? extends UserIdentity, ? extends ConfigMap, IdentityProviderConfig<?>>, ? extends UserIdentity, ? extends ConfigMap, IdentityProviderConfig<?>>> map = authorities
//                .stream()
////                .map(a -> (IdentityProviderAuthority<IdentityProvider<?>, IdentityProviderConfig<?>>) a)
//                .collect(Collectors.toMap(e -> e.getAuthorityId(), e -> e));

        this.setAuthorities(authorities);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notEmpty(authorities, "at least one identity provider authority is required");
    }

}
