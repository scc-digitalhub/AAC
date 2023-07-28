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

package it.smartcommunitylab.aac.password.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.config.CredentialsAuthoritiesProperties;
import it.smartcommunitylab.aac.core.base.AbstractCredentialsConfigurationProvider;
import it.smartcommunitylab.aac.core.provider.config.ConfigurableCredentialsProvider;

import org.springframework.stereotype.Service;

@Service
public class PasswordCredentialsConfigurationProvider
    extends AbstractCredentialsConfigurationProvider<PasswordIdentityProviderConfigMap, PasswordCredentialsServiceConfig> {

    public PasswordCredentialsConfigurationProvider(CredentialsAuthoritiesProperties authoritiesProperties) {
        super(SystemKeys.AUTHORITY_PASSWORD);
        if (authoritiesProperties != null && authoritiesProperties.getPassword() != null) {
            setDefaultConfigMap(authoritiesProperties.getPassword());
        } else {
            setDefaultConfigMap(new PasswordIdentityProviderConfigMap());
        }
    }

    @Override
    protected PasswordCredentialsServiceConfig buildConfig(ConfigurableCredentialsProvider cp) {
        return new PasswordCredentialsServiceConfig(cp, getConfigMap(cp.getConfiguration()));
    }
}
