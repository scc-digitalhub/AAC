package it.smartcommunitylab.aac.core.base;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.util.Assert;

import it.smartcommunitylab.aac.common.NoSuchAuthorityException;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.authorities.ProviderAuthority;
import it.smartcommunitylab.aac.core.model.ConfigurableProvider;
import it.smartcommunitylab.aac.core.provider.ResourceProvider;

public class AbstractAuthorityService<R extends ResourceProvider, C extends ConfigurableProvider, A extends ProviderAuthority<R>> {

    private final String type;
    protected Map<String, A> authorities;

    public AbstractAuthorityService(String type) {
        Assert.hasText(type, "type is mandatory");
        this.type = type;
        authorities = Collections.emptyMap();
    }

    public String getType() {
        return type;
    }

    public void setAuthorities(
            Collection<A> authorities) {
        Assert.notNull(authorities, "authorities list can not be null");
        Map<String, A> map = authorities.stream()
                .collect(Collectors.toMap(e -> e.getAuthorityId(), e -> e));

        this.authorities = map;
    }

    public Collection<A> getAuthorities() {
        return authorities.values();
    }

    public A findAuthority(String authorityId) {
        return authorities.get(authorityId);
    }

    public A getAuthority(String authorityId) throws NoSuchAuthorityException {
        A authority = authorities.get(authorityId);
        if (authority == null) {
            throw new NoSuchAuthorityException();
        }

        return authority;
    }

    public R getProvider(String authorityId, String providerId)
            throws NoSuchAuthorityException, NoSuchProviderException {
        A authority = getAuthority(authorityId);
        return authority.getProvider(providerId);
    }

    public List<R> getProviders(String authorityId, String realm)
            throws NoSuchProviderException, NoSuchAuthorityException {
        A authority = getAuthority(authorityId);
        return authority.getProviders(realm);
    }

    public R registerProvider(
            C provider) throws NoSuchProviderException, NoSuchAuthorityException, SystemException {
        if (!provider.isEnabled()) {
            throw new IllegalArgumentException("provider is disabled");
        }

        String authorityId = provider.getAuthority();
        A authority = getAuthority(authorityId);
        return authority.registerProvider(provider);
    }

    public void unregisterProvider(C provider)
            throws NoSuchAuthorityException, SystemException {
        String authorityId = provider.getAuthority();
        String providerId = provider.getProvider();

        A authority = getAuthority(authorityId);
        authority.unregisterProvider(providerId);
    }

}
