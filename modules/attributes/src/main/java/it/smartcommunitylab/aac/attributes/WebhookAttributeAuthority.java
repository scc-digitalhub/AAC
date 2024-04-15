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
import it.smartcommunitylab.aac.attributes.provider.WebhookAttributeProvider;
import it.smartcommunitylab.aac.attributes.provider.WebhookAttributeProviderConfig;
import it.smartcommunitylab.aac.attributes.provider.WebhookAttributeProviderConfigMap;
import it.smartcommunitylab.aac.attributes.service.AttributeService;
import it.smartcommunitylab.aac.attributes.store.AttributeStore;
import it.smartcommunitylab.aac.attributes.store.AutoJdbcAttributeStore;
import it.smartcommunitylab.aac.attributes.store.InMemoryAttributeStore;
import it.smartcommunitylab.aac.attributes.store.NullAttributeStore;
import it.smartcommunitylab.aac.attributes.store.PersistentAttributeStore;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.model.PersistenceMode;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
public class WebhookAttributeAuthority
    extends AbstractAttributeAuthority<
        WebhookAttributeProvider,
        WebhookAttributeProviderConfigMap,
        WebhookAttributeProviderConfig
    > {

    // system attributes store
    protected final AutoJdbcAttributeStore jdbcAttributeStore;

    public WebhookAttributeAuthority(
        AttributeService attributeService,
        AutoJdbcAttributeStore jdbcAttributeStore,
        ProviderConfigRepository<WebhookAttributeProviderConfig> registrationRepository
    ) {
        super(SystemKeys.AUTHORITY_WEBHOOK, attributeService, registrationRepository);
        Assert.notNull(jdbcAttributeStore, "attribute store is mandatory");

        this.jdbcAttributeStore = jdbcAttributeStore;
    }

    @Override
    protected WebhookAttributeProvider buildProvider(WebhookAttributeProviderConfig config) {
        AttributeStore attributeStore = getAttributeStore(config.getProvider(), config.getPersistence());

        WebhookAttributeProvider ap = new WebhookAttributeProvider(
            config.getProvider(),
            attributeService,
            attributeStore,
            config,
            config.getRealm()
        );

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
