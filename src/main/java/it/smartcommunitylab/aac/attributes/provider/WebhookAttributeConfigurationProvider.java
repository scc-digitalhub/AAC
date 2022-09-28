package it.smartcommunitylab.aac.attributes.provider;

import org.springframework.stereotype.Service;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractAttributeConfigurationProvider;
import it.smartcommunitylab.aac.core.model.ConfigurableAttributeProvider;

@Service
public class WebhookAttributeConfigurationProvider extends
        AbstractAttributeConfigurationProvider<WebhookAttributeProviderConfigMap, WebhookAttributeProviderConfig> {

    public WebhookAttributeConfigurationProvider() {
        super(SystemKeys.AUTHORITY_WEBHOOK);
        setDefaultConfigMap(new WebhookAttributeProviderConfigMap());
    }

    @Override
    protected WebhookAttributeProviderConfig buildConfig(ConfigurableAttributeProvider cp) {
        return new WebhookAttributeProviderConfig(cp);
    }

}
