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

package it.smartcommunitylab.aac.oidc.apple.auth;

import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.oidc.apple.provider.AppleIdentityProviderConfig;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.util.Assert;

public class AppleClientRegistrationRepository implements ClientRegistrationRepository {

    // provider configs by id
    private final ProviderConfigRepository<AppleIdentityProviderConfig> registrationRepository;

    public AppleClientRegistrationRepository(
        ProviderConfigRepository<AppleIdentityProviderConfig> registrationRepository
    ) {
        Assert.notNull(registrationRepository, "provider registration repository can not be null");
        this.registrationRepository = registrationRepository;
    }

    /*
     * read access as per interface
     */

    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        Assert.hasText(registrationId, "registration id cannot be empty");

        // fetch provider registration with matching id
        AppleIdentityProviderConfig providerConfig = registrationRepository.findByProviderId(registrationId);
        if (providerConfig == null) {
            return null;
        }

        // build
        // TODO evaluate loading cache
        return providerConfig.getClientRegistration();
    }
}
