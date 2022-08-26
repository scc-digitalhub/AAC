package it.smartcommunitylab.aac.attributes.provider;

import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractAttributeConfigurationProvider;
import it.smartcommunitylab.aac.core.model.ConfigurableAttributeProvider;
import it.smartcommunitylab.aac.core.model.ConfigurableProvider;

@Service
public class WebhookAttributeConfigurationProvider extends
        AbstractAttributeConfigurationProvider<WebhookAttributeProviderConfig, WebhookAttributeProviderConfigMap> {

    public WebhookAttributeConfigurationProvider() {
        super(SystemKeys.AUTHORITY_MAPPER);
        setDefaultConfigMap(new WebhookAttributeProviderConfigMap());
    }

    @Override
    protected WebhookAttributeProviderConfig buildConfig(ConfigurableProvider cp) {
        Assert.isInstanceOf(ConfigurableAttributeProvider.class, cp);
        return new WebhookAttributeProviderConfig((ConfigurableAttributeProvider) cp);
    }

}
