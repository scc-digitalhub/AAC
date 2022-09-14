package it.smartcommunitylab.aac.attributes;

import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.provider.WebhookAttributeProvider;
import it.smartcommunitylab.aac.attributes.provider.WebhookAttributeProviderConfig;
import it.smartcommunitylab.aac.attributes.provider.WebhookAttributeProviderConfigMap;
import it.smartcommunitylab.aac.attributes.service.AttributeService;
import it.smartcommunitylab.aac.attributes.store.AttributeStore;
import it.smartcommunitylab.aac.attributes.store.AutoJdbcAttributeStore;
import it.smartcommunitylab.aac.attributes.store.InMemoryAttributeStore;
import it.smartcommunitylab.aac.attributes.store.NullAttributeStore;
import it.smartcommunitylab.aac.attributes.store.PersistentAttributeStore;
import it.smartcommunitylab.aac.core.base.AbstractAttributeAuthority;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;

@Service
public class WebhookAttributeAuthority extends
        AbstractAttributeAuthority<WebhookAttributeProvider, WebhookAttributeProviderConfigMap, WebhookAttributeProviderConfig> {

    // system attributes store
    protected final AutoJdbcAttributeStore jdbcAttributeStore;

    public WebhookAttributeAuthority(
            AttributeService attributeService,
            AutoJdbcAttributeStore jdbcAttributeStore,
            ProviderConfigRepository<WebhookAttributeProviderConfig> registrationRepository) {
        super(SystemKeys.AUTHORITY_WEBHOOK, attributeService, registrationRepository);
        Assert.notNull(jdbcAttributeStore, "attribute store is mandatory");

        this.jdbcAttributeStore = jdbcAttributeStore;
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

    /*
     * helpers
     */

    protected AttributeStore getAttributeStore(String providerId, String persistence) {
        // we generate a new store for each provider
        AttributeStore store = new NullAttributeStore();
        if (SystemKeys.PERSISTENCE_LEVEL_REPOSITORY.equals(persistence)) {
            store = new PersistentAttributeStore(getAuthorityId(), providerId, jdbcAttributeStore);
        } else if (SystemKeys.PERSISTENCE_LEVEL_MEMORY.equals(persistence)) {
            store = new InMemoryAttributeStore(getAuthorityId(), providerId);
        }

        return store;
    }

}
