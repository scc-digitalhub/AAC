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

package it.smartcommunitylab.aac.webauthn.provider;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.config.CredentialsAuthoritiesProperties;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.credentials.base.AbstractCredentialsConfigurationProvider;
import it.smartcommunitylab.aac.credentials.model.ConfigurableCredentialsProvider;
import org.springframework.stereotype.Service;

// @Service
public class WebAuthnCredentialsConfigurationProvider
    extends AbstractCredentialsConfigurationProvider<
        WebAuthnCredentialsServiceConfig,
        WebAuthnIdentityProviderConfigMap
    > {

    public WebAuthnCredentialsConfigurationProvider(
        ProviderConfigRepository<WebAuthnCredentialsServiceConfig> registrationRepository,
        CredentialsAuthoritiesProperties authoritiesProperties
    ) {
        super(SystemKeys.AUTHORITY_WEBAUTHN, registrationRepository);
        if (
            authoritiesProperties != null &&
            authoritiesProperties.getSettings() != null &&
            authoritiesProperties.getWebauthn() != null
        ) {
            setDefaultSettingsMap(authoritiesProperties.getSettings());
            setDefaultConfigMap(authoritiesProperties.getWebauthn());
        } else {
            setDefaultConfigMap(new WebAuthnIdentityProviderConfigMap());
        }
    }

    @Override
    protected WebAuthnCredentialsServiceConfig buildConfig(ConfigurableCredentialsProvider cp) {
        return new WebAuthnCredentialsServiceConfig(
            cp,
            getSettingsMap(cp.getSettings()),
            getConfigMap(cp.getConfiguration())
        );
    }
}
