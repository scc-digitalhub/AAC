/**
 * Copyright 2023 Fondazione Bruno Kessler
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

package it.smartcommunitylab.aac.core.provider.config;

import it.smartcommunitylab.aac.core.model.ConfigurableProvider;
import it.smartcommunitylab.aac.core.persistence.ProviderEntity;
import org.springframework.core.convert.converter.Converter;

public class ProviderEntityConverter implements Converter<ProviderEntity, ConfigurableProvider> {

    public ConfigurableProvider convert(ProviderEntity pe) {
        ConfigurableProvider cp = new ConfigurableProviderImpl(
            pe.getType(),
            pe.getAuthority(),
            pe.getProvider(),
            pe.getRealm()
        );

        //config
        cp.setSettings(pe.getSettingsMap());
        cp.setConfiguration(pe.getConfigurationMap());
        cp.setVersion(pe.getVersion());
        cp.setEnabled(pe.getEnabled() != null ? pe.getEnabled().booleanValue() : false);

        //details
        cp.setName(pe.getName());
        cp.setTitleMap(pe.getTitleMap());
        cp.setDescriptionMap(pe.getDescriptionMap());

        return cp;
    }
}
