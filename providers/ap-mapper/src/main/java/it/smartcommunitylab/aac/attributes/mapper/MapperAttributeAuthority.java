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

package it.smartcommunitylab.aac.attributes.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.base.AbstractAttributeAuthority;
import it.smartcommunitylab.aac.attributes.service.AttributeService;
import it.smartcommunitylab.aac.attributes.store.AttributeStore;
import it.smartcommunitylab.aac.attributes.store.AutoJdbcAttributeStore;
import it.smartcommunitylab.aac.attributes.store.PersistentAttributeStore;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
public class MapperAttributeAuthority
    extends AbstractAttributeAuthority<
        MapperAttributeProvider,
        MapperAttributeProviderConfigMap,
        MapperAttributeProviderConfig
    > {

    // system attributes store
    protected final AutoJdbcAttributeStore jdbcAttributeStore;

    public MapperAttributeAuthority(
        AttributeService attributeService,
        AutoJdbcAttributeStore jdbcAttributeStore,
        ProviderConfigRepository<MapperAttributeProviderConfig> registrationRepository
    ) {
        super(SystemKeys.AUTHORITY_MAPPER, attributeService, registrationRepository);
        Assert.notNull(jdbcAttributeStore, "attribute store is mandatory");

        this.jdbcAttributeStore = jdbcAttributeStore;
    }

    @Override
    protected MapperAttributeProvider buildProvider(MapperAttributeProviderConfig config) {
        AttributeStore attributeStore = new PersistentAttributeStore(
            getAuthorityId(),
            config.getProvider(),
            jdbcAttributeStore
        );

        return new MapperAttributeProvider(
            config.getProvider(),
            attributeService,
            attributeStore,
            config,
            config.getRealm()
        );
    }
}
