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

package it.smartcommunitylab.aac.core.persistence;

import it.smartcommunitylab.aac.core.model.ConfigurableProvider;
import it.smartcommunitylab.aac.model.ConfigMap;
import java.util.Collections;
import java.util.function.Supplier;
import org.springframework.core.convert.converter.Converter;
import org.springframework.util.Assert;

public class ProviderEntityConverter<C extends ConfigurableProvider<S>, S extends ConfigMap>
    implements Converter<ProviderEntity, C> {

    private final Supplier<C> factory;

    public ProviderEntityConverter(Supplier<C> factory) {
        Assert.notNull(factory, "factory is required");
        this.factory = factory;
    }

    public C convert(ProviderEntity pe) {
        C cp = factory.get();

        cp.setAuthority(pe.getAuthority());
        cp.setProvider(pe.getProvider());
        cp.setRealm(pe.getRealm());

        //config
        cp.setSettings(pe.getSettingsMap());
        cp.setConfiguration(pe.getConfigurationMap());
        cp.setVersion(pe.getVersion());
        cp.setEnabled(pe.getEnabled() != null ? pe.getEnabled().booleanValue() : false);

        //details
        cp.setName(pe.getName());
        cp.setTitleMap(pe.getTitleMap() != null ? pe.getTitleMap() : Collections.emptyMap());
        cp.setDescriptionMap(pe.getDescriptionMap() != null ? pe.getDescriptionMap() : Collections.emptyMap());

        return cp;
    }
}
