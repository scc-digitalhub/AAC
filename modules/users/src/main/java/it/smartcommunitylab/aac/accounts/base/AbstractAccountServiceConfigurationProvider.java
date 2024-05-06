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

package it.smartcommunitylab.aac.accounts.base;

import it.smartcommunitylab.aac.accounts.model.ConfigurableAccountProvider;
import it.smartcommunitylab.aac.accounts.provider.AccountProviderConfigurationProvider;
import it.smartcommunitylab.aac.accounts.provider.AccountProviderSettingsMap;
import it.smartcommunitylab.aac.base.model.AbstractConfigMap;
import it.smartcommunitylab.aac.base.provider.AbstractConfigurationProvider;

public abstract class AbstractAccountServiceConfigurationProvider<
    P extends AbstractAccountServiceConfig<M>, M extends AbstractConfigMap
>
    extends AbstractConfigurationProvider<P, ConfigurableAccountProvider, AccountProviderSettingsMap, M>
    implements AccountProviderConfigurationProvider<P, M> {

    protected AbstractAccountServiceConfigurationProvider(String authority) {
        super(authority);
        setDefaultSettingsMap(new AccountProviderSettingsMap());
    }
    // @Override
    // protected ConfigurableAccountService buildConfigurable(P providerConfig) {
    //     ConfigurableAccountService cp = new ConfigurableAccountService(
    //         providerConfig.getAuthority(),
    //         providerConfig.getProvider(),
    //         providerConfig.getRealm()
    //     );

    //     cp.setName(providerConfig.getName());
    //     cp.setTitleMap(providerConfig.getTitleMap());
    //     cp.setDescriptionMap(providerConfig.getDescriptionMap());

    //     cp.setSettings(getConfiguration(providerConfig.getSettingsMap()));
    //     cp.setConfiguration(getConfiguration(providerConfig.getConfigMap()));

    //     // provider config are active by definition
    //     cp.setEnabled(true);

    //     return cp;
    // }
}
