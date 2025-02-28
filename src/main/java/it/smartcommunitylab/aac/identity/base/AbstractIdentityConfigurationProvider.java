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

package it.smartcommunitylab.aac.identity.base;

import com.fasterxml.jackson.databind.JavaType;
import it.smartcommunitylab.aac.base.model.AbstractConfigMap;
import it.smartcommunitylab.aac.base.provider.AbstractConfigurationProvider;
import it.smartcommunitylab.aac.config.ApplicationProperties;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.identity.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.identity.provider.IdentityProviderConfigurationProvider;
import it.smartcommunitylab.aac.identity.provider.IdentityProviderSettingsMap;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractIdentityConfigurationProvider<
    P extends AbstractIdentityProviderConfig<M>, M extends AbstractConfigMap
>
    extends AbstractConfigurationProvider<P, ConfigurableIdentityProvider, IdentityProviderSettingsMap, M>
    implements IdentityProviderConfigurationProvider<P, M> {

    protected ApplicationProperties applicationProperties;

    protected AbstractIdentityConfigurationProvider(
        String authority,
        ProviderConfigRepository<P> registrationRepository
    ) {
        super(authority, registrationRepository);
        setDefaultSettingsMap(new IdentityProviderSettingsMap());
    }

    @Autowired
    public void setApplicationProperties(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    @Override
    protected JavaType extractSettingsType() {
        return mapper.getTypeFactory().constructSimpleType(IdentityProviderSettingsMap.class, null);
    }

    @Override
    protected JavaType extractConfigType() {
        return _extractJavaType(1);
    }

    @Override
    protected ConfigurableIdentityProvider buildConfigurable(P providerConfig) {
        ConfigurableIdentityProvider cp = new ConfigurableIdentityProvider(
            providerConfig.getAuthority(),
            providerConfig.getProvider(),
            providerConfig.getRealm()
        );

        cp.setName(providerConfig.getName());
        cp.setTitleMap(providerConfig.getTitleMap());
        cp.setDescriptionMap(providerConfig.getDescriptionMap());

        cp.setSettings(getConfiguration(providerConfig.getSettingsMap()));
        cp.setConfiguration(getConfiguration(providerConfig.getConfigMap()));

        // provider config are active by definition
        cp.setEnabled(true);

        return cp;
    }
}
