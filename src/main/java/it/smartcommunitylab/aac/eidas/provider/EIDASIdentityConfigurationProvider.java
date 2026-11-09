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

package it.smartcommunitylab.aac.eidas.provider;

import com.fasterxml.jackson.databind.JavaType;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.config.IdentityAuthoritiesProperties;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.identity.base.AbstractIdentityConfigurationProvider;
import it.smartcommunitylab.aac.identity.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.identity.provider.IdentityProviderSettingsMap;
import it.smartcommunitylab.aac.spid.provider.SpidIdentityProviderConfigMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/*
 * Builds the configuration of an eidas provider.
 *
 * SPID resolves its upstream idps from the local registry; eIDAS has a single node, declared in the
 * configuration via `idpMetadataUrl`. setIdentityProviders() already covers that case on its own:
 * with a metadata url and no registry it reads the node metadata and registers it as the only
 * upstream idp, so the inherited SPID logic (registration ids, login button) works unchanged.
 */
@Service
public class EIDASIdentityConfigurationProvider
    extends AbstractIdentityConfigurationProvider<EIDASIdentityProviderConfig, SpidIdentityProviderConfigMap> {

    @Autowired
    public EIDASIdentityConfigurationProvider(
        @Qualifier("eidasProviderConfigRepository") ProviderConfigRepository<EIDASIdentityProviderConfig> registrationRepository,
        IdentityAuthoritiesProperties authoritiesProperties
    ) {
        super(SystemKeys.AUTHORITY_EIDAS, registrationRepository);
        setDefaultSettingsMap(new IdentityProviderSettingsMap());
        setDefaultConfigMap(new EIDASIdentityProviderConfigMap());
        if (
            authoritiesProperties != null &&
            authoritiesProperties.getSettings() != null &&
            authoritiesProperties.getEidas() != null
        ) {
            setDefaultSettingsMap(authoritiesProperties.getSettings());
            setDefaultConfigMap(authoritiesProperties.getEidas());
        }
    }

    /*
     * the generic config map type is inherited from the SPID configuration, pin the eidas one so
     * that configurations are deserialized (and validated) as eidas config maps
     */
    @Override
    protected JavaType extractConfigType() {
        return mapper.getTypeFactory().constructSimpleType(EIDASIdentityProviderConfigMap.class, null);
    }

    @Override
    protected EIDASIdentityProviderConfig buildConfig(ConfigurableIdentityProvider cp) {
        EIDASIdentityProviderConfig config = new EIDASIdentityProviderConfig(
            cp,
            getSettingsMap(cp.getSettings()),
            getConfigMap(cp.getConfiguration())
        );

        // base url FIRST. Unlike spid, setIdentityProviders() below resolves the node right away (no local
        // registry: the single node comes from idpMetadataUrl) and builds our relying party registration with
        // entityId(getEntityId()), which falls back to getMetadataUrl() when no entityId is configured - and
        // getMetadataUrl() is null until the base url is set. With the spid order (registry first, base url
        // after) a provider without an explicit entityId fails to register with "entityId cannot be empty".
        if (applicationProperties != null) {
            config.setBaseUrl(applicationProperties.getUrl());
        }

        // no local registry for eidas: the single node is derived from idpMetadataUrl
        config.setIdentityProviders(null);

        return config;
    }
}
