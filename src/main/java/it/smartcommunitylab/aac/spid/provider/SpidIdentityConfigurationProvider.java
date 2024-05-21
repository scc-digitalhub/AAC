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
import it.smartcommunitylab.aac.spid.model.SpidRegistration;
import java.util.Collection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SpidIdentityConfigurationProvider
    extends AbstractIdentityConfigurationProvider<SpidIdentityProviderConfig, SpidIdentityProviderConfigMap> {

    private Collection<SpidRegistration> localRegistry;

    @Autowired
    public SpidIdentityConfigurationProvider(
        ProviderConfigRepository<SpidIdentityProviderConfig> registrationRepository,
        IdentityAuthoritiesProperties authoritiesProperties
    ) {
        super(SystemKeys.AUTHORITY_SPID, registrationRepository);
        setDefaultSettingsMap(new IdentityProviderSettingsMap());
        setDefaultConfigMap(new SpidIdentityProviderConfigMap());
        if (
            authoritiesProperties != null &&
            authoritiesProperties.getSettings() != null &&
            authoritiesProperties.getSpid() != null
        ) {
            setDefaultSettingsMap(authoritiesProperties.getSettings());
            setDefaultConfigMap(authoritiesProperties.getSpid());
        }
    }

    @Override
    protected SpidIdentityProviderConfig buildConfig(ConfigurableIdentityProvider cp) {
        SpidIdentityProviderConfig spidConfig = new SpidIdentityProviderConfig(
            cp,
            getSettingsMap(cp.getSettings()),
            getConfigMap(cp.getConfiguration())
        );
        // TODO: how do I _know_ that local registry has been set by the time we invoke the buildConfig?
        //  Check architecture and relationship with IdentityAuthority
        if (this.localRegistry != null) {
            spidConfig.setIdentityProviders(this.localRegistry);
        }
        return spidConfig;
    }

    public void setLocalRegistry(Collection<SpidRegistration> localRegistry) {
        this.localRegistry = localRegistry;
    }
}
