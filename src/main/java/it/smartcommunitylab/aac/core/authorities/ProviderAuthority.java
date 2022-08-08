package it.smartcommunitylab.aac.core.authorities;

import java.util.List;

import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.model.ConfigurableProvider;
import it.smartcommunitylab.aac.core.provider.ResourceProvider;

public interface ProviderAuthority<R extends ResourceProvider> {

    public String getAuthorityId();

    public boolean hasProvider(String providerId);

    public R getProvider(String providerId) throws NoSuchProviderException;

    public List<R> getProviders(String realm);

    public R registerProvider(ConfigurableProvider config)
            throws IllegalArgumentException, RegistrationException, SystemException;

    public void unregisterProvider(String providerId) throws SystemException;

}
