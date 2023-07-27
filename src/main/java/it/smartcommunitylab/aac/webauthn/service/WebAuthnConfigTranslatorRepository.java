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

package it.smartcommunitylab.aac.webauthn.service;

import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.core.service.TranslatorProviderConfigRepository;
import it.smartcommunitylab.aac.webauthn.provider.WebAuthnCredentialsServiceConfig;
import it.smartcommunitylab.aac.webauthn.provider.WebAuthnIdentityProviderConfig;

public class WebAuthnConfigTranslatorRepository
    extends TranslatorProviderConfigRepository<WebAuthnIdentityProviderConfig, WebAuthnCredentialsServiceConfig> {

    public WebAuthnConfigTranslatorRepository(
        ProviderConfigRepository<WebAuthnIdentityProviderConfig> externalRepository
    ) {
        super(externalRepository);
        setConverter(source -> {
            WebAuthnCredentialsServiceConfig config = new WebAuthnCredentialsServiceConfig(
                source.getProvider(),
                source.getRealm()
            );
            config.setName(source.getName());
            config.setTitleMap(source.getTitleMap());
            config.setDescriptionMap(source.getDescriptionMap());

            // we share the same configMap
            config.setConfigMap(source.getConfigMap());
            config.setRepositoryId(source.getRepositoryId());

            return config;
        });
    }
}
