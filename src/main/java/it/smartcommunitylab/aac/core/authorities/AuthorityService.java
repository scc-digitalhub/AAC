package it.smartcommunitylab.aac.core.authorities;

import it.smartcommunitylab.aac.common.NoSuchAuthorityException;
import it.smartcommunitylab.aac.core.model.Resource;
import it.smartcommunitylab.aac.core.provider.ResourceProvider;
import java.util.Collection;

public interface AuthorityService<A extends ProviderAuthority<? extends ResourceProvider<?>, ? extends Resource>> {
    /*
     * Details
     */
    public String getType();

    /*
     * Authorities read-only
     */
    public Collection<A> getAuthorities();

    public Collection<String> getAuthoritiesIds();

    public A findAuthority(String authorityId);

    public A getAuthority(String authorityId) throws NoSuchAuthorityException;
}
