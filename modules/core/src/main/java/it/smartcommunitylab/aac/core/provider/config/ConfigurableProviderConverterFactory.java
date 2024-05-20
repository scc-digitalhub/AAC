/**
 * Copyright 2024 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.core.provider.config;

import it.smartcommunitylab.aac.base.provider.config.AbstractConfigurableProviderConverter;
import it.smartcommunitylab.aac.core.model.ConfigurableProvider;
import it.smartcommunitylab.aac.core.model.ProviderConfig;
import it.smartcommunitylab.aac.model.ConfigMap;
import java.lang.reflect.InvocationTargetException;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.type.TypeDescription;

public class ConfigurableProviderConverterFactory {

    static ConfigurableProviderConverterFactory instance = null;

    protected ConfigurableProviderConverterFactory() {}

    public static ConfigurableProviderConverterFactory instance() {
        if (instance == null) {
            instance = new ConfigurableProviderConverterFactory();
        }

        return instance;
    }

    @SuppressWarnings("unchecked")
    public <
        P extends ProviderConfig<S, M>, C extends ConfigurableProvider<S>, S extends ConfigMap, M extends ConfigMap
    > AbstractConfigurableProviderConverter<P, C, S, M> build(
        java.lang.reflect.Type configType,
        java.lang.reflect.Type configurableType,
        java.lang.reflect.Type settingsType,
        java.lang.reflect.Type mapType
    ) {
        try {
            return (AbstractConfigurableProviderConverter<P, C, S, M>) new ByteBuddy()
                .subclass(
                    TypeDescription.Generic.Builder.parameterizedType(
                        AbstractConfigurableProviderConverter.class,
                        configType,
                        configurableType,
                        settingsType,
                        mapType
                    ).build()
                )
                .name(
                    getClass().getPackageName() + "." + configurableType.getTypeName() + "To" + configType.getTypeName()
                )
                .make()
                .load(getClass().getClassLoader())
                .getLoaded()
                .getDeclaredConstructor()
                .newInstance();
        } catch (
            InstantiationException
            | IllegalAccessException
            | IllegalArgumentException
            | InvocationTargetException
            | NoSuchMethodException
            | SecurityException e
        ) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }
}
