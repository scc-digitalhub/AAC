package it.smartcommunitylab.aac.core.base;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.service.AttributeService;
import it.smartcommunitylab.aac.core.authorities.AttributeProviderAuthority;
import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.core.model.ConfigurableAttributeProvider;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.provider.AttributeConfigurationProvider;
import it.smartcommunitylab.aac.core.provider.AttributeProvider;
import it.smartcommunitylab.aac.core.provider.AttributeProviderConfig;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

public abstract class AbstractAttributeAuthority<
    S extends AttributeProvider<M, C>, M extends AbstractConfigMap, C extends AbstractAttributeProviderConfig<M>
>
    extends AbstractConfigurableProviderAuthority<S, UserAttributes, ConfigurableAttributeProvider, M, C>
    implements AttributeProviderAuthority<S, M, C>, InitializingBean {

    // attributes sets service
    protected final AttributeService attributeService;

    // configuration provider
    protected AttributeConfigurationProvider<M, C> configProvider;

    public AbstractAttributeAuthority(
        String authorityId,
        AttributeService attributeService,
        ProviderConfigRepository<C> registrationRepository
    ) {
        super(authorityId, registrationRepository);
        Assert.notNull(attributeService, "attribute service is mandatory");

        this.attributeService = attributeService;
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_ATTRIBUTES;
    }

    @Override
    public AttributeConfigurationProvider<M, C> getConfigurationProvider() {
        return configProvider;
    }

    @Autowired
    public void setConfigProvider(AttributeConfigurationProvider<M, C> configProvider) {
        Assert.notNull(configProvider, "config provider is mandatory");
        this.configProvider = configProvider;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(configProvider, "config provider is mandatory");
    }
}
