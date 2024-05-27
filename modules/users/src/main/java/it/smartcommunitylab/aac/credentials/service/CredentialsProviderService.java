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

package it.smartcommunitylab.aac.credentials.service;

import it.smartcommunitylab.aac.base.service.AbstractConfigurableProviderService;
import it.smartcommunitylab.aac.common.NoSuchAuthorityException;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.credentials.model.ConfigurableCredentialsProvider;
import it.smartcommunitylab.aac.credentials.model.EditableUserCredentials;
import it.smartcommunitylab.aac.credentials.model.UserCredentials;
import it.smartcommunitylab.aac.credentials.provider.CredentialsService;
import it.smartcommunitylab.aac.credentials.provider.CredentialsServiceConfig;
import it.smartcommunitylab.aac.credentials.provider.CredentialsServiceSettingsMap;
import it.smartcommunitylab.aac.model.ConfigMap;
import java.util.Collection;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CredentialsProviderService
    extends AbstractConfigurableProviderService<
        CredentialsService<
            ? extends UserCredentials,
            ? extends EditableUserCredentials,
            ConfigMap,
            CredentialsServiceConfig<ConfigMap>
        >,
        ConfigurableCredentialsProvider,
        CredentialsServiceConfig<ConfigMap>,
        CredentialsServiceSettingsMap
    > {

    public CredentialsService<
        ? extends UserCredentials,
        ? extends EditableUserCredentials,
        ConfigMap,
        CredentialsServiceConfig<ConfigMap>
    > findCredentialsService(String providerId) {
        return findResourceProvider(providerId);
    }

    public CredentialsService<
        ? extends UserCredentials,
        ? extends EditableUserCredentials,
        ConfigMap,
        CredentialsServiceConfig<ConfigMap>
    > getCredentialsService(String providerId) throws NoSuchProviderException, NoSuchAuthorityException {
        return getResourceProvider(providerId);
    }

    public Collection<
        CredentialsService<
            ? extends UserCredentials,
            ? extends EditableUserCredentials,
            ConfigMap,
            CredentialsServiceConfig<ConfigMap>
        >
    > listCredentialsServices() {
        return listResourceProviders();
    }

    public Collection<
        CredentialsService<
            ? extends UserCredentials,
            ? extends EditableUserCredentials,
            ConfigMap,
            CredentialsServiceConfig<ConfigMap>
        >
    > listCredentialsServicesByRealm(String realm) {
        return listResourceProvidersByRealm(realm);
    }
}
