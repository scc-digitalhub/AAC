package it.smartcommunitylab.aac.core.authorities;

import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.core.model.ConfigurableProvider;
import it.smartcommunitylab.aac.core.provider.ProviderConfig;
import it.smartcommunitylab.aac.core.provider.ResourceProvider;

public interface ConfigurableAuthorityService<
    A extends ConfigurableProviderAuthority<? extends ResourceProvider<?>, ?, ? extends ConfigurableProvider, ? extends ConfigMap, ? extends ProviderConfig<? extends ConfigMap>>
>
    extends AuthorityService<A> {}
