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

import it.smartcommunitylab.aac.base.model.AbstractConfigMap;
import it.smartcommunitylab.aac.base.model.AbstractSettingsMap;
import it.smartcommunitylab.aac.core.model.ConfigurableProvider;
import it.smartcommunitylab.aac.core.model.ConfigurableProviderImpl;
import it.smartcommunitylab.aac.core.model.ProviderConfig;
import it.smartcommunitylab.aac.model.ConfigMap;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.util.Assert;

@Slf4j
public abstract class AbstractProviderConfigConverter<
    P extends ProviderConfig<S, M>, C extends ConfigurableProvider<S>, S extends ConfigMap, M extends ConfigMap
>
    extends AbstractConfigurableConverter<P, C, S, M>
    implements Converter<P, C> {

    protected AbstractProviderConfigConverter() {
        super();
    }

    @Override
    protected void init() {
        super.init();
        log.debug(
            "init provider config converter for {}: {}",
            providerType.getTypeName(),
            configurableType.getTypeName()
        );
        if (log.isTraceEnabled()) {
            log.trace("inferred generics types: {},{}", settingsType.getTypeName(), configType.getTypeName());
        }
    }

    @Override
    public C convert(P pc) {
        Assert.notNull(configurableType, "configurableType can not be null");
        Assert.notNull(providerType, "providerType can not be null");

        //try auto-build if C is subclass of default
        if (configurableType.isTypeOrSubTypeOf(ConfigurableProviderImpl.class)) {
            log.debug("build configurable for {}", pc.getProvider());
            if (log.isTraceEnabled()) {
                log.trace("config: {}", pc);
            }

            try {
                //build as typed
                //note: use RAW class to instantiate referenced class and not simpleType!
                @SuppressWarnings("unchecked")
                C cp = (C) configurableType.getRawClass().getDeclaredConstructor().newInstance();
                cp.setAuthority(pc.getAuthority());
                cp.setProvider(pc.getProvider());
                cp.setRealm(pc.getRealm());

                cp.setName(pc.getName());
                cp.setTitleMap(pc.getTitleMap());
                cp.setDescriptionMap(pc.getDescriptionMap());

                cp.setSettings(
                    pc.getSettingsMap() != null ? pc.getSettingsMap().getConfiguration() : Collections.emptyMap()
                );
                cp.setConfiguration(
                    pc.getConfigMap() != null ? pc.getConfigMap().getConfiguration() : Collections.emptyMap()
                );

                // provider config are active by definition
                cp.setEnabled(true);

                if (log.isTraceEnabled()) {
                    log.trace("configurable: {}", cp);
                }

                return cp;
            } catch (Exception e) {
                throw new UnsupportedOperationException();
            }
        }

        throw new UnsupportedOperationException();
    }
}
