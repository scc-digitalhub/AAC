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
package it.smartcommunitylab.aac.base.provider.config;

import it.smartcommunitylab.aac.core.model.ConfigurableProvider;
import it.smartcommunitylab.aac.core.persistence.ProviderEntity;
import it.smartcommunitylab.aac.core.provider.config.ConfigurableProviderConverter;
import org.springframework.core.convert.converter.Converter;
import org.springframework.util.Assert;

public abstract class AbstractConfigurableProviderConverter<T extends ConfigurableProvider>
    implements ConfigurableProviderConverter<T> {

    protected final Converter<T, ProviderEntity> configConverter;
    protected final Converter<ProviderEntity, T> entityConverter;

    protected AbstractConfigurableProviderConverter(
        Converter<T, ProviderEntity> configConverter,
        Converter<ProviderEntity, T> entityConverter
    ) {
        Assert.notNull(entityConverter, "entity converter is required");
        Assert.notNull(configConverter, "config converter is required");
        this.configConverter = configConverter;
        this.entityConverter = entityConverter;
    }

    @Override
    public ProviderEntity convert(T configurable) {
        if (configConverter != null) {
            return configConverter.convert(configurable);
        }

        throw new UnsupportedOperationException("Unimplemented method 'convert'");
    }

    @Override
    public T convert(ProviderEntity entity) {
        if (entityConverter != null) {
            return entityConverter.convert(entity);
        }
        throw new UnsupportedOperationException("Unimplemented method 'convert'");
    }
}
