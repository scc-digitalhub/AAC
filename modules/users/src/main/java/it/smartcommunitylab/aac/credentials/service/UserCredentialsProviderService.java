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
import it.smartcommunitylab.aac.credentials.model.ConfigurableUserCredentialsProvider;
import it.smartcommunitylab.aac.credentials.model.EditableUserCredentials;
import it.smartcommunitylab.aac.credentials.model.UserCredentials;
import it.smartcommunitylab.aac.credentials.provider.UserCredentialsProvider;
import it.smartcommunitylab.aac.credentials.provider.UserCredentialsProviderConfig;
import it.smartcommunitylab.aac.credentials.provider.UserCredentialsProviderSettingsMap;
import it.smartcommunitylab.aac.model.ConfigMap;
import java.util.Collection;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserCredentialsProviderService
    extends AbstractConfigurableProviderService<
        UserCredentialsProvider<
            ? extends UserCredentials,
            ? extends EditableUserCredentials,
            ConfigMap,
            UserCredentialsProviderConfig<ConfigMap>
        >,
        ConfigurableUserCredentialsProvider,
        UserCredentialsProviderConfig<ConfigMap>,
        UserCredentialsProviderSettingsMap
    > {

    public UserCredentialsProvider<
        ? extends UserCredentials,
        ? extends EditableUserCredentials,
        ConfigMap,
        UserCredentialsProviderConfig<ConfigMap>
    > findCredentialsService(String providerId) {
        return findResourceProvider(providerId);
    }

    public UserCredentialsProvider<
        ? extends UserCredentials,
        ? extends EditableUserCredentials,
        ConfigMap,
        UserCredentialsProviderConfig<ConfigMap>
    > getCredentialsProvider(String providerId) throws NoSuchProviderException, NoSuchAuthorityException {
        return getResourceProvider(providerId);
    }

    public Collection<
        UserCredentialsProvider<
            ? extends UserCredentials,
            ? extends EditableUserCredentials,
            ConfigMap,
            UserCredentialsProviderConfig<ConfigMap>
        >
    > listCredentialsProviders() {
        return listResourceProviders();
    }

    public Collection<
        UserCredentialsProvider<
            ? extends UserCredentials,
            ? extends EditableUserCredentials,
            ConfigMap,
            UserCredentialsProviderConfig<ConfigMap>
        >
    > listCredentialsProvidersByRealm(String realm) {
        return listResourceProvidersByRealm(realm);
    }
}
