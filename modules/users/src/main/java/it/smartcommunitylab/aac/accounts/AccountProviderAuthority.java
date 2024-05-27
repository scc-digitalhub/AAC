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

package it.smartcommunitylab.aac.accounts;

import it.smartcommunitylab.aac.accounts.model.UserAccount;
import it.smartcommunitylab.aac.accounts.provider.AccountProvider;
import it.smartcommunitylab.aac.accounts.provider.AccountProviderConfig;
import it.smartcommunitylab.aac.accounts.provider.AccountProviderSettingsMap;
import it.smartcommunitylab.aac.core.authorities.ConfigurableProviderAuthority;
import it.smartcommunitylab.aac.model.ConfigMap;

public interface AccountProviderAuthority<
    S extends AccountProvider<U, M, P>, U extends UserAccount, P extends AccountProviderConfig<M>, M extends ConfigMap
>
    extends ConfigurableProviderAuthority<S, P, AccountProviderSettingsMap, M> {}
