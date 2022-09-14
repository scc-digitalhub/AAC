package it.smartcommunitylab.aac.attributes.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractAttributeProviderConfig;
import it.smartcommunitylab.aac.core.model.ConfigurableAttributeProvider;

public class WebhookAttributeProviderConfig extends AbstractAttributeProviderConfig<WebhookAttributeProviderConfigMap> {
    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    public WebhookAttributeProviderConfig(String provider, String realm) {
        super(SystemKeys.AUTHORITY_WEBHOOK, provider, realm, new WebhookAttributeProviderConfigMap());
    }

    public WebhookAttributeProviderConfig(ConfigurableAttributeProvider cp) {
        super(cp);
    }

}
