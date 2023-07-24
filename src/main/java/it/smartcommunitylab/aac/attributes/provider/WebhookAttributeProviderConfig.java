package it.smartcommunitylab.aac.attributes.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.AbstractAttributeProviderConfig;
import it.smartcommunitylab.aac.core.model.ConfigurableAttributeProvider;

public class WebhookAttributeProviderConfig extends AbstractAttributeProviderConfig<WebhookAttributeProviderConfigMap> {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;
    public static final String RESOURCE_TYPE =
        SystemKeys.RESOURCE_PROVIDER + SystemKeys.ID_SEPARATOR + WebhookAttributeProviderConfigMap.RESOURCE_TYPE;

    public WebhookAttributeProviderConfig(String provider, String realm) {
        super(SystemKeys.AUTHORITY_WEBHOOK, provider, realm, new WebhookAttributeProviderConfigMap());
    }

    public WebhookAttributeProviderConfig(
        ConfigurableAttributeProvider cp,
        WebhookAttributeProviderConfigMap configMap
    ) {
        super(cp, configMap);
    }

    /**
     * Private constructor for JPA and other serialization tools.
     *
     * We need to implement this to enable deserialization of resources via
     * reflection
     */
    @SuppressWarnings("unused")
    private WebhookAttributeProviderConfig() {
        super(SystemKeys.AUTHORITY_WEBHOOK, (String) null, (String) null, new WebhookAttributeProviderConfigMap());
    }
}
