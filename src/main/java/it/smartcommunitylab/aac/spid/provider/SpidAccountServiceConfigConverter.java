/*
 * Copyright 2024 the original author or authors
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

package it.smartcommunitylab.aac.spid.provider;

import it.smartcommunitylab.aac.accounts.provider.AccountServiceSettingsMap;
import org.springframework.core.convert.converter.Converter;

public class SpidAccountServiceConfigConverter
    implements Converter<SpidIdentityProviderConfig, SpidAccountServiceConfig> {

    @Override
    public SpidAccountServiceConfig convert(SpidIdentityProviderConfig source) {
        SpidAccountServiceConfig config = new SpidAccountServiceConfig(
            source.getAuthority(),
            source.getProvider(),
            source.getRealm()
        );
        // TODO: rivedere
        config.setName(source.getName());
        config.setTitleMap(source.getTitleMap());
        config.setDescriptionMap(source.getDescriptionMap());

        config.setConfigMap(source.getConfigMap());
        config.setVersion(source.getVersion());

        // build settings map with provided information
        AccountServiceSettingsMap settingsMap = new AccountServiceSettingsMap();
        settingsMap.setPersistence(source.getPersistence());
        //        settingsMap.setRepositoryId(source.getReposioryId());
        config.setSettingsMap(settingsMap);

        return config;
    }
}
