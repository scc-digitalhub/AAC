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

package it.smartcommunitylab.aac.core.base;

import it.smartcommunitylab.aac.core.base.model.AbstractConfigMap;
import it.smartcommunitylab.aac.core.base.provider.AbstractConfigurationProvider;
import it.smartcommunitylab.aac.core.provider.AttributeConfigurationProvider;
import it.smartcommunitylab.aac.core.provider.config.ConfigurableAttributeProvider;

public abstract class AbstractAttributeConfigurationProvider<
    M extends AbstractConfigMap, C extends AbstractAttributeProviderConfig<M>
>
    extends AbstractConfigurationProvider<M, ConfigurableAttributeProvider, C>
    implements AttributeConfigurationProvider<M, C> {

    public AbstractAttributeConfigurationProvider(String authority) {
        super(authority);
    }

    @Override
    public ConfigurableAttributeProvider getConfigurable(C providerConfig) {
        ConfigurableAttributeProvider cp = new ConfigurableAttributeProvider(
            providerConfig.getAuthority(),
            providerConfig.getProvider(),
            providerConfig.getRealm()
        );

        cp.setPersistence(providerConfig.getPersistence());
        cp.setEvents(providerConfig.getEvents());

        cp.setName(providerConfig.getName());
        cp.setTitleMap(providerConfig.getTitleMap());
        cp.setDescriptionMap(providerConfig.getDescriptionMap());

        cp.setConfiguration(getConfiguration(providerConfig.getConfigMap()));
        cp.setAttributeSets(providerConfig.getAttributeSets());

        // provider config are active by definition
        cp.setEnabled(true);

        return cp;
    }
}
