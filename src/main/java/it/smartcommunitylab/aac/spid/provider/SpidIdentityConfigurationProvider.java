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

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.config.IdentityAuthoritiesProperties;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.identity.base.AbstractIdentityConfigurationProvider;
import it.smartcommunitylab.aac.identity.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.identity.provider.IdentityProviderSettingsMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SpidIdentityConfigurationProvider
    extends AbstractIdentityConfigurationProvider<SpidIdentityProviderConfig, SpidIdentityProviderConfigMap> {

    @Autowired
    public SpidIdentityConfigurationProvider(
        ProviderConfigRepository<SpidIdentityProviderConfig> registrationRepository,
        IdentityAuthoritiesProperties authoritiesProperties
    ) {
        this(
            SystemKeys.AUTHORITY_SPID,
            registrationRepository,
            new IdentityProviderSettingsMap(),
            new SpidIdentityProviderConfigMap()
        );
        if (
            authoritiesProperties != null &&
            authoritiesProperties.getSettings() != null &&
            authoritiesProperties.getSpid() != null
        ) {
            setDefaultSettingsMap(authoritiesProperties.getSettings());
            setDefaultConfigMap(authoritiesProperties.getSpid());
        }
    }

    public SpidIdentityConfigurationProvider(
        String authority,
        ProviderConfigRepository<SpidIdentityProviderConfig> registrationRepository,
        IdentityProviderSettingsMap settings,
        SpidIdentityProviderConfigMap configs
    ) {
        super(authority, registrationRepository);
        setDefaultSettingsMap(settings);
        setDefaultConfigMap(configs);
    }

    @Override
    protected SpidIdentityProviderConfig buildConfig(ConfigurableIdentityProvider cp) {
        return new SpidIdentityProviderConfig(
            cp,
            getSettingsMap(cp.getSettings()),
            getConfigMap(cp.getConfiguration())
        );
    }
}
