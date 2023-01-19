package it.smartcommunitylab.aac.core.authorities;

import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.core.model.ConfigurableProvider;
import it.smartcommunitylab.aac.core.model.Resource;
import it.smartcommunitylab.aac.core.provider.ConfigurableResourceProvider;
import it.smartcommunitylab.aac.core.provider.ProviderConfig;

/*
 * Single provider per realm 
 */

public interface SingleProviderAuthority<S extends ConfigurableResourceProvider<R, T, M, C>, R extends Resource, T extends ConfigurableProvider, M extends ConfigMap, C extends ProviderConfig<M>> {

    public S findProviderByRealm(String realm);

    public S getProviderByRealm(String realm) throws NoSuchProviderException;

}
