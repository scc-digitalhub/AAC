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
import it.smartcommunitylab.aac.core.authorities.IdentityProviderAuthority;
import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.core.provider.FilterProvider;
import it.smartcommunitylab.aac.core.provider.IdentityProvider;
import it.smartcommunitylab.aac.core.provider.IdentityProviderConfigurationProvider;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.core.provider.config.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.core.provider.config.IdentityProviderConfig;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

public abstract class AbstractSingleProviderIdentityAuthority<
    S extends IdentityProvider<I, ?, ?, M, C>,
    I extends UserIdentity,
    M extends AbstractConfigMap,
    C extends AbstractIdentityProviderConfig<M>
>
    extends AbstractSingleConfigurableProviderAuthority<S, I, ConfigurableIdentityProvider, M, C>
    implements IdentityProviderAuthority<S, I, M, C>, InitializingBean {

    // configuration provider
    protected IdentityProviderConfigurationProvider<M, C> configProvider;

    public AbstractSingleProviderIdentityAuthority(
        String authorityId,
        ProviderConfigRepository<C> registrationRepository
    ) {
        super(authorityId, registrationRepository);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        Assert.notNull(configProvider, "config provider is mandatory");
    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_IDENTITY;
    }

    @Override
    public IdentityProviderConfigurationProvider<M, C> getConfigurationProvider() {
        return configProvider;
    }

    public void setConfigProvider(IdentityProviderConfigurationProvider<M, C> configProvider) {
        Assert.notNull(configProvider, "config provider is mandatory");
        this.configProvider = configProvider;
    }

    @Override
    public FilterProvider getFilterProvider() {
        // authorities are not required to expose filters
        return null;
    }
}
