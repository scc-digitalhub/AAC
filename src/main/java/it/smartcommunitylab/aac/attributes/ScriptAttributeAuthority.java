package it.smartcommunitylab.aac.attributes;

import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.provider.ScriptAttributeProvider;
import it.smartcommunitylab.aac.attributes.provider.ScriptAttributeProviderConfig;
import it.smartcommunitylab.aac.attributes.provider.ScriptAttributeProviderConfigMap;
import it.smartcommunitylab.aac.attributes.service.AttributeService;
import it.smartcommunitylab.aac.attributes.store.AttributeStore;
import it.smartcommunitylab.aac.attributes.store.AutoJdbcAttributeStore;
import it.smartcommunitylab.aac.claims.ScriptExecutionService;
import it.smartcommunitylab.aac.core.base.AbstractAttributeAuthority;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;

@Service
public class ScriptAttributeAuthority extends
        AbstractAttributeAuthority<ScriptAttributeProvider, ScriptAttributeProviderConfig, ScriptAttributeProviderConfigMap> {

    // execution service for custom attributes mapping
    private final ScriptExecutionService executionService;

    public ScriptAttributeAuthority(
            AttributeService attributeService, ScriptExecutionService executionService,
            AutoJdbcAttributeStore jdbcAttributeStore,
            ProviderConfigRepository<ScriptAttributeProviderConfig> registrationRepository) {
        super(SystemKeys.AUTHORITY_SCRIPT, attributeService, jdbcAttributeStore, registrationRepository);
        Assert.notNull(executionService, "script execution service is mandatory");

        this.executionService = executionService;
    }

    @Override
    protected ScriptAttributeProvider buildProvider(ScriptAttributeProviderConfig config) {
        AttributeStore attributeStore = getAttributeStore(config.getProvider(), config.getPersistence());

        ScriptAttributeProvider ap = new ScriptAttributeProvider(
                config.getProvider(),
                attributeService, attributeStore,
                config,
                config.getRealm());
        ap.setExecutionService(executionService);

        return ap;
    }

}
