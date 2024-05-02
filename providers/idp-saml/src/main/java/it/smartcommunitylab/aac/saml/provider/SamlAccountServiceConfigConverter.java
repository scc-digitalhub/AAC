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

package it.smartcommunitylab.aac.saml.provider;

import it.smartcommunitylab.aac.accounts.provider.AccountServiceSettingsMap;
import org.springframework.core.convert.converter.Converter;

public class SamlAccountServiceConfigConverter
    implements Converter<SamlIdentityProviderConfig, SamlAccountServiceConfig> {

    @Override
    public SamlAccountServiceConfig convert(SamlIdentityProviderConfig source) {
        SamlAccountServiceConfig config = new SamlAccountServiceConfig(
            source.getAuthority(),
            source.getProvider(),
            source.getRealm()
        );
        config.setName(source.getName());
        config.setTitleMap(source.getTitleMap());
        config.setDescriptionMap(source.getDescriptionMap());

        // we share the same configMap
        config.setConfigMap(source.getConfigMap());
        config.setVersion(source.getVersion());

        //build new settingsMap
        AccountServiceSettingsMap settingsMap = new AccountServiceSettingsMap();
        settingsMap.setPersistence(source.getPersistence());
        settingsMap.setRepositoryId(source.getRepositoryId());
        config.setSettingsMap(settingsMap);

        return config;
    }
}
