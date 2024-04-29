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

package it.smartcommunitylab.aac.credentials.base;

import it.smartcommunitylab.aac.base.model.AbstractConfigMap;
import it.smartcommunitylab.aac.base.provider.AbstractConfigurationProvider;
import it.smartcommunitylab.aac.credentials.model.ConfigurableCredentialsProvider;
import it.smartcommunitylab.aac.credentials.provider.CredentialsServiceConfigurationProvider;
import it.smartcommunitylab.aac.credentials.provider.CredentialsServiceSettingsMap;

public abstract class AbstractCredentialsConfigurationProvider<
    P extends AbstractCredentialsServiceConfig<M>, M extends AbstractConfigMap
>
    extends AbstractConfigurationProvider<P, ConfigurableCredentialsProvider, CredentialsServiceSettingsMap, M>
    implements CredentialsServiceConfigurationProvider<P, M> {

    protected AbstractCredentialsConfigurationProvider(String authority) {
        super(authority);
        setDefaultSettingsMap(new CredentialsServiceSettingsMap());
    }

    @Override
    protected ConfigurableCredentialsProvider buildConfigurable(P providerConfig) {
        ConfigurableCredentialsProvider cp = new ConfigurableCredentialsProvider(
            providerConfig.getAuthority(),
            providerConfig.getProvider(),
            providerConfig.getRealm()
        );

        cp.setName(providerConfig.getName());
        cp.setTitleMap(providerConfig.getTitleMap());
        cp.setDescriptionMap(providerConfig.getDescriptionMap());

        cp.setSettings(getConfiguration(providerConfig.getSettingsMap()));
        cp.setConfiguration(getConfiguration(providerConfig.getConfigMap()));

        // provider config are active by definition
        cp.setEnabled(true);

        return cp;
    }
}
