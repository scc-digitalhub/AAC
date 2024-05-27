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

package it.smartcommunitylab.aac.base.provider.config;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import it.smartcommunitylab.aac.core.model.ConfigurableProvider;
import it.smartcommunitylab.aac.core.model.ProviderConfig;
import it.smartcommunitylab.aac.model.ConfigMap;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.util.Assert;

@Slf4j
public abstract class AbstractConfigurableProviderConverter<
    P extends ProviderConfig<S, M>, C extends ConfigurableProvider<S>, S extends ConfigMap, M extends ConfigMap
>
    extends AbstractConfigurableConverter<P, C, S, M>
    implements Converter<C, P> {

    protected AbstractConfigurableProviderConverter() {
        super();
    }

    @Override
    protected void init() {
        super.init();
        log.debug(
            "init configurable provider converter for {}: {}",
            configurableType.getTypeName(),
            providerType.getTypeName()
        );
        if (log.isTraceEnabled()) {
            log.trace("inferred generics types: {},{}", settingsType.getTypeName(), configType.getTypeName());
        }
    }

    @Override
    public P convert(C cp) {
        Assert.notNull(configurableType, "configurableType can not be null");
        Assert.notNull(providerType, "providerType can not be null");

        //try auto-build if C is subclass of default
        if (providerType.isTypeOrSubTypeOf(AbstractProviderConfig.class)) {
            log.debug("build config for {}", cp.getProvider());
            if (log.isTraceEnabled()) {
                log.trace("configurable: {}", cp);
            }

            //build as typed
            try {
                @SuppressWarnings("unchecked")
                //note: use RAW class to instantiate referenced class and not simpleType!
                P pc = (P) providerType.getRawClass().getDeclaredConstructor().newInstance();
                @SuppressWarnings("unchecked")
                AbstractProviderConfig<S, M> apc = (AbstractProviderConfig<S, M>) pc;

                apc.setAuthority(cp.getAuthority());
                apc.setProvider(cp.getProvider());
                apc.setRealm(cp.getRealm());

                apc.setName(cp.getName());
                apc.setTitleMap(cp.getTitleMap());
                apc.setDescriptionMap(cp.getDescriptionMap());

                apc.setSettingsMap(
                    getSettingsMap(cp.getSettings() != null ? cp.getSettings() : Collections.emptyMap())
                );
                apc.setConfigMap(
                    getConfigMap(cp.getConfiguration() != null ? cp.getConfiguration() : Collections.emptyMap())
                );
                apc.setVersion(cp.getVersion() != null ? cp.getVersion().intValue() : 0);

                if (log.isTraceEnabled()) {
                    log.trace("config: {}", pc);
                }

                return pc;
            } catch (
                InstantiationException
                | IllegalAccessException
                | IllegalArgumentException
                | InvocationTargetException
                | NoSuchMethodException
                | SecurityException e
            ) {
                throw new UnsupportedOperationException();
            }
        }

        throw new UnsupportedOperationException();
    }

    private M getConfigMap(Map<String, Serializable> map) {
        Assert.notNull(configType, "settingsType can not be null");

        // return a valid config from props
        // use mapper
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        return mapper.convertValue(map, configType);
    }

    private S getSettingsMap(Map<String, Serializable> map) {
        Assert.notNull(settingsType, "settingsType can not be null");

        // return a valid config from props
        // use mapper
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        return mapper.convertValue(map, settingsType);
    }
}
