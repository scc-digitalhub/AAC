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

package it.smartcommunitylab.aac.base.provider;

import it.smartcommunitylab.aac.base.model.AbstractConfigMap;
import it.smartcommunitylab.aac.core.provider.config.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.identity.provider.AbstractIdentityProviderConfig;
import it.smartcommunitylab.aac.identity.provider.IdentityProviderConfigurationProvider;

public abstract class AbstractIdentityConfigurationProvider<
    M extends AbstractConfigMap, C extends AbstractIdentityProviderConfig<M>
>
    extends AbstractConfigurationProvider<M, ConfigurableIdentityProvider, C>
    implements IdentityProviderConfigurationProvider<M, C> {

    protected AbstractIdentityConfigurationProvider(String authority) {
        super(authority);
    }

    @Override
    public ConfigurableIdentityProvider getConfigurable(C providerConfig) {
        ConfigurableIdentityProvider cp = new ConfigurableIdentityProvider(
            providerConfig.getAuthority(),
            providerConfig.getProvider(),
            providerConfig.getRealm()
        );

        cp.setName(providerConfig.getName());
        cp.setTitleMap(providerConfig.getTitleMap());
        cp.setDescriptionMap(providerConfig.getDescriptionMap());

        cp.setLinkable(providerConfig.getLinkable());
        String persistenceValue = providerConfig.getPersistence() != null
            ? providerConfig.getPersistence().getValue()
            : null;
        cp.setPersistence(persistenceValue);
        cp.setEvents(providerConfig.getEvents());
        cp.setPosition(providerConfig.getPosition());

        cp.setConfiguration(getConfiguration(providerConfig.getConfigMap()));
        cp.setHookFunctions(providerConfig.getHookFunctions());

        // provider config are active by definition
        cp.setEnabled(true);

        return cp;
    }
}
