package it.smartcommunitylab.aac.core.base;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.authorities.CredentialsServiceAuthority;
import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.core.model.ConfigurableCredentialsProvider;
import it.smartcommunitylab.aac.core.model.UserCredentials;
import it.smartcommunitylab.aac.core.provider.CredentialsServiceConfig;
import it.smartcommunitylab.aac.core.provider.CredentialsServiceConfigurationProvider;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.core.provider.AccountCredentialsService;

public abstract class AbstractCredentialsAuthority<S extends AccountCredentialsService<R, M, C>, R extends UserCredentials, M extends ConfigMap, C extends CredentialsServiceConfig<M>>
        extends AbstractSingleProviderAuthority<S, R, ConfigurableCredentialsProvider, M, C>
        implements CredentialsServiceAuthority<S, R, M, C>, InitializingBean {

    // configuration provider
    protected CredentialsServiceConfigurationProvider<M, C> configProvider;

    public AbstractCredentialsAuthority(
            String authorityId,
            ProviderConfigRepository<C> registrationRepository) {
        super(authorityId, registrationRepository);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        Assert.notNull(configProvider, "config provider is mandatory");
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_CREDENTIALS;
    }

    @Override
    public CredentialsServiceConfigurationProvider<M, C> getConfigurationProvider() {
        return configProvider;
    }

    public void setConfigProvider(CredentialsServiceConfigurationProvider<M, C> configProvider) {
        Assert.notNull(configProvider, "config provider is mandatory");
        this.configProvider = configProvider;
    }

}
