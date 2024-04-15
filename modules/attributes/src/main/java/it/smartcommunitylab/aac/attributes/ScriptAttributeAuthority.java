/*
 * Copyright 2023 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.attributes;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.base.AbstractAttributeAuthority;
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
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.model.PersistenceMode;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
public class ScriptAttributeAuthority
    extends AbstractAttributeAuthority<
        ScriptAttributeProvider,
        ScriptAttributeProviderConfigMap,
        ScriptAttributeProviderConfig
    > {

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

    protected AttributeStore getAttributeStore(String providerId, PersistenceMode persistence) {
        // we generate a new store for each provider
        AttributeStore store = new NullAttributeStore();
        if (PersistenceMode.REPOSITORY == persistence) {
            store = new PersistentAttributeStore(getAuthorityId(), providerId, jdbcAttributeStore);
        } else if (PersistenceMode.SESSION == persistence) {
            store = new InMemoryAttributeStore(getAuthorityId(), providerId);
        }

        return store;
    }
}
