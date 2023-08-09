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

public abstract class AbstractCredentialsConfigurationProvider<
    M extends AbstractConfigMap, C extends AbstractCredentialsServiceConfig<M>
>
    extends AbstractConfigurationProvider<M, ConfigurableCredentialsProvider, C>
    implements CredentialsServiceConfigurationProvider<M, C> {

    protected AbstractCredentialsConfigurationProvider(String authority) {
        super(authority);
    }

    @Override
    public ConfigurableCredentialsProvider getConfigurable(C providerConfig) {
        ConfigurableCredentialsProvider cs = new ConfigurableCredentialsProvider(
            providerConfig.getAuthority(),
            providerConfig.getProvider(),
            providerConfig.getRealm()
        );

        cs.setName(providerConfig.getName());
        cs.setTitleMap(providerConfig.getTitleMap());
        cs.setDescriptionMap(providerConfig.getDescriptionMap());

        cs.setRepositoryId(providerConfig.getRepositoryId());

        cs.setConfiguration(getConfiguration(providerConfig.getConfigMap()));

        // provider config are active by definition
        cs.setEnabled(true);

        return cs;
    }
}
