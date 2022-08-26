package it.smartcommunitylab.aac.attributes;

import org.springframework.stereotype.Service;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.provider.WebhookAttributeProvider;
import it.smartcommunitylab.aac.attributes.provider.WebhookAttributeProviderConfig;
import it.smartcommunitylab.aac.attributes.provider.WebhookAttributeProviderConfigMap;
import it.smartcommunitylab.aac.attributes.service.AttributeService;
import it.smartcommunitylab.aac.attributes.store.AttributeStore;
import it.smartcommunitylab.aac.attributes.store.AutoJdbcAttributeStore;
import it.smartcommunitylab.aac.core.base.AbstractAttributeAuthority;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;

@Service
public class WebhookAttributeAuthority extends
        AbstractAttributeAuthority<WebhookAttributeProvider, WebhookAttributeProviderConfig, WebhookAttributeProviderConfigMap> {

    public WebhookAttributeAuthority(
            AttributeService attributeService,
            AutoJdbcAttributeStore jdbcAttributeStore,
            ProviderConfigRepository<WebhookAttributeProviderConfig> registrationRepository) {
        super(SystemKeys.AUTHORITY_WEBHOOK, attributeService, jdbcAttributeStore, registrationRepository);
    }

    @Override
    protected WebhookAttributeProvider buildProvider(WebhookAttributeProviderConfig config) {
        AttributeStore attributeStore = getAttributeStore(config.getProvider(), config.getPersistence());

        WebhookAttributeProvider ap = new WebhookAttributeProvider(
                config.getProvider(),
                attributeService, attributeStore,
                config,
                config.getRealm());

        return ap;
    }

}
