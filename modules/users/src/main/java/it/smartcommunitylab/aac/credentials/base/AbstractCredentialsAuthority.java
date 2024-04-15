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

import it.smartcommunitylab.aac.base.authorities.AbstractSingleConfigurableProviderAuthority;
import it.smartcommunitylab.aac.base.model.AbstractConfigMap;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.credentials.CredentialsServiceAuthority;
import it.smartcommunitylab.aac.credentials.model.ConfigurableCredentialsProvider;
import it.smartcommunitylab.aac.credentials.provider.CredentialsServiceConfigurationProvider;
import it.smartcommunitylab.aac.credentials.provider.CredentialsServiceSettingsMap;
import org.springframework.util.Assert;

public abstract class AbstractCredentialsAuthority<
    S extends AbstractCredentialsService<R, E, M, C>,
    R extends AbstractUserCredentials,
    E extends AbstractEditableUserCredentials,
    C extends AbstractCredentialsServiceConfig<M>,
    M extends AbstractConfigMap
>
    extends AbstractSingleConfigurableProviderAuthority<
        S,
        ConfigurableCredentialsProvider,
        C,
        CredentialsServiceSettingsMap,
        M
    >
    implements CredentialsServiceAuthority<S, R, E, M, C> {

    // configuration provider
    protected CredentialsServiceConfigurationProvider<C, M> configProvider;

    protected AbstractCredentialsAuthority(String authorityId, ProviderConfigRepository<C> registrationRepository) {
        super(authorityId, registrationRepository);
    }

    // @Override
    // public void afterPropertiesSet() throws Exception {
    //     super.afterPropertiesSet();
    //     Assert.notNull(configProvider, "config provider is mandatory");
    // }

    // @Override
    // public String getType() {
    //     return SystemKeys.RESOURCE_CREDENTIALS;
    // }

    @Override
    public CredentialsServiceConfigurationProvider<C, M> getConfigurationProvider() {
        return configProvider;
    }

    public void setConfigProvider(CredentialsServiceConfigurationProvider<C, M> configProvider) {
        Assert.notNull(configProvider, "config provider is mandatory");
        this.configProvider = configProvider;
    }
}
