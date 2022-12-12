package it.smartcommunitylab.aac.core.authorities;

import java.util.List;

import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.core.model.Resource;
import it.smartcommunitylab.aac.core.provider.ResourceProvider;

/*
 * Provider authorities handle resource providers
 */
public interface ProviderAuthority<S extends ResourceProvider<R>, R extends Resource> {

    /*
     * Details
     */

    public String getAuthorityId();

    public String getType();

    /*
     * Providers
     */

    public boolean hasProvider(String providerId);

    public S findProvider(String providerId);

    public S getProvider(String providerId) throws NoSuchProviderException;

    public List<S> getProvidersByRealm(String realm);

}
