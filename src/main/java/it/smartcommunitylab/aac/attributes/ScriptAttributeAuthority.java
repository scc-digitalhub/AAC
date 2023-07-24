package it.smartcommunitylab.aac.attributes;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.provider.ScriptAttributeProvider;
import it.smartcommunitylab.aac.attributes.provider.ScriptAttributeProviderConfig;
import it.smartcommunitylab.aac.attributes.provider.ScriptAttributeProviderConfigMap;
import it.smartcommunitylab.aac.attributes.service.AttributeService;
import it.smartcommunitylab.aac.attributes.store.AttributeStore;
import it.smartcommunitylab.aac.attributes.store.AutoJdbcAttributeStore;
import it.smartcommunitylab.aac.attributes.store.InMemoryAttributeStore;
import it.smartcommunitylab.aac.attributes.store.NullAttributeStore;
import it.smartcommunitylab.aac.attributes.store.PersistentAttributeStore;
import it.smartcommunitylab.aac.claims.ScriptExecutionService;
import it.smartcommunitylab.aac.core.base.AbstractAttributeAuthority;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
public class ScriptAttributeAuthority
    extends AbstractAttributeAuthority<ScriptAttributeProvider, ScriptAttributeProviderConfigMap, ScriptAttributeProviderConfig> {

    // execution service for custom attributes mapping
    private final ScriptExecutionService executionService;

    // system attributes store
    protected final AutoJdbcAttributeStore jdbcAttributeStore;

    public ScriptAttributeAuthority(
        AttributeService attributeService,
        ScriptExecutionService executionService,
        AutoJdbcAttributeStore jdbcAttributeStore,
        ProviderConfigRepository<ScriptAttributeProviderConfig> registrationRepository
    ) {
        super(SystemKeys.AUTHORITY_SCRIPT, attributeService, registrationRepository);
        Assert.notNull(executionService, "script execution service is mandatory");
        Assert.notNull(jdbcAttributeStore, "attribute store is mandatory");

        this.jdbcAttributeStore = jdbcAttributeStore;
        this.executionService = executionService;
    }

    @Override
    protected ScriptAttributeProvider buildProvider(ScriptAttributeProviderConfig config) {
        AttributeStore attributeStore = getAttributeStore(config.getProvider(), config.getPersistence());

        ScriptAttributeProvider ap = new ScriptAttributeProvider(
            config.getProvider(),
            attributeService,
            attributeStore,
            config,
            config.getRealm()
        );
        ap.setExecutionService(executionService);

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
