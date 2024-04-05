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

package it.smartcommunitylab.aac.spid.auth;

import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.spid.provider.SpidIdentityProviderConfig;
import java.util.Optional;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.util.Assert;

public class SpidRelyingPartyRegistrationRepository implements RelyingPartyRegistrationRepository {

    private final ProviderConfigRepository<SpidIdentityProviderConfig> providerConfigRepository;

    public SpidRelyingPartyRegistrationRepository(
        ProviderConfigRepository<SpidIdentityProviderConfig> providerConfigRepository
    ) {
        Assert.notNull(providerConfigRepository, "provider registration repository can not be null");
        this.providerConfigRepository = providerConfigRepository;
    }

    /*
     * read access as per interface
     */
    @Override
    public RelyingPartyRegistration findByRegistrationId(String registrationId) {
        Assert.hasText(registrationId, "registration id can not be empty");

        String[] s = registrationId.split("\\|");
        if (s.length < 2) {
            SpidIdentityProviderConfig providerConfig = providerConfigRepository.findByProviderId(registrationId);
            if (providerConfig == null) {
                return null;
            }

            return providerConfig.getRelyingPartyRegistration();
        } else {
            SpidIdentityProviderConfig providerConfig = providerConfigRepository.findByProviderId(s[0]);
            if (providerConfig == null) {
                return null;
            }

            return providerConfig
                .getRelyingPartyRegistrations()
                .stream()
                .filter(reg -> reg.getRegistrationId().equals(registrationId))
                .findAny()
                .orElse(null);
        }
    }
}
