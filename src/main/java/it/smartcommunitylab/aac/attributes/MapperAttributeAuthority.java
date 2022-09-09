package it.smartcommunitylab.aac.attributes;

import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.provider.MapperAttributeProvider;
import it.smartcommunitylab.aac.attributes.provider.MapperAttributeProviderConfig;
import it.smartcommunitylab.aac.attributes.provider.MapperAttributeProviderConfigMap;
import it.smartcommunitylab.aac.attributes.service.AttributeService;
import it.smartcommunitylab.aac.attributes.store.AttributeStore;
import it.smartcommunitylab.aac.attributes.store.AutoJdbcAttributeStore;
import it.smartcommunitylab.aac.attributes.store.InMemoryAttributeStore;
import it.smartcommunitylab.aac.attributes.store.NullAttributeStore;
import it.smartcommunitylab.aac.attributes.store.PersistentAttributeStore;
import it.smartcommunitylab.aac.core.base.AbstractAttributeAuthority;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;

@Service
public class MapperAttributeAuthority extends
        AbstractAttributeAuthority<MapperAttributeProvider, MapperAttributeProviderConfigMap, MapperAttributeProviderConfig> {

    // system attributes store
    protected final AutoJdbcAttributeStore jdbcAttributeStore;

    public MapperAttributeAuthority(
            AttributeService attributeService,
            AutoJdbcAttributeStore jdbcAttributeStore,
            ProviderConfigRepository<MapperAttributeProviderConfig> registrationRepository) {
        super(SystemKeys.AUTHORITY_MAPPER, attributeService, registrationRepository);
        Assert.notNull(jdbcAttributeStore, "attribute store is mandatory");

        this.jdbcAttributeStore = jdbcAttributeStore;
    }

    @Override
    protected MapperAttributeProvider buildProvider(MapperAttributeProviderConfig config) {
        AttributeStore attributeStore = getAttributeStore(config.getProvider(), config.getPersistence());

        MapperAttributeProvider ap = new MapperAttributeProvider(
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
