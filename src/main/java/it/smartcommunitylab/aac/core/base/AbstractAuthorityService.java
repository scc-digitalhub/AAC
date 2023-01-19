package it.smartcommunitylab.aac.core.base;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.util.Assert;

import it.smartcommunitylab.aac.common.NoSuchAuthorityException;
import it.smartcommunitylab.aac.core.authorities.AuthorityService;
import it.smartcommunitylab.aac.core.authorities.ProviderAuthority;

public abstract class AbstractAuthorityService<A extends ProviderAuthority<?, ?>>
        implements AuthorityService<A> {

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

    public Collection<String> getAuthoritiesIds() {
        return new ArrayList<>(authorities.keySet());
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

    public A registerAuthority(A a) {
        Assert.notNull(a, "authority can not be null");
        Assert.hasText(a.getAuthorityId(), "id is mandatory");

//        // cast principal and handle errors
//        A a = null;
//        try {
//            @SuppressWarnings("unchecked")
//            A ac = (A) pa;
//            a = ac;
//        } catch (ClassCastException e) {
//            throw new IllegalArgumentException("wrong provider class");
//        }

        // check if already registered
        if (this.authorities.containsKey(a.getAuthorityId())) {
            throw new IllegalArgumentException("already registered");
        }

        this.authorities.put(a.getAuthorityId(), a);
        return a;
    }

//    public R getProvider(String authorityId, String providerId)
//            throws NoSuchAuthorityException, NoSuchProviderException {
//        A authority = getAuthority(authorityId);
//        return authority.getProvider(providerId);
//    }
//
//    public List<R> getProviders(String authorityId, String realm)
//            throws NoSuchProviderException, NoSuchAuthorityException {
//        A authority = getAuthority(authorityId);
//        return authority.getProviders(realm);
//    }
//
//    public R registerProvider(
//            C provider) throws NoSuchProviderException, NoSuchAuthorityException, SystemException {
//        if (!provider.isEnabled()) {
//            throw new IllegalArgumentException("provider is disabled");
//        }
//
//        String authorityId = provider.getAuthority();
//        A authority = getAuthority(authorityId);
//        return authority.registerProvider(provider);
//    }
//
//    public void unregisterProvider(C provider)
//            throws NoSuchAuthorityException, SystemException {
//        String authorityId = provider.getAuthority();
//        String providerId = provider.getProvider();
//
//        A authority = getAuthority(authorityId);
//        authority.unregisterProvider(providerId);
//    }

}
