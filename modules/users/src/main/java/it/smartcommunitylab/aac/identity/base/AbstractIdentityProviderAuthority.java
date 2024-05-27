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

package it.smartcommunitylab.aac.identity.base;

import it.smartcommunitylab.aac.accounts.base.AbstractUserAccount;
import it.smartcommunitylab.aac.base.authorities.AbstractConfigurableProviderAuthority;
import it.smartcommunitylab.aac.base.model.AbstractConfigMap;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.identity.IdentityProviderAuthority;
import it.smartcommunitylab.aac.identity.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.identity.provider.IdentityProviderConfigurationProvider;
import it.smartcommunitylab.aac.identity.provider.IdentityProviderSettingsMap;
import org.springframework.util.Assert;

public abstract class AbstractIdentityProviderAuthority<
    P extends AbstractIdentityProvider<
        U,
        ? extends AbstractUserAccount,
        ? extends AbstractUserAuthenticatedPrincipal,
        M,
        C
    >,
    U extends AbstractUserIdentity,
    C extends AbstractIdentityProviderConfig<M>,
    M extends AbstractConfigMap
>
    extends AbstractConfigurableProviderAuthority<P, C, IdentityProviderSettingsMap, M>
    implements IdentityProviderAuthority<P, C, M> {

    // configuration provider
    protected IdentityProviderConfigurationProvider<C, M> configProvider;

    protected AbstractIdentityProviderAuthority(
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
    // @Override
    // public String getType() {
    //     return SystemKeys.RESOURCE_IDENTITY;
    // }

    // @Override
    // public IdentityProviderConfigurationProvider<C, M> getConfigurationProvider() {
    //     return configProvider;
    // }

    // public void setConfigProvider(IdentityProviderConfigurationProvider<C, M> configProvider) {
    //     Assert.notNull(configProvider, "config provider is mandatory");
    //     this.configProvider = configProvider;
    // }

    // @Override
    // public FilterProvider getFilterProvider() {
    //     // authorities are not required to expose filters
    //     return null;
    // }
}
