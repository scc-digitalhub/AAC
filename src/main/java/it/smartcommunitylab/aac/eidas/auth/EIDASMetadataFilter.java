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

package it.smartcommunitylab.aac.eidas.auth;

import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import it.smartcommunitylab.aac.eidas.EIDASIdentityAuthority;
import it.smartcommunitylab.aac.spid.auth.SpidMetadataFilter;
import it.smartcommunitylab.aac.spid.provider.SpidIdentityProviderConfig;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;

/*
 * Publishes the SP metadata of the eidas providers under /auth/eidas/metadata/: the SPID filter, unchanged.
 *
 * Subclassed for the same reason as EIDASWebSsoAuthenticationRequestFilter: SpidMetadataFilter names its
 * OncePerRequestFilter mark after the SPID authority url, so a second instance in the chain would silence the
 * first one (404 on /auth/spid/metadata/ with the eidas authority present). See that class for the details.
 */
public class EIDASMetadataFilter extends SpidMetadataFilter {

    public static final String DEFAULT_FILTER_URI = EIDASIdentityAuthority.AUTHORITY_URL + "metadata/{registrationId}";

    public EIDASMetadataFilter(
        ProviderConfigRepository<SpidIdentityProviderConfig> configRepository,
        RelyingPartyRegistrationRepository relyingPartyRegistrationRepository
    ) {
        this(configRepository, relyingPartyRegistrationRepository, DEFAULT_FILTER_URI);
    }

    public EIDASMetadataFilter(
        ProviderConfigRepository<SpidIdentityProviderConfig> configRepository,
        RelyingPartyRegistrationRepository relyingPartyRegistrationRepository,
        String filterProcessingUrl
    ) {
        super(configRepository, relyingPartyRegistrationRepository, filterProcessingUrl);
    }

    @Override
    protected String getFilterName() {
        return getClass().getName() + "." + EIDASIdentityAuthority.AUTHORITY_URL;
    }
}
