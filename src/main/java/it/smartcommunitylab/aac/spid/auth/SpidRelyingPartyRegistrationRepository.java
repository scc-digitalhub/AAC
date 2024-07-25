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
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/*
 * SpidRelyingPartyRegistrationRepository is a repository of relying party registration.
 * The current implementation is "fake" repository. Instead, we store all spid provider
 * configurations and find a registration by asking it to the provider that generated that
 * registration. This is only possible because the registrationId encodes the providerId,
 * hence we can find the matching provider using the registrationId.
 */
public class SpidRelyingPartyRegistrationRepository implements RelyingPartyRegistrationRepository {

    private final ProviderConfigRepository<SpidIdentityProviderConfig> providerConfigRepository;

    public SpidRelyingPartyRegistrationRepository(
        ProviderConfigRepository<SpidIdentityProviderConfig> providerConfigRepository
    ) {
        Assert.notNull(providerConfigRepository, "provider registration repository can not be null");
        this.providerConfigRepository = providerConfigRepository;
    }

    /*
     * findByRegistrationId provides read access as per interface
     * Spid implements two different patterns for registration ids, that are:
     * (1) {providerId}
     * (2) {providerId}|{entityLabel}
     * where entity labels is the upstream SPID identity provider in the registry
     * The function try to guess which one we use in order to return the correct
     * relying party registration.
     */
    @Override
    public RelyingPartyRegistration findByRegistrationId(String registrationId) {
        Assert.hasText(registrationId, "registration id can not be empty");

        String decodedReg = SpidIdentityProviderConfig.decodeRegistrationId(registrationId); // either (1) or (2) as per function docs
        String[] s = StringUtils.split(decodedReg, "|");
        if (s == null) {
            // registrationId is base64 url encode(providerId)
            SpidIdentityProviderConfig providerConfig = providerConfigRepository.findByProviderId(decodedReg);
            if (providerConfig == null) {
                return null;
            }

            return providerConfig.getRelyingPartyRegistration();
        }
        // registrationId is base64 url encode({providerId}|{entityLabel})
        // s[0], s[1] = providerId, entityLabel
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
