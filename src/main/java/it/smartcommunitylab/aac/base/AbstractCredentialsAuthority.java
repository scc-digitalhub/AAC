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

package it.smartcommunitylab.aac.base;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.base.model.AbstractConfigMap;
import it.smartcommunitylab.aac.core.authorities.CredentialsServiceAuthority;
import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.core.model.EditableUserCredentials;
import it.smartcommunitylab.aac.core.model.UserCredentials;
import it.smartcommunitylab.aac.core.provider.AccountCredentialsService;
import it.smartcommunitylab.aac.core.provider.CredentialsServiceConfig;
import it.smartcommunitylab.aac.core.provider.CredentialsServiceConfigurationProvider;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.core.provider.config.ConfigurableCredentialsProvider;

import org.springframework.util.Assert;

public abstract class AbstractCredentialsAuthority<
    S extends AccountCredentialsService<R, E, M, C>,
    R extends UserCredentials,
    E extends EditableUserCredentials,
    M extends AbstractConfigMap,
    C extends AbstractCredentialsServiceConfig<M>
>
    extends AbstractSingleConfigurableProviderAuthority<S, R, ConfigurableCredentialsProvider, M, C>
    implements CredentialsServiceAuthority<S, R, E, M, C> {

    // configuration provider
    protected CredentialsServiceConfigurationProvider<M, C> configProvider;

    public AbstractCredentialsAuthority(String authorityId, ProviderConfigRepository<C> registrationRepository) {
        super(authorityId, registrationRepository);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        Assert.notNull(configProvider, "config provider is mandatory");
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_CREDENTIALS;
    }

    @Override
    public CredentialsServiceConfigurationProvider<M, C> getConfigurationProvider() {
        return configProvider;
    }

    public void setConfigProvider(CredentialsServiceConfigurationProvider<M, C> configProvider) {
        Assert.notNull(configProvider, "config provider is mandatory");
        this.configProvider = configProvider;
    }
}
