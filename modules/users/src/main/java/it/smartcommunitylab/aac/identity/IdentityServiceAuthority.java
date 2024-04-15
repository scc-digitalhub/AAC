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

package it.smartcommunitylab.aac.identity;

import it.smartcommunitylab.aac.accounts.model.EditableUserAccount;
import it.smartcommunitylab.aac.accounts.model.UserAccount;
import it.smartcommunitylab.aac.accounts.provider.AccountServiceSettingsMap;
import it.smartcommunitylab.aac.core.authorities.ConfigurableProviderAuthority;
import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.core.provider.FilterProvider;
import it.smartcommunitylab.aac.identity.model.ConfigurableIdentityService;
import it.smartcommunitylab.aac.identity.model.UserIdentity;
import it.smartcommunitylab.aac.identity.provider.IdentityService;
import it.smartcommunitylab.aac.identity.provider.IdentityServiceConfig;

public interface IdentityServiceAuthority<
    S extends IdentityService<I, U, E, M, C>,
    I extends UserIdentity,
    U extends UserAccount,
    E extends EditableUserAccount,
    M extends ConfigMap,
    C extends IdentityServiceConfig<M>
>
    extends ConfigurableProviderAuthority<S, ConfigurableIdentityService, C, AccountServiceSettingsMap, M> {
    /*
     * Filter provider exposes filters for registration in filter chain
     */
    public FilterProvider getFilterProvider();
}
