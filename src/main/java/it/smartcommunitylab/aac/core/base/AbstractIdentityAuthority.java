package it.smartcommunitylab.aac.core.base;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.authorities.IdentityProviderAuthority;
import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.core.provider.FilterProvider;
import it.smartcommunitylab.aac.core.provider.IdentityProvider;
import it.smartcommunitylab.aac.core.provider.IdentityProviderConfig;
import it.smartcommunitylab.aac.core.provider.IdentityProviderConfigurationProvider;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import org.springframework.util.Assert;

public abstract class AbstractIdentityAuthority<
    S extends IdentityProvider<I, ?, ?, M, C>,
    I extends UserIdentity,
    M extends AbstractConfigMap,
    C extends AbstractIdentityProviderConfig<M>
>
    extends AbstractConfigurableProviderAuthority<S, I, ConfigurableIdentityProvider, M, C>
    implements IdentityProviderAuthority<S, I, M, C> {

    // configuration provider
    protected IdentityProviderConfigurationProvider<M, C> configProvider;

    public AbstractIdentityAuthority(String authorityId, ProviderConfigRepository<C> registrationRepository) {
        super(authorityId, registrationRepository);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        Assert.notNull(configProvider, "config provider is mandatory");
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_IDENTITY;
    }

    @Override
    public IdentityProviderConfigurationProvider<M, C> getConfigurationProvider() {
        return configProvider;
    }

    public void setConfigProvider(IdentityProviderConfigurationProvider<M, C> configProvider) {
        Assert.notNull(configProvider, "config provider is mandatory");
        this.configProvider = configProvider;
    }

    @Override
    public FilterProvider getFilterProvider() {
        // authorities are not required to expose filters
        return null;
    }
}
